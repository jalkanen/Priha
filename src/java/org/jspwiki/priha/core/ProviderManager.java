package org.jspwiki.priha.core;

import java.util.Collection;
import java.util.List;
import java.util.prefs.Preferences;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

import org.jspwiki.priha.core.values.ValueImpl;
import org.jspwiki.priha.nodetype.GenericNodeType;
import org.jspwiki.priha.nodetype.NodeTypeManagerImpl;
import org.jspwiki.priha.providers.RepositoryProvider;
import org.jspwiki.priha.util.InvalidPathException;
import org.jspwiki.priha.util.Path;

import com.opensymphony.oscache.base.Cache;
import com.opensymphony.oscache.base.NeedsRefreshException;

/**
 *  This is a front-end class for managing single or, in the future, multiple providers
 *  for a single repository.
 *  <p>
 *  This class also provides caching and some additional helper functions over
 *  the regular Provider interface.
 *  <p>
 *  The ProviderManager is a singleton per Repository.
 */
public class ProviderManager implements ItemStore
{
    private static final int   DEFAULT_CACHE_SIZE = 1;
    private RepositoryProvider m_provider;
    private static final String DEFAULT_PROVIDER = "org.jspwiki.priha.providers.FileProvider";
    private RepositoryImpl     m_repository;
    
    public ProviderManager( RepositoryImpl repository, Preferences prefs )
    {
        String className = prefs.get( "provider",  DEFAULT_PROVIDER );

        m_repository = repository;
        
        Class cl;
        try
        {
            cl = Class.forName( className );
            m_provider = (RepositoryProvider) cl.newInstance();
            m_provider.start( repository );
        }
        catch (ClassNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace( );
        }
        catch (InstantiationException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     *  Checks whether a node exists in the repository.
     *  
     * @param path
     * @return
     * @throws InvalidPathException 
     */
    public boolean hasNode( WorkspaceImpl ws, Path path ) throws InvalidPathException
    {
        return m_provider.nodeExists( ws, path );
    }
    
    public void open(Credentials credentials, String workspaceName) 
        throws NoSuchWorkspaceException, RepositoryException
    {
        m_provider.open( m_repository, credentials, workspaceName );
    }


    private Object getPropertyValue(WorkspaceImpl impl, Path ptPath) throws RepositoryException
    {
        Object stored = m_provider.getPropertyValue( impl, ptPath );
            
        return stored;
    }

    public Collection<String> listWorkspaces()
    {
        return m_provider.listWorkspaces();
    }

    public List<Path>listNodes(WorkspaceImpl impl, Path path)
    {
        return m_provider.listNodes( impl, path );
    }

    public void close(WorkspaceImpl impl)
    {
        m_provider.close( impl );
    }

    /**
     *  Removes the item at the end of the path.
     *  
     *  @param impl
     *  @param path
     *  @throws RepositoryException
     */
    public void remove(WorkspaceImpl impl, Path path) throws RepositoryException
    {
        m_provider.remove( impl, path );
    }

    /**
     * Loads the state of a node from the repository.
     *
     * @param ws TODO
     * @param path
     * @return A brand new NodeImpl.
     *
     * @throws RepositoryException
     */
    NodeImpl loadNode( WorkspaceImpl ws, Path path ) throws RepositoryException
    {
        NodeImpl ni = null;
        
        List<String> properties = m_provider.listProperties( ws, path );
    
        Path ptPath = path.resolve("jcr:primaryType");
        PropertyImpl primaryType = ws.createPropertyImpl( ptPath );
    
        ValueImpl v = (ValueImpl)getPropertyValue( ws, ptPath );
        
        if( v == null )
            throw new RepositoryException("Repository did not return a primary type for path "+path);
    
        primaryType.loadValue( v );
        
        NodeTypeManagerImpl ntm = (NodeTypeManagerImpl)ws.getNodeTypeManager();
        GenericNodeType type = (GenericNodeType) ntm.getNodeType( primaryType.getString() );
    
        NodeDefinition nd = ntm.findNodeDefinition( primaryType.getString() );
    
        ni = new NodeImpl( (SessionImpl)ws.getSession(), path, type, nd, false );
    
        for( String name : properties )
        {
            ptPath = path.resolve(name);
            
            PropertyImpl p = loadProperty(ws, ni, ptPath, name);
            
            ni.addChildProperty( p );            
        }

        return ni;
    }

    private PropertyImpl loadProperty(WorkspaceImpl ws, NodeImpl ni, Path ptPath, String name)
        throws RepositoryException,
        ValueFormatException,
        VersionException,
        LockException,
        ConstraintViolationException
    {
        Object values = getPropertyValue( ws, ptPath );
   
        PropertyImpl p = ws.createPropertyImpl( ptPath );
   
        boolean multiple = values instanceof ValueImpl[];
   
        PropertyDefinition pd = ((GenericNodeType)ni.getPrimaryNodeType()).findPropertyDefinition(name,multiple);
        p.setDefinition( pd );
        
        if( multiple )
            p.loadValue( (ValueImpl[]) values );
        else
            p.loadValue( (ValueImpl) values );
        
        return p;
    }

    public void addNode(WorkspaceImpl ws, NodeImpl ni) throws RepositoryException
    {
        m_provider.addNode(ws, ni.getInternalPath());
    }

    public void copy(WorkspaceImpl m_workspace, Path srcpath, Path destpath) throws RepositoryException
    {
        throw new UnsupportedRepositoryOperationException();
    }

    public NodeImpl findByUUID(WorkspaceImpl ws, String uuid) throws RepositoryException
    {
        Path path = m_provider.findByUUID(ws, uuid);
        
        return loadNode(ws, path);
    }

    public ItemImpl getItem(WorkspaceImpl ws, Path path) throws InvalidPathException, RepositoryException
    {
        try
        {
            NodeImpl ni = loadNode( ws, path );
            
            return ni;
        }
        catch( RepositoryException e )
        {
            NodeImpl ni = loadNode( ws, path.getParentPath() );
            
            return (PropertyImpl) ni.getChildProperty( path.getLastComponent() );
        }
    }

    public void move(WorkspaceImpl m_workspace, Path srcpath, Path destpath) throws RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();
    }

    public boolean nodeExists(WorkspaceImpl ws, Path path)
    {
        return m_provider.nodeExists(ws, path);
    }

    public void open(RepositoryImpl repository, Credentials credentials, String workspaceName) throws NoSuchWorkspaceException, RepositoryException
    {
        m_provider.open(repository, credentials, workspaceName);
    }

    public void putProperty(WorkspaceImpl ws, PropertyImpl pi) throws RepositoryException
    {
        m_provider.putPropertyValue( ws, pi );   
    }

    public void start(RepositoryImpl repository)
    {
        m_provider.start(repository);
    }

    public void stop(RepositoryImpl repository)
    {
        m_provider.stop(repository);
    }

}
