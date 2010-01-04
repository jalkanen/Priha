package org.priha.core.observation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.observation.*;

import org.priha.core.NodeImpl;
import org.priha.core.SessionImpl;
import org.priha.core.WorkspaceImpl;
import org.priha.path.Path;
import org.priha.path.PathFactory;
import org.priha.util.GenericIterator;
import org.priha.util.ChangeStore.Change;

/**
 *  Implements an ObservationManager. Again, the actual implementation is a per-Session class instantiated
 *  at request.
 *  
 */
// FIXME: This class is not particularly optimized.
public class ObservationManagerImpl
{
    private static ObservationManagerImpl c_manager = new ObservationManagerImpl();
    
    private ArrayList<EventListenerWrapper> m_listeners = new ArrayList<EventListenerWrapper>();
    
    private Logger log = Logger.getLogger(ObservationManagerImpl.class.getName());
    
    public void addEventListener(SessionImpl session, EventListener listener, int eventTypes, Path absPath, 
                                 boolean isDeep, String[] uuid, String[] nodeTypeName, boolean noLocal)
        throws RepositoryException
    {
        EventListenerWrapper elw = new EventListenerWrapper(session,listener,eventTypes,absPath,isDeep,uuid,nodeTypeName,noLocal);
        
        m_listeners.add(elw);
    }

    public EventListenerIterator getRegisteredEventListeners(SessionImpl session) throws RepositoryException
    {
        List<EventListener> list = new ArrayList<EventListener>();
        
        for( Iterator<EventListenerWrapper> i = m_listeners.iterator() ; i.hasNext(); )
        {
            EventListenerWrapper elw = i.next();
            if( elw.getSessionId().equals(session.getId()) ) 
            {
                list.add( elw.getListener() );
            }
        }
        
        return new Iter(list);
    }

    public void removeEventListener(EventListener listener) throws RepositoryException
    {
        for( Iterator<EventListenerWrapper> i = m_listeners.iterator() ; i.hasNext(); )
        {
            if( i.next().getListener() == listener ) 
            {
                i.remove();
                return;
            }
        }
    }
    
    public void fireEvent( SessionImpl srcSession, List<Change> changes )
    {
        for( Iterator<EventListenerWrapper> i = m_listeners.iterator() ; i.hasNext(); )
        {
            EventListenerWrapper elw = i.next();

            // Fire the event
            
            try
            {
                elw.getListener().onEvent( new EventIteratorImpl(filterEvents(srcSession,elw,changes)) );
            }
            catch( Exception e )
            {
                log.warning("Unable to fire event "+ e.getMessage());
            }
        }        
    }

    private List<Event> filterEvents( SessionImpl session, EventListenerWrapper elw, List<Change> changes ) throws ItemNotFoundException, AccessDeniedException, RepositoryException
    {
        ArrayList<Event> list = new ArrayList<Event>();

        for( Change c : changes )
        {
            System.out.println(c);
            // Session filtering
            if( elw.isNoLocal() && session.getId().equals(elw.getSessionId()) )
            {
                continue;
            }
            
            // Path filtering
            if( elw.getAbsPath() != null )
            {
                Path itemPath = c.getPath();
                Path ePath = elw.getAbsPath();
                
                //
                // This fairly complicated sentence fulfils the contract on page 272 of JCR-1.0 spec.
                //
                if( !(( (!itemPath.isRoot() && ePath.equals(itemPath.getParentPath())) && !elw.isDeep()) ||
                    (ePath.isParentOf(itemPath) && elw.isDeep() )) )
                {
                    continue;
                }
               
            }
            
            // UUID filtering
           
            if( elw.getUuids() != null )
            {
                NodeImpl parent = c.getItem().getParent();
                
                try
                {
                    String parentUUID = parent.getUUID();
                    
                    if( Arrays.binarySearch( elw.getUuids(), parentUUID ) < 0 )
                        continue;
                }
                catch( UnsupportedRepositoryOperationException e ) {}
            }
            
            // Nodetype filtering
            
            if( elw.getNodeTypeNames() != null )
            {
                NodeImpl parent = c.getItem().getParent();
                
                boolean success = false;
                for( String s : elw.getNodeTypeNames() )
                {
                    if( parent.isNodeType(s) ) 
                    {
                        success = true;
                        break;
                    }
                }
                
                if( !success ) continue;
            }

            // EventType filtering
            int eventType = -1;
            
            switch( c.getState() )
            {
                case NEW:
                case MOVED:
                    if( c.getItem().isNode() )
                        eventType = Event.NODE_ADDED;
                    else
                        eventType = Event.PROPERTY_ADDED;
                    break;
                    
                case UPDATED:
                    if( !c.getItem().isNode() )
                        eventType = Event.PROPERTY_CHANGED;
                    
                    break;
                    
                case REMOVED:
                    if( c.getItem().isNode() )
                        eventType = Event.NODE_REMOVED;
                    else
                        eventType = Event.PROPERTY_REMOVED;
                    
                    break;
                    
            }
            
            if( (elw.getEventTypes() & eventType) == 0 )
            {
                continue;
            }
            
            // Add to the event list
            
            
            if( eventType != -1 )
            {
                Event e = new EventImpl(session, eventType, c.getPath());
                list.add( e );
                System.out.println("Firing "+e);
            }
        }
        
        return list;
    }
    
