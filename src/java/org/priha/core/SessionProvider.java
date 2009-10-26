/*
    Priha - A JSR-170 implementation library.

    Copyright (C) 2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    Licensed under the Apache License, Version 2.0 (the "License"); 
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at 
    
      http://www.apache.org/licenses/LICENSE-2.0 
      
    Unless required by applicable law or agreed to in writing, software 
    distributed under the License is distributed on an "AS IS" BASIS, 
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
    See the License for the specific language governing permissions and 
    limitations under the License. 
 */
package org.priha.core;

import java.util.*;
import java.util.logging.Logger;

import javax.jcr.*;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;

import org.priha.nodetype.QNodeType;
import org.priha.path.*;
import org.priha.providers.StoreTransaction;
import org.priha.util.*;
import org.priha.util.ChangeStore.Change;
import org.priha.version.VersionManager;

/**
 *  This is a special provider which stores the state of the Session.
 *  <p>
 *  At the moment this could be O(N) depending on the size of the unsaved items.
 *  
 */
public class SessionProvider
{
    private ItemStore          m_source;
    private WorkspaceImpl      m_workspace;
    
    ChangeStore        m_changedItems; // TODO: Back to private
    
    // FIXME: Should probably be elsewhere
    private static PathManager c_sessionPathManager = new PathManager();
 
    private static final int   DEFAULT_CACHESIZE = 1000;
    
    private Map<PathRef,ItemImpl> m_fetchedItems;
    private Map<String,NodeImpl>  m_uuidMap;
    private Logger             log = Logger.getLogger(SessionProvider.class.getName());
    
    public SessionProvider( SessionImpl session, ItemStore source )
    {
        m_source  = source;
        m_workspace = session.getWorkspace();
        
        m_changedItems = new ChangeStore();
        
        m_fetchedItems = new SizeLimitedHashMap<PathRef,ItemImpl>(DEFAULT_CACHESIZE);
        m_uuidMap      = new SizeLimitedHashMap<String,NodeImpl>(DEFAULT_CACHESIZE);
    }
    
    /**
     *  Makes sure an item is cleared away from all internal session caches.
     *  
     *  @param ii Item to remove. May be null.
     *  @param uuid UUID of an item to remove. May be null.
     */
    private void clearSingleItem( final ItemImpl ii, final String uuid )
    {
        if( ii != null )   m_fetchedItems.remove( ii.getPathReference() );
        if( uuid != null ) m_uuidMap.remove( uuid );        
    }
    
    /**
     *  Visits all Sessions from this particular Repository and clears local caches.
     *  
     *  @param ii Item to remove. May be null.
     *  @param uuid UUID of an item to remove. May be null.
     */
    private void clearAllCaches( final ItemImpl ii, final String uuid )
    {
        m_workspace.getSession().getRepository().visit( new RepositoryImpl.SessionVisitor() {

            public void visit( SessionImpl session )
            {
                session.m_provider.clearSingleItem( ii, uuid );
            }
            
        });
    }
    
    /**
     *  Saves everything starting from root node.
     *  
     *  @throws RepositoryException If something goes wrong.
     */
    public void save() throws RepositoryException
    {
        save( Path.ROOT );
    }
    
    /**
     *  Call when you wish to add a new Node in this Session.
     *  
     *  @param ni Node to add
     *  @throws RepositoryException If the Path cannot be determined.
     */
    public void addNode( NodeImpl ni ) throws RepositoryException
    {
        m_changedItems.add( ni.getState(), ni );
    }

    public ItemImpl getItem( Path path ) throws InvalidPathException, RepositoryException
    {
        ItemImpl ii = m_changedItems.get(path);

        if( ii != null ) 
        {
            return ii;
        }
        
        ii = m_fetchedItems.get( getPathManager().getPathRef( path ) );
        
        if( ii != null )
            return ii;
        
        ii = m_source.getItem(m_workspace, path);
        
        if( ii != null ) m_fetchedItems.put( ii.getPathReference(), ii );
        
        return ii;
    }
    
    public void close()
    {
        m_source.close( m_workspace );
    }

    public void copy(Path srcpath, Path destpath) throws RepositoryException
    {
        m_source.copy( m_workspace, srcpath, destpath );
    }

