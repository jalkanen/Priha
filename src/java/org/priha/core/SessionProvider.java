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

import static org.priha.core.JCRConstants.Q_JCR_PRIMARYTYPE;

import java.util.*;
import java.util.Map.Entry;

import javax.jcr.*;
import javax.xml.namespace.QName;

import org.priha.util.InvalidPathException;
import org.priha.util.Path;

/**
 *  This is a special provider which stores the state of the Session.
 *  
 */
public class SessionProvider
{
    //private SessionImpl        m_session;
    private ItemStore          m_source;
    private WorkspaceImpl      m_workspace;
    
    private SortedMap<Path,ItemImpl> m_changedItems;
    
    // FIXME: This is a leaky cache.
    private Map<Path,ItemImpl> m_fetchedItems = new HashMap<Path,ItemImpl>();
    
    public SessionProvider( SessionImpl session, ItemStore source )
    {
        //m_session = session;
        m_source  = source;
        m_workspace = session.getWorkspace();
        
        //
        //  The nodes are sorted according to their length to make
        //  sure when they are saved, we save the parent path first.
        //
        m_changedItems = new TreeMap<Path,ItemImpl>( new PrimaryTypePreferringComparator() );
    }
    
    public void save() throws RepositoryException
    {
        save( Path.ROOT );
    }
    
    public void addNode( NodeImpl ni ) throws RepositoryException
    {
        m_changedItems.put( ni.getInternalPath(), ni );
    }

