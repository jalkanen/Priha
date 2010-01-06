package org.priha.core.observation;

import java.util.*;
import java.util.logging.Logger;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.observation.*;
import javax.jcr.observation.EventListener;

import org.priha.core.*;
import org.priha.path.Path;
import org.priha.path.PathFactory;
import org.priha.util.ChangeStore;
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
    
    private Map<String,List<EventListenerWrapper>> m_listeners = new HashMap<String,List<EventListenerWrapper>>();
    
    private Logger log = Logger.getLogger(ObservationManagerImpl.class.getName());
    
    /**
     *  Adds an event listener for a particular Session.
     *  
     * @param session
     * @param listener
     * @param eventTypes
     * @param absPath
     * @param isDeep
     * @param uuid
     * @param nodeTypeName
     * @param noLocal
     * @throws RepositoryException
     */
    public void addEventListener(SessionImpl session, EventListener listener, int eventTypes, Path absPath, 
                                 boolean isDeep, String[] uuid, String[] nodeTypeName, boolean noLocal)
        throws RepositoryException
    {
        EventListenerWrapper elw = new EventListenerWrapper(session,listener,eventTypes,absPath,isDeep,uuid,nodeTypeName,noLocal);
        
        List<EventListenerWrapper> list = m_listeners.get(session.getWorkspace().getName());
        
        if( list == null )
            list = new ArrayList<EventListenerWrapper>();
        
        list.add( elw );
        
        m_listeners.put(session.getWorkspace().getName(), list);
    }

    /**
     *  Lists EventListeners for a given Session.
     *  
     * @param session
     * @return
     * @throws RepositoryException
     */
    public EventListenerIterator getRegisteredEventListeners(SessionImpl session) throws RepositoryException
    {
        List<EventListener> list = new ArrayList<EventListener>();
        
        List<EventListenerWrapper> l = m_listeners.get(session.getWorkspace().getName());
        
        if( l != null )
        {
            for( Iterator<EventListenerWrapper> i = l.iterator() ; i.hasNext(); )
            {
                EventListenerWrapper elw = i.next();
                if( elw.getSessionId().equals(session.getId()) ) 
                {
                    list.add( elw.getListener() );
                }
            }
        }
        return new Iter(list);
    }

    /**
     *  Removes an Event listener (for any session)
     * @param listener
     * @throws RepositoryException
     */
    public void removeEventListener(EventListener listener) throws RepositoryException
    {
        for( List<EventListenerWrapper> le : m_listeners.values() )
        {
            for( Iterator<EventListenerWrapper> i = le.iterator() ; i.hasNext(); )
            {
                if( i.next().getListener() == listener ) 
                {
                    i.remove();
                    return;
                }
            }
        }
    }
    
    public void fireEvent( SessionImpl srcSession, ChangeStore changes )
    {
        List<EventListenerWrapper> l = m_listeners.get(srcSession.getWorkspace().getName());

        if( l != null )
        {
            for( Iterator<EventListenerWrapper> i = l.iterator() ; i.hasNext(); )
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
    }

    private List<Event> filterEvents( SessionImpl session, EventListenerWrapper elw, ChangeStore changes ) throws ItemNotFoundException, AccessDeniedException, RepositoryException
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

            // Tags filtering
            
            if( !c.getItem().isNode() )
            {
                if( ((PropertyImpl)c.getItem()).isTransient() ) continue;
            }
            
            // Internal tags filtering
            
            if( c.getPath().getLastComponent().equals(NodeImpl.Q_PRIHA_TMPMOVE) )
                continue;
            
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

            // Transient changes filtering. Priha will actually list all the events, both additions and removals
            // so we need to filter out the ones which result in no change in the repository.
            
            if( c.getState() != ItemState.REMOVED )
            {
                if( changes.getLatestChange(c.getPath()).getState() == ItemState.REMOVED )
                    continue;
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
        
        public void fireEvents( ChangeStore changes )
        {
            ObservationManagerImpl.this.fireEvent( m_session, changes );
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