    public NodeImpl findByUUID(String uuid) throws RepositoryException
    {
        for( Change c : m_changedItems )
        {
            ItemImpl ni = c.getItem();
            try
            {
                // NEW items don't yet have an UUID
                if( ni.isNode() && ni.getState() != ItemState.NEW )
                {
                    String pid = ((NodeImpl)ni).getUUID();
                    if( uuid.equals(pid) ) return (NodeImpl)ni;
                }
            }
            catch(RepositoryException e) {}
        }

        NodeImpl ii = m_uuidMap.get( uuid );
        
        if( ii == null )
        {
            ii = m_source.findByUUID( m_workspace, uuid );
            
            if( ii != null ) 
            {
                m_fetchedItems.put( ii.getPathReference(), ii );
                m_uuidMap.put( uuid, ii );
            }
        }
        
        return ii;
    }

    /**
     *  Finds all references to the given UUID.
     *  
     *  @param uuid
     *  @return
     *  @throws RepositoryException
     */
    public List<PropertyImpl> getReferences( String uuid ) throws RepositoryException
    {
        TreeSet<PropertyImpl> response = new TreeSet<PropertyImpl>();
        
        for (Change c : m_changedItems )
        {
            ItemImpl ii = c.getItem();
            if (!ii.isNode())
            {
                PropertyImpl pi = (PropertyImpl) ii;

                if (pi.getType() == PropertyType.REFERENCE)
                {
                    Value[] v;

                    if (pi.getDefinition().isMultiple())
                    {
                        v = pi.getValues();
                    }
                    else
                    {
                        v = new Value[] { pi.getValue() };
                    }

                    for (Value vv : v)
                    {
                        if (vv.getString().equals(uuid))
                        {
                            response.add(pi);
                            break;
                        }
                    }
                }
            }
        }
        
        response.addAll( m_source.getReferences(m_workspace,uuid) );
        
        // FIXME: Kludge
        ArrayList<PropertyImpl> ls = new ArrayList<PropertyImpl>();
        ls.addAll( response );
        return ls;
    }

    public List<Path> listNodes(Path parentpath) throws RepositoryException
    {
        List<Path> res = new ArrayList<Path>();
        
        for( Change c : m_changedItems )
        {
            ItemImpl ni = c.getItem();
            if( parentpath.isParentOf(ni.getInternalPath()) && ni.isNode() && !res.contains(ni.getInternalPath()) )
            {
                res.add( ni.getInternalPath() );
            }
        }
        
        try
        {
            List<? extends Path> existingNodes = m_source.listNodes(m_workspace, parentpath);
            
            for( Path p : existingNodes )
            {
                if( !res.contains( p ) ) res.add( p );
            }
        }
        catch( PathNotFoundException e )
        {
            // This is fine, because it could be that the node has not yet
            // been save()d.
        }
        
        return res;
    }

    public Collection<? extends String> listWorkspaces() throws RepositoryException
    {
        return m_source.listWorkspaces();
    }

    public boolean nodeExists(Path path) throws RepositoryException
    {
        Change c = m_changedItems.getChange(path);

        if( c != null && c.getItem().isNode() ) 
        {
            if( c.getState() == ItemState.REMOVED || c.getState() == ItemState.MOVED ) return false;
            return true;
        }
        
        return m_source.nodeExists(m_workspace, path);
    }
/*
    public void open( Credentials credentials, String workspaceName)
        throws RepositoryException,
               NoSuchWorkspaceException
    {
        m_source.open(credentials, workspaceName);
    }
*/
    public void remove(ItemImpl item) throws RepositoryException
    {
        m_changedItems.add( item.getState(), item );
    }

    public void stop()
    {
        m_source.stop();
    }

    public boolean hasPendingChanges()
    {
        return !m_changedItems.isEmpty();
    }