    private static class EventIteratorImpl 
        extends GenericIterator 
        implements EventIterator
    {
        public EventIteratorImpl(List<Event> list)
        {
            super(list);
        }

        public Event nextEvent()
        {
            return (Event) super.next();
        }
    }
    
    private static class EventImpl implements Event
    {
        SessionImpl m_session;
        int m_eventType;
        Path m_path;
        
        public EventImpl(SessionImpl s, int eventType, Path path)
        {
            m_session = s;
            m_eventType = eventType;
            m_path = path;
        }
        
        public String getPath() throws RepositoryException
        {
            return m_path.toString(m_session);
        }

        public int getType()
        {
            return m_eventType;
        }

        public String getUserID()
        {
            return m_session.getUserID();
        }

        public String toString()
        {
            return getEventString(m_eventType) + " " + m_path.toString();
        }

        private String getEventString(int type)
        {
            switch( type )
            {
                case NODE_ADDED:
                    return "NODE_ADDED";
                    
                case NODE_REMOVED:
                    return "NODE_REMOVED";
                    
                case PROPERTY_ADDED:
                    return "PROPERTY_ADDED";
                    
                case PROPERTY_CHANGED:
                    return "PROPERTY_CHANGED";
                    
                case PROPERTY_REMOVED:
                    return "PROPERTY_REMOVED";
                    
                default:
                    return "UNKNOWN";
            }
        }
    }
    
    /**
     *  Provides iteration through EventListeners.
     *  
     */
    private static class Iter extends GenericIterator implements EventListenerIterator
    {
        public Iter(List<EventListener> list)
        {
            super( list );
        }

        public EventListener nextEventListener()
        {
            return (EventListener)super.next();
        }
        
        public Object next()
        {
            return nextEventListener();
        }
    }
    
    /**
     *  Wraps everything interesting for an EventListener.
     */
    private static class EventListenerWrapper
    {
        private String        m_sessionId;
        private EventListener m_listener;
        private int           m_eventTypes;
        private Path          m_absPath;
        private boolean       m_isDeep;
        private String[]      m_uuids;
        private String[]      m_nodeTypeNames;
        private boolean       m_noLocal;
        
        public EventListenerWrapper( SessionImpl session, EventListener listener, int eventTypes,
                                     Path absPath, boolean isDeep, String[] uuids, String[] nodeTypeNames, boolean noLocal )
        {
            m_sessionId = session.getId();
            m_listener  = listener;
            m_eventTypes = eventTypes;
            m_absPath   = absPath;
            m_isDeep    = isDeep;
            m_uuids     = uuids;
            m_nodeTypeNames = nodeTypeNames;
            m_noLocal   = noLocal;
            
            if( m_uuids != null ) Arrays.sort(m_uuids);
        }
        
        public String getSessionId()
        {
            return m_sessionId;
        }
        public EventListener getListener()
        {
            return m_listener;
        }
        public int getEventTypes()
        {
            return m_eventTypes;
        }
        public Path getAbsPath()
        {
            return m_absPath;
        }
        public boolean isDeep()
        {
            return m_isDeep;
        }
        public String[] getUuids()
        {
            return m_uuids;
        }
        public String[] getNodeTypeNames()
        {
            return m_nodeTypeNames;
        }
        public boolean isNoLocal()
        {
            return m_noLocal;
        }
    }
    
    /**
     *  Session-local ObservationManager.  
     */
    public class Impl implements ObservationManager
    {
        SessionImpl m_session;
        
        public Impl(SessionImpl session)
        {
            m_session = session;
        }

        public void addEventListener(EventListener listener, int eventTypes, String absPath, 
                                     boolean isDeep, String[] uuid, String[] nodeTypeName, boolean noLocal)
            throws RepositoryException
        {
            ObservationManagerImpl.this.addEventListener(m_session, listener, eventTypes, 
                                                         PathFactory.getPath(m_session, absPath), 
                                                         isDeep, uuid, nodeTypeName, noLocal);
        }

        public EventListenerIterator getRegisteredEventListeners() throws RepositoryException
        {
            return ObservationManagerImpl.this.getRegisteredEventListeners(m_session);
        }

        public void removeEventListener(EventListener listener) throws RepositoryException
        {
            ObservationManagerImpl.this.removeEventListener(listener);
        }
        
        public void fireEvent( List<Change> change )
        {
            ObservationManagerImpl.this.fireEvent( m_session, change );
        }
    }

    /**
     *  Get an instance for a Workspace.
     */
    public static ObservationManagerImpl.Impl getInstance(WorkspaceImpl workspaceImpl)
    {
        return c_manager.new Impl(workspaceImpl.getSession());
    }
}
