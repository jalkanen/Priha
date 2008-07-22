package org.jspwiki.priha.providers;

import java.util.*;

import javax.jcr.*;

import org.jspwiki.priha.core.PropertyImpl;
import org.jspwiki.priha.core.RepositoryImpl;
import org.jspwiki.priha.core.WorkspaceImpl;
import org.jspwiki.priha.util.ConfigurationException;
import org.jspwiki.priha.util.Path;

/**
 *  Holds the contents in memory only.  It even contains some
 *  optimization for UUID-based access.
 */
public class MemoryProvider implements RepositoryProvider
{
    private Map<Path,Object> m_values    = new Hashtable<Path,Object>();
    private Set<Path>        m_nodePaths = new TreeSet<Path>();
    private Map<String,Path> m_uuids     = new Hashtable<String,Path>();
    
    public void addNode(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        m_nodePaths.add( path );
    }

    public void close(WorkspaceImpl ws)
    {
    }

    public void copy(WorkspaceImpl ws, Path srcpath, Path destpath) throws RepositoryException
    {
        throw new UnsupportedRepositoryOperationException();
    }

    public Path findByUUID(WorkspaceImpl ws, String uuid) throws RepositoryException
    {
        return m_uuids.get( uuid );
    }

    public List<Path> findReferences(WorkspaceImpl ws, String uuid) throws RepositoryException
    {
        ArrayList<Path> res = new ArrayList<Path>();
        
        for( Map.Entry<Path,Object> e : m_values.entrySet() )
        {
            if( e.getValue() instanceof Value && 
                e.getKey().getLastComponent().equals("jcr:uuid") )
            {
                if( ((Value)e.getValue()).getString().equals(uuid) )
                {
                    res.add( e.getKey() );
                }
            }
        }
        
        return res;
    }

    public Object getPropertyValue(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        Object o = m_values.get(path);
        
        if( o == null ) throw new PathNotFoundException();
        
        return o;
    }

    public List<Path> listNodes(WorkspaceImpl ws, Path parentpath) throws RepositoryException
    {
        ArrayList<Path> res = new ArrayList<Path>();
        
        for( Path p : m_nodePaths )
        {
            if( parentpath.isParentOf(p) && !parentpath.equals(p) )
            {
                res.add(p);
            }
        }
        
        return res;
    }

    public List<String> listProperties(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        ArrayList<String> res = new ArrayList<String>();
        
        for( Path p : m_values.keySet() )
        {
            if( path.isParentOf(p) && !path.equals(p) )
            {
                res.add(p.getLastComponent());
            }
        }
        
        return res;
    }

    public Collection<String> listWorkspaces()
    {
        return Arrays.asList( new String[] { "default" } );
    }

    public void move(WorkspaceImpl ws, Path srcpath, Path destpath) throws RepositoryException
    {
        throw new UnsupportedRepositoryOperationException();
    }

    public boolean nodeExists(WorkspaceImpl ws, Path path)
    {
        return m_nodePaths.contains(path);
    }

    public void open(RepositoryImpl rep, Credentials credentials, String workspaceName)
                                                                                       throws RepositoryException,
                                                                                           NoSuchWorkspaceException
    {
        if( !workspaceName.equals("default") ) throw new NoSuchWorkspaceException();
    }

    public void putPropertyValue(WorkspaceImpl ws, PropertyImpl property) throws RepositoryException
    {
        if( property.getDefinition().isMultiple() )
        {
            Value[] values = property.getValues();
            
            m_values.put( property.getInternalPath(), values );
        }
        else
        {
            Value value = property.getValue();
            if( property.getName().equals("jcr:uuid") )
            {
                m_uuids.put( value.getString(), property.getInternalPath() );
            }
            
            m_values.put( property.getInternalPath(), value );    
        }
        
        // System.out.println("Stored "+property.getInternalPath());
    }

    public void remove(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        boolean wasThere = m_nodePaths.remove(path);
        
        if( !wasThere )
        {
            m_values.remove( path );
            
            for( Map.Entry<String,Path> e : m_uuids.entrySet() )
            {
                if( e.getValue().equals(path) )
                {
                    m_uuids.remove(e.getKey());
                    break;
                }
            }
        }
    }

    public void start(RepositoryImpl repository, Properties properties) throws ConfigurationException
    {
    }

    public void stop(RepositoryImpl rep)
    {
    }

}