    public void clear()
    {
        m_changedItems.clear();
    }

//    @SuppressWarnings("fallthrough")
    public void save(Path path) throws RepositoryException
    {
        checkReferentialIntegrity(path);
        
        checkMoveConstraint(path);        
        
//        m_changedItems.dump();
        
        List<Path> toberemoved = new ArrayList<Path>();
        
        ChangeStore unsaved = new ChangeStore();
        
        //
        //  Do the actual save.  The way we do this is that we simply just take the
        //  first one from the queue, and and attempt to save it.  This allows
        //  e.g. preSave() to create some additional properties before saving - 
        //  a basic Iterator over the Set would cause ConcurrentModificationExceptions.
        //
        //  All unsaved items are stored in a particular list, and then added back
        //  to the savequeue.
        //
        
        StoreTransaction tx = m_source.storeStarted( m_workspace );
        boolean succeeded = false;
        
        try
        {
            Change change;
            
            while( (change = m_changedItems.peek()) != null )
            {
                ItemImpl ii = change.getItem();
                    
                if( path.isParentOf(ii.getInternalPath()) || path.equals( ii.getInternalPath() ))
                {
                    if( ii.isNode() )
                    {
                        NodeImpl ni = (NodeImpl) ii;
                        checkSanity(change.getState(), change.getPath(), ni);
                    
                        switch( change.getState() )
                        {
                            case EXISTS:
                                // These guys shouldn't be here.
                                break;
                                
                            case UPDATED:
                                ni.preSave();
                                // Nodes which exist don't need to be added, but they might need to be reordered.
//                                toberemoved.remove( change.getPath() ); // In case it's there
                                
                                List<Path> childOrder = ni.getChildOrder();
                                if( childOrder != null )
                                {
                                    System.out.println("Reordering children...");
                                    
                                    m_source.reorderNodes( tx, change.getPath(), childOrder );
                                    ni.setChildOrder(null); // Rely again on the repository order.
                                }
                                
                                ni.postSave();
                                break;
                            
                            case NEW:
                                ni.preSave();
                                toberemoved.remove( change.getPath() ); // In case it's there
                                m_source.addNode( tx, ni );
                                ni.postSave();
                                m_fetchedItems.put( ni.getPathReference(), ni );
                                break;
                        
                            case REMOVED:
                                if( !m_source.nodeExists( m_workspace, change.getPath() ) )
                                {
                                    throw new InvalidItemStateException("The item has been removed by some other Session "+ii.getInternalPath());
                                }
                                String uuid = null;
                                try
                                {
                                    uuid = ni.getUUID();
                                }
                                catch( UnsupportedRepositoryOperationException e ) {} // Fine, no uuid
                                
                                toberemoved.add( change.getPath() );
//                                m_source.remove(tx, change.getPath());
                                clearAllCaches( ni, uuid );
                                break;
                                
                            case MOVED:                                
                                toberemoved.add( change.getPath() );
//                                m_source.remove(tx, change.getPath());
                                clearAllCaches( ni, null );
                                break;
                                
                            case UNDEFINED:
                                throw new IllegalStateException("Node should not at this stage be UNDEFINED "+change.getPath());
                              
                        }
                    }
                    else
                    {
                        PropertyImpl pi = (PropertyImpl)ii;
                        
                        switch( change.getState() )
                        {
                            case EXISTS:
                                // These guys shouldn't be here.
                                break;
                                
                            case NEW:
                            case UPDATED:
                                // Do not save transient properties.
                                if( pi.isTransient() )
                                {
                                    break;
                                }
                                pi.preSave();
                                m_source.putProperty( tx, change.getPath(), change.getValue() );
                                pi.postSave();
                                m_fetchedItems.put( pi.getPathReference(), pi );
                                toberemoved.remove( change.getPath() ); // In case it's there
                                break;
                                
                            case REMOVED:
                                if( !pi.isTransient() )
                                {
                                    toberemoved.add(change.getPath());
//                                m_source.remove(tx, change.getPath());
                                }
                                
                                clearAllCaches( pi, null );
                                break;   
                                
                            case MOVED:
                                throw new RepositoryException("Properties should never be marked as MOVED!");
                                
                            case UNDEFINED:
                                throw new IllegalStateException("A Property should not at this stage be UNDEFINED! "+change.getPath());
                        }
                    }                
                }
                else
                {
                    unsaved.add( change );
                }
            
                //
                //  Remove from the queue so that we don't use it again.  This must
                //  be done here so that there never is an Item missing if any of the
                //  intermediate calls do e.g. hasProperty();
                //
                m_changedItems.remove();
            }
        
            //
            //  Finally, do the remove.  First, sort all in a reverse
            //  depth order (longest first).
            //
            
            Collections.sort( toberemoved, new Comparator<Path>() {

                public int compare(Path o1, Path o2)
                {
                    return o2.depth() - o1.depth();
                }
            
            });
        
            for( Path p : toberemoved )
            {
                log.finest("Removing "+p);
                m_source.remove(tx, p);
                //
                // Some accesses may trigger local cache hits, so we do remove any items at this
                // stage as well.
                //
                m_fetchedItems.remove( m_workspace.getSession().getPathManager().getPathRef(p) );
            }
            
            //
            //  Put the unsaved ones back into the queue
            //
            for( Change c : unsaved )
            {
                m_changedItems.add( c );
            }
            m_source.storeFinished( tx );
            succeeded = true;
        }
        finally
        {
            if(!succeeded) m_source.storeCancelled( tx );
        }
    }

