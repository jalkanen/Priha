package org.jspwiki.priha.core;

import java.util.*;
import java.util.Map.Entry;

import javax.jcr.Credentials;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;

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
    
    private List<NodeImpl>         m_nodes;
    private Map<Path,PropertyImpl> m_properties;
    
    public SessionProvider( SessionImpl session, ItemStore source )
    {
        m_session = session;
        m_source  = source;
        m_workspace = (WorkspaceImpl)session.getWorkspace();
        
        m_nodes = new ArrayList<NodeImpl>();
        m_properties = new HashMap<Path,PropertyImpl>();
    }
    
    public void save() throws RepositoryException
    {
        save( new Path("/") );
    }
    
    public void addNode( NodeImpl ni ) throws RepositoryException
    {
        m_nodes.add( ni );
    }

    public NodeImpl getNode( Path path ) throws InvalidPathException, RepositoryException
    {
        for( NodeImpl ni : m_nodes )
        {
            if( ni.getInternalPath().equals(path) )
            {
                return ni;
            }
        }
        
        return (NodeImpl)m_source.getItem(m_workspace, path);
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
        return m_source.findByUUID(m_workspace, uuid);
    }

    public PropertyImpl getProperty(Path path) throws RepositoryException
    {
        PropertyImpl propval = m_properties.get( path );
        
        if( propval == null )
        {
            propval = (PropertyImpl)m_source.getItem(m_workspace, path);
        }
        
        return propval;
    }

    public List<Path> listNodes(Path parentpath)
    {
        ArrayList<Path> res = new ArrayList<Path>();
        
        for( NodeImpl ni : m_nodes )
        {
            if( parentpath.isParentOf(ni.getInternalPath()) )
            {
                res.add( ni.getInternalPath() );
            }
        }
        
        res.addAll( m_source.listNodes(m_workspace, parentpath) );
        
        return res;
    }
/*
    public List<PropertyImpl> listProperties(Path path) throws RepositoryException
    {
        ArrayList<PropertyImpl> res = new ArrayList<PropertyImpl>();
        
        for( PropertyImpl p : m_properties.values() )
        {
            if( path.isParentOf(p.getInternalPath()) )
            {
                res.add( p );
            }
        }
        
        res.addAll( m_source.listProperties(m_workspace, path) );
        
        return res;
    }
*/
    public Collection<String> listWorkspaces()
    {
        return m_source.listWorkspaces();
    }

    public void move(Path srcpath, Path destpath) throws RepositoryException
    {
        m_source.move(m_workspace, srcpath, destpath);
    }

    public boolean nodeExists(Path path)
    {
        for( NodeImpl ni : m_nodes )
        {
            if( ni.getInternalPath().equals(path) ) return true;
        }
        
        return m_source.nodeExists(m_workspace, path);
    }

    public void open( Credentials credentials, String workspaceName)
        throws RepositoryException,
               NoSuchWorkspaceException
    {
        m_source.open((RepositoryImpl)m_session.getRepository(), credentials, workspaceName);
    }

    public void putProperty(PropertyImpl property) throws RepositoryException
    {
        m_properties.put( property.getInternalPath(), property );
    }

    public void remove(Path path) throws RepositoryException
    {
        m_source.remove(m_workspace, path);
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
        return !(m_nodes.isEmpty() && m_properties.isEmpty());
    }

    public void clear()
    {
        m_nodes.clear();
        m_properties.clear();
    }

    public void save(Path path) throws RepositoryException
    {
        for( Iterator<NodeImpl> i = m_nodes.iterator(); i.hasNext(); )
        {
            NodeImpl ni = i.next();
            
            if( path.isParentOf(ni.getInternalPath()) )
            {
                m_source.addNode( m_workspace, ni );
                i.remove();
            }
        }
        
        for( Iterator<Entry<Path, PropertyImpl>> i = m_properties.entrySet().iterator(); i.hasNext(); )
        {
            Entry<Path,PropertyImpl> e = i.next();
            if( path.isParentOf(e.getKey()) )
            {
                m_source.putProperty( m_workspace, e.getValue() );
            }
        }
    }

}
