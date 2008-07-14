package org.jspwiki.priha.core;

import java.util.*;
import java.util.Map.Entry;

import javax.jcr.*;

import org.jspwiki.priha.util.InvalidPathException;
import org.jspwiki.priha.util.Path;

/**
 *  This is a special provider which stores the state of the Session.
 *  
 */
public class SessionProvider
{
    private SessionImpl        m_session;
    private ItemStore          m_source;
    private WorkspaceImpl      m_workspace;
    
    private SortedMap<Path,ItemImpl> m_items;
    
    public SessionProvider( SessionImpl session, ItemStore source )
    {
        m_session = session;
        m_source  = source;
        m_workspace = (WorkspaceImpl)session.getWorkspace();
        
        //
        //  The nodes are sorted according to their length to make
        //  sure when they are saved, we save the parent path first.
        //
        m_items = new TreeMap<Path,ItemImpl>( new Comparator<Path>() {
            public int compare(Path o1, Path o2)
            {
                int res = o1.depth() - o2.depth();
                
                if( res == 0 )
                {
                    //
                    //  OK, this is a bit kludgy.  We put the primaryType first so that
                    //  we make sure it's the first property to be saved.
                    //
                    
                    if( o1.getLastComponent().equals("jcr:primaryType") && !o2.getLastComponent().equals("jcr:primaryType") ) return -1;
                    if( o2.getLastComponent().equals("jcr:primaryType") && !o1.getLastComponent().equals("jcr:primaryType") ) return 1;
                    
                    res = o1.toString().compareTo( o2.toString() );
                }
                
                return res;
            };
        });
    }
    
    public void save() throws RepositoryException
    {
        save( Path.ROOT );
    }
    
    public void addNode( NodeImpl ni ) throws RepositoryException
    {
        m_items.put( ni.getInternalPath(), ni );
    }

    public ItemImpl getItem( Path path ) throws InvalidPathException, RepositoryException
    {
        ItemImpl ii = m_items.get(path);

        if( ii != null ) 
            return ii;
        
        return m_source.getItem(m_workspace, path);
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
        for( ItemImpl ni : m_items.values() )
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
        
        return m_source.findByUUID( m_workspace, uuid );
    }

    /**
     *  Finds all references to the given UUID.
     *  
     *  @param uuid
     *  @return
     *  @throws RepositoryException
     */
    public Collection<PropertyImpl> getReferences( String uuid ) throws RepositoryException
    {
        TreeSet<PropertyImpl> response = new TreeSet<PropertyImpl>();
        
        for (ItemImpl ii : m_items.values())
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
        
        return response;
    }

    public List<Path> listNodes(Path parentpath)
    {
        ArrayList<Path> res = new ArrayList<Path>();
        
        for( ItemImpl ni : m_items.values() )
        {
            if( parentpath.isParentOf(ni.getInternalPath()) && ni.isNode() )
            {
                res.add( ni.getInternalPath() );
            }
        }
        
        res.addAll( m_source.listNodes(m_workspace, parentpath) );
        
        return res;
    }

    public Collection<? extends String> listWorkspaces()
    {
        return m_source.listWorkspaces();
    }

    public void move(Path srcpath, Path destpath) throws RepositoryException
    {
        m_source.move(m_workspace, srcpath, destpath);
    }

    public boolean nodeExists(Path path)
    {
        ItemImpl ni = m_items.get( path );

        if( ni != null && ni.isNode() ) 
        {
            if( ni.getState() == ItemState.REMOVED ) return false;
            return true;
        }
        
        return m_source.nodeExists(m_workspace, path);
    }

    public void open( Credentials credentials, String workspaceName)
        throws RepositoryException,
               NoSuchWorkspaceException
    {
        m_source.open((RepositoryImpl)m_session.getRepository(), credentials, workspaceName);
    }

    public void remove(ItemImpl item) throws RepositoryException
    {
        m_items.put( item.getInternalPath(), item );
    }

    public void start()
    {
        m_source.start( (RepositoryImpl)m_session.getRepository() );
    }

    public void stop()
    {
        m_source.stop((RepositoryImpl)m_session.getRepository());
    }

    public boolean hasPendingChanges()
    {
        return !m_items.isEmpty();
    }

    public void clear()
    {
        m_items.clear();
    }

    public void save(Path path) throws RepositoryException
    {
        for( Iterator<Entry<Path, ItemImpl>> i = m_items.entrySet().iterator(); i.hasNext(); )
        {
            Entry<Path, ItemImpl> entry = i.next();
            
            ItemImpl ii = entry.getValue();
            
            if( path.isParentOf(ii.getInternalPath()) )
            {
                if( ii.isNode() )
                {
                    NodeImpl ni = (NodeImpl) ii;
                    
                    switch( ni.getState() )
                    {
                        case NEW:
                        case EXISTS:
                            ni.preSave();
                            m_source.addNode( m_workspace, ni );
                            ni.postSave();
                            break;
                        
                        case REMOVED:
                            m_source.remove( m_workspace, ni.getInternalPath() );
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
                            break;
                                
                        case REMOVED:
                            m_source.remove( m_workspace, pi.getInternalPath() );
                            break;                     
                    }
                }

                i.remove();
                
            }
        }
    }

    public Collection<? extends PropertyImpl> getProperties(Path path) throws RepositoryException
    {
        HashMap<String,PropertyImpl> result = new HashMap<String,PropertyImpl>();
        
        //
        //  Find the node first
        //
        NodeImpl ni = null;
        
        try
        {
            ni = (NodeImpl) m_source.getItem( m_workspace, path );
            
            List<String> propertyNames = m_source.listProperties( m_workspace, path );
            
            for( String pName : propertyNames )
            {
                result.put( pName, (PropertyImpl)m_source.getItem( m_workspace, path.resolve(pName) ) );
            }
        }
        catch( RepositoryException e )
        {
            ItemImpl ii = m_items.get( path );
            
            if( ii != null && ii.isNode() ) ni = (NodeImpl)ii;
        }
        
        if( ni == null )
        {
            throw new ItemNotFoundException("There is no such Node");
        }
        
        //
        //  Now, we need to collate the properties from the Node which was
        //  found with the properties which have been changed.  We put them all in the
        //  same hashmap and rely on the fact that there can't be two items with 
        //  the same key.
        //
        for( ItemImpl ii : m_items.values() )
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
        m_items.put( property.getInternalPath(), property );
    }

}