    private void checkMoveConstraint(Path path)
                                               throws RepositoryException,
                                                   ValueFormatException,
                                                   PathNotFoundException,
                                                   NamespaceException,
                                                   ConstraintViolationException
    {
        //
        //  Test MOVE_CONSTRAINT
        //
        
        for( Iterator<Change> i = m_changedItems.iterator(); i.hasNext(); )
        {
            Change c = i.next();
            
            ItemImpl ii = c.getItem();
            
            if( path.isParentOf(c.getPath()) || path.equals( c.getPath() ))
            {
                if( ii.isNode() )
                {
                    NodeImpl ni = (NodeImpl)ii;
                    
                    if( ni.hasProperty(SessionImpl.MOVE_CONSTRAINT) )
                    {
                        String tgtPath = ni.getProperty(SessionImpl.MOVE_CONSTRAINT).getString();
                        
                        NodeImpl tgt = (NodeImpl)m_changedItems.get(PathFactory.getPath(m_workspace.getSession(),tgtPath));
                        
                        if( tgt != null &&
                            path.isParentOf(tgt.getInternalPath()) )
                        {
                            // Is okay; as it should be.  We no longer need this.
                        }
                        else
                        {
                            throw new ConstraintViolationException("When moving, both source and target Nodes must be saved in one go.");
                        }
                    }
                }
            }
        }
    }

    private void checkReferentialIntegrity(Path path) throws RepositoryException, ReferentialIntegrityException
    {
        //
        //  Test referential integrity.
        //
        for( Iterator<Change> i = m_changedItems.iterator(); i.hasNext(); )
        {
            Change c = i.next();
            
            ItemImpl ii = c.getItem();
            
            if( path.isParentOf(ii.getInternalPath()) || path.equals( ii.getInternalPath() ))
            {
                if( ii.isNode() && ii.getState() == ItemState.REMOVED )
                {
                    try
                    {
                        Collection<PropertyImpl> refs = getReferences( ((NodeImpl)ii).getUUID() );
                        
                        for( PropertyImpl pi : refs )
                        {
                            if( pi.getState() != ItemState.REMOVED )
                            {
                                throw new ReferentialIntegrityException("Attempted to remove a Node which still has references: "+
                                                                        ii.getPath()+
                                                                        " (the Property holding the reference is "+
                                                                        pi.getPath()+
                                                                        ")");
                            }
                        }
                    }
                    catch( UnsupportedRepositoryOperationException e )
                    {
                        // Does not have an UUID, so cannot be referenced.
                    }
                }
            }
        }
    }

    /**
     *  State checks the sanity of a Node before saving.
     *  
     *  @param state
     *  @param ni
     *  @throws RepositoryException
     */
    private void checkSanity(ItemState state, Path path, NodeImpl ni) throws RepositoryException
    {
        WorkspaceImpl ws = ni.getSession().getWorkspace();
        SessionImpl session = ni.getSession();
        
        if( state != ItemState.REMOVED && state != ItemState.MOVED && ni.getState() != ItemState.REMOVED )
            ni.autoCreateProperties();
        
        //
        //  Check that parent still exists
        //
        
        if( !ni.getInternalPath().isRoot() ) 
        {
            if( !ws.nodeExists(ni.getInternalPath().getParentPath()) )
            {
                throw new InvalidItemStateException("No parent available.");
            }
        }
        
        //
        //  Check if nobody has removed us if we were still supposed to exist.
        //
        
        if( state != ItemState.NEW && state != ItemState.REMOVED && state != ItemState.MOVED )
        {
            if( !ws.nodeExists(path) )
            {
                throw new InvalidItemStateException("Looks like this Node has been removed by another session: "+ni.getInternalPath().toString(session));
            }
            
            try
            {
                String uuid = ni.getUUID();
                
                NodeImpl currentNode = session.getNodeByUUID( uuid );
                
                if( !currentNode.getInternalPath().equals(ni.getInternalPath()) )
                    throw new InvalidItemStateException("Page has been moved");
            }
            catch( UnsupportedRepositoryOperationException e ){} // Not referenceable, so it's okay
        }
        
        //
        //  Check mandatory properties
        //
        if( state != ItemState.REMOVED && state != ItemState.MOVED && ni.getState() != ItemState.REMOVED )
        {
            //
            //  If this node is versionable, then make sure there is a VersionHistory as well.
            //
            
            if( ni.hasMixinType("mix:versionable") )
            {
                VersionManager.createVersionHistory( ni );
            }

            ni.checkMandatoryProperties( ni.getPrimaryQNodeType() );

            for( NodeType nt : ni.getMixinNodeTypes() )
            {
                ni.checkMandatoryProperties( ((QNodeType.Impl)nt).getQNodeType() );
            }

        }
        
    }
    