    public ItemImpl getItem( Path path ) throws InvalidPathException, RepositoryException
    {
        ItemImpl ii = m_changedItems.get(path);

        if( ii != null ) 
            return ii;
        
        ii = m_fetchedItems.get(path);
        
        if( ii != null )
            return ii;
        
        ii = m_source.getItem(m_workspace, path);
        
        if( ii != null ) m_fetchedItems.put( path, ii );
        
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
        for( ItemImpl ni : m_changedItems.values() )
        {
            try
            {
                if( ni.isNode() )
                {
                    String pid = ((NodeImpl)ni).getUUID();
                    if( uuid.equals(pid) ) return (NodeImpl)ni;
                }
            }
            catch(RepositoryException e) {}
        }
/*
        for( ItemImpl ni : m_fetchedItems.values() )
        {
            try
            {
                if( ni.isNode() )
                {
                    String pid = ((NodeImpl)ni).getUUID();
                    if( uuid.equals(pid) ) return (NodeImpl)ni;
                }
            }
            catch(RepositoryException e) {}
        }
*/
        NodeImpl ii = m_source.findByUUID( m_workspace, uuid );
        
        if( ii != null ) m_fetchedItems.put( ii.getInternalPath(), ii );
        
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
        
        for (ItemImpl ii : m_changedItems.values())
        {
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

    public Set<Path> listNodes(Path parentpath) throws RepositoryException
    {
        TreeSet<Path> res = new TreeSet<Path>();
        
        for( ItemImpl ni : m_changedItems.values() )
        {
            if( parentpath.isParentOf(ni.getInternalPath()) && ni.isNode() )
            {
                res.add( ni.getInternalPath() );
            }
        }
        
        res.addAll( m_source.listNodes(m_workspace, parentpath) );
        
        return res;
    }

    public Collection<? extends String> listWorkspaces() throws RepositoryException
    {
        return m_source.listWorkspaces();
    }

    public void move(Path srcpath, Path destpath) throws RepositoryException
    {
        m_source.move(m_workspace, srcpath, destpath);
    }

    public boolean nodeExists(Path path) throws RepositoryException
    {
        ItemImpl ni = m_changedItems.get( path );

        if( ni != null && ni.isNode() ) 
        {
            if( ni.getState() == ItemState.REMOVED ) return false;
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
        m_changedItems.put( item.getInternalPath(), item );
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

    public void save(Path path) throws RepositoryException
    {
        //
        //  Test referential integrity.
        //
        for( Iterator<Entry<Path, ItemImpl>> i = m_changedItems.entrySet().iterator(); i.hasNext(); )
        {
            Entry<Path, ItemImpl> entry = i.next();
            
            ItemImpl ii = entry.getValue();
            
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
                                throw new ReferentialIntegrityException("Attempted to remove a Node which still has references");
                        }
                    }
                    catch( UnsupportedRepositoryOperationException e )
                    {
                        // Does not have an UUID, so cannot be referenced.
                    }
                }
            }
        }
        
        List<Path> toberemoved = new ArrayList<Path>();
        
        List<ItemImpl> unsaved = new ArrayList<ItemImpl>();
        
        //
        //  Do the actual save.  The way we do this is that we simply just take the
        //  first one from the queue, and and attempt to save it.  This allows
        //  e.g. preSave() to create some additional properties before saving - 
        //  a basic Iterator over the Set would cause ConcurrentModificationExceptions.
        //
        //  All unsaved items are stored in a particular list, and then added back
        //  to the savequeue.
        //
        
        m_source.storeStarted( m_workspace );
        
        try
        {
            while( !m_changedItems.isEmpty() )
            {
                Entry<Path,ItemImpl> entry = m_changedItems.entrySet().iterator().next();
                ItemImpl ii = entry.getValue();
                    
                if( path.isParentOf(ii.getInternalPath()) || path.equals( ii.getInternalPath() ))
                {
                    if( ii.isNode() )
                    {
                        NodeImpl ni = (NodeImpl) ii;
                    
                        switch( ni.getState() )
                        {
                            case EXISTS:
                                ni.preSave();
                                // Nodes which exist don't need to be added.
                                ni.postSave();
                                break;
                            
                            case NEW:
                                ni.preSave();
                                m_source.addNode( m_workspace, ni );
                                ni.postSave();
                                m_fetchedItems.put( ni.getInternalPath(), ni );
                                break;
                        
                            case REMOVED:
                                if( !m_source.nodeExists( m_workspace, ii.getInternalPath() ) )
                                {
                                    throw new InvalidItemStateException("The item has been removed by some other Session "+ii.getInternalPath());
                                }
                                toberemoved.add(ni.getInternalPath());
                                m_fetchedItems.remove( ni.getInternalPath() );
                                break;
                        }
                    }
                    else
                    {
                        PropertyImpl pi = (PropertyImpl)ii;
                        
                        switch( pi.getState() )
                        {
                            case NEW:
                            case EXISTS:
                                pi.preSave();
                                m_source.putProperty( m_workspace, pi );
                                pi.postSave();
                                m_fetchedItems.put( pi.getInternalPath(), pi );
                                break;
                                
                            case REMOVED:
                                toberemoved.add(pi.getInternalPath());
                                m_fetchedItems.remove( pi.getInternalPath() );
                                break;                     
                        }
                    }                
                }
                else
                {
                    unsaved.add( ii );
                }
            
                //
                //  Remove from the queue so that we don't use it again.  This must
                //  be done here so that there never is an Item missing if any of the
                //  intermediate calls do e.g. hasProperty();
                //
                m_changedItems.remove( entry.getKey() );
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
                //System.out.println("Removing "+p);
                m_source.remove(m_workspace, p);
            }
            
            //
            //  Put the unsaved ones back into the queue
            //
            for( ItemImpl ii : unsaved )
            {
                m_changedItems.put( ii.getInternalPath(), ii );
            }
        }
        finally
        {
            m_source.storeFinished( m_workspace );
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
        for( ItemImpl ii : m_changedItems.values() )
        {
            if( ii.isNode() == false && ii.getInternalPath().getParentPath().equals(path) )
            {
                result.put( ii.getInternalPath().getLastComponent(), (PropertyImpl) ii );
            }
        }
        
        return result.values();
    }

    public void putProperty(NodeImpl impl, PropertyImpl property) throws RepositoryException
    {
        addNode( impl );
        m_changedItems.put( property.getInternalPath(), property );
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
        
        for( Iterator<Entry<Path, ItemImpl>> i = m_changedItems.entrySet().iterator(); i.hasNext(); )
        {
            Entry<Path, ItemImpl> entry = i.next();
            
            ItemImpl ii = entry.getValue();
            
            if( path.isParentOf(ii.getInternalPath()) )
            {
                i.remove();
            }
        }
 
    }

    /**
     *  A comparator which puts primarytypes first.
     */
    private static final class PrimaryTypePreferringComparator implements Comparator<Path>
    {
        public int compare(Path o1, Path o2)
        {
            int res = o1.depth() - o2.depth();
            
            if( res == 0 )
            {
                //
                //  OK, this is a bit kludgy.  We put the primaryType first so that
                //  we make sure it's the first property to be saved.
                //
                
                if( o1.getLastComponent().equals(Q_JCR_PRIMARYTYPE) && !o2.getLastComponent().equals(Q_JCR_PRIMARYTYPE) ) return -1;
                if( o2.getLastComponent().equals(Q_JCR_PRIMARYTYPE) && !o1.getLastComponent().equals(Q_JCR_PRIMARYTYPE) ) return 1;
                
                res = o1.toString().compareTo( o2.toString() );
            }
            
            return res;
        }
    }

    /**
     *  Goes directly into the repository, to find whether a Node exists currently.
     *  
     *  @param path
     *  @return
     *  @throws RepositoryException
     */
    public boolean nodeExistsInRepository( Path path ) throws RepositoryException
    {
        return m_source.nodeExists( m_workspace, path );
    }


}