    public Collection<? extends PropertyImpl> getProperties(Path path) throws RepositoryException
    {
        HashMap<QName,PropertyImpl> result = new HashMap<QName,PropertyImpl>();
        
        //
        //  Find the node first
        //
        NodeImpl ni = null;
        
        try
        {
            ni = (NodeImpl) m_source.getItem( m_workspace, path );
            
            List<QName> propertyNames = m_source.listProperties( m_workspace, path );
            
            for( QName pName : propertyNames )
            {
                result.put( pName, (PropertyImpl)m_source.getItem( m_workspace, path.resolve(pName) ) );
            }
        }
        catch( RepositoryException e )
        {
            ItemImpl ii = m_changedItems.get( path );
            
            if( ii != null && ii.isNode() ) ni = (NodeImpl)ii;
        }
        
        if( ni == null )
        {
            throw new ItemNotFoundException("There is no such Node: "+path);
        }
        
        //
        //  Now, we need to collate the properties from the Node which was
        //  found with the properties which have been changed.  We put them all in the
        //  same hashmap and rely on the fact that there can't be two items with 
        //  the same key.
        //
        for( Iterator<ItemImpl> i = m_changedItems.values(); i.hasNext(); )
        {
            ItemImpl ii = i.next();
            if( ii.isNode() == false && ii.getInternalPath().getParentPath().equals(path) )
            {
                result.put( ii.getInternalPath().getLastComponent(), (PropertyImpl) ii );
            }
        }
        
        return result.values();
    }

    public void putProperty(NodeImpl impl, PropertyImpl property) throws RepositoryException
    {
//        addNode( impl );
        m_changedItems.add( property.getState(), property );
    }

    /**
     *  Refreshes all the items within the given path.
     *  
     *  @param keepChanges If true, does nothing (Priha implements COPY-ON-WRITE). If false,
     *                     purges all changes from the path.
     *  @param path The path from which to start refreshing.
     */
    public void refresh(boolean keepChanges, Path path)
    {
        //
        //  FIXME: Should notify the providers to refresh the caches, if caching
        //         is implemented.
        //
        if( keepChanges ) return;
        
        if( path.isRoot() ) 
        {
            m_changedItems.clear(); // Shortcut
            return;
        }
        
        for( Iterator<Change> c = m_changedItems.iterator(); c.hasNext(); )
        {
            Change change = c.next();
                        
            if( path.isParentOf(change.getItem().getInternalPath()) )
            {
                c.remove();
            }
        }
 
    }


    /**
     *  Goes directly into the repository, to find whether a Node exists currently.
     *  It ignores the transient state; so any new node additions or removals are
     *  ignored.
     *  
     *  @param path The path to check
     *  @return True, if the backend holds a given Node.
     *  @throws RepositoryException If something goes wrong.
     */
    public boolean nodeExistsInRepository( Path path ) throws RepositoryException
    {
        return m_source.nodeExists( m_workspace, path );
    }

    /**
     *  Provides a HashMap which has a maximum size. If the HashMap
     *  becomes full, it will start expelling the oldest entries.  It can
     *  be used to create a cache which does not grow bigger than limited.
     *  
     *  @param <K> Type of the key.
     *  @param <V> Type of the value.
     */
    private class SizeLimitedHashMap<K,V> extends LinkedHashMap<K,V>
    {
        private static final long serialVersionUID = 1L;
        private static final int MAX_SIZE = 100;
        private int m_maxSize = MAX_SIZE;
        
        /**
         *  Creates a SizeLimitedHashMap for a certain size.
         *  
         *  @param maxSize Maximum size.
         */
        public SizeLimitedHashMap(int maxSize)
        {
            super();
            m_maxSize = maxSize;
        }
        
        /**
         *  Creates a SizeLimitedHashMap with the default size {@value SizeLimitedHashMap#MAX_SIZE}.
         */
        public SizeLimitedHashMap()
        {
            super();
        }
        
        /**
         *  Returns true, making the underlying implementation remove the eldest item,
         *  when the hashmap has grown bigger than the specified maximum size.
         */
        @Override
        protected boolean removeEldestEntry(Map.Entry<K,V> eldest)
        {
            return size() > m_maxSize;
        }
    }

   
    public Path getPath( PathRef p ) throws PathNotFoundException
    {
        return c_sessionPathManager.getPath( p );
    }

    public PathManager getPathManager()
    {
        return c_sessionPathManager;
    }

}
