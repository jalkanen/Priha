package org.jspwiki.priha.core;

import java.util.Collection;
import java.util.List;
import java.util.prefs.Preferences;

import javax.jcr.Credentials;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;

import org.jspwiki.priha.core.values.ValueImpl;
import org.jspwiki.priha.nodetype.GenericNodeType;
import org.jspwiki.priha.nodetype.NodeTypeManagerImpl;
import org.jspwiki.priha.providers.RepositoryProvider;
import org.jspwiki.priha.util.InvalidPathException;
import org.jspwiki.priha.util.Path;

/**
 *  This is a front-end class for managing single or, in the future, multiple providers
 *  for a single repository.
 *  <p>
 *  This class also provides caching and some additional helper functions over
 *  the regular Provider interface.
 *
 */
public class ProviderManager
{
    private RepositoryProvider m_provider;
    public static final String DEFAULT_PROVIDER = "org.jspwiki.priha.providers.FileProvider";
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
            e.printStackTrace();
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
        
        /*
        if( path.isRoot() ) return true;
        
        List<Path> nodes = m_provider.listNodes( ws, path.getParentPath() );
        
        for( Path p : nodes )
        {
            if( p.getLastComponent().equals(path.getLastComponent()) )
            {
                // Found it
                
                return true;
            }
        }
        
        return false;
        */
    }
    
    public void open(Credentials credentials, String workspaceName) 
        throws NoSuchWorkspaceException, RepositoryException
    {
        m_provider.open( m_repository, credentials, workspaceName );
    }

    public void putNode(WorkspaceImpl impl, NodeImpl node) throws RepositoryException
    {
        m_provider.putNode( impl, node );
    }

    public List<String> listProperties(WorkspaceImpl impl, Path path) throws RepositoryException
    {
        return m_provider.listProperties( impl, path );
    }

    public Object getPropertyValue(WorkspaceImpl impl, Path ptPath) throws RepositoryException
    {
        return m_provider.getPropertyValue( impl, ptPath );
    }

    public Collection<String> listWorkspaces()
    {
        return m_provider.listWorkspaces();
    }

    public List<Path> listNodes(WorkspaceImpl impl, Path path)
    {
        return m_provider.listNodes( impl, path );
    }

    public void close(WorkspaceImpl impl)
    {
        m_provider.close( impl );
    }

    public void remove(WorkspaceImpl impl, Path path) throws RepositoryException
    {
        m_provider.remove( impl, path );
    }

    /**
     * Loads the state of a node from the repository.
     *
     * @param impl TODO
     * @param path
     * @return A brand new NodeImpl.
     *
     * @throws RepositoryException
     */
    public NodeImpl loadNode( WorkspaceImpl impl, Path path ) throws RepositoryException
    {
        List<String> properties = listProperties( impl, path );
    
        Path ptPath = path.resolve("jcr:primaryType");
        PropertyImpl primaryType = impl.createPropertyImpl( ptPath );
    
        ValueImpl v = (ValueImpl)getPropertyValue( impl, ptPath );
        
        if( v == null )
            throw new RepositoryException("Repository did not return a primary type for path "+path);
    
        primaryType.setValue( v );
        
        NodeTypeManagerImpl ntm = (NodeTypeManagerImpl)impl.getNodeTypeManager();
        GenericNodeType type = (GenericNodeType) ntm.getNodeType( primaryType.getString() );
    
        NodeDefinition nd = ntm.findNodeDefinition( primaryType.getString() );
    
        NodeImpl ni = new NodeImpl( (SessionImpl)impl.getSession(), path, type, nd );
    
        properties.remove("jcr:primaryType"); // Already handled.
        
        for( String name : properties )
        {
            ptPath = path.resolve(name);
            
            Object values = getPropertyValue( impl, ptPath );
    
            PropertyImpl p = impl.createPropertyImpl( ptPath );
    
            boolean multiple = values instanceof ValueImpl[];
    
            PropertyDefinition pd = ((GenericNodeType)ni.getPrimaryNodeType()).findPropertyDefinition(name,multiple);
            p.setDefinition( pd );
            
            if( multiple )
                p.setValue( (ValueImpl[]) values );
            else
                p.setValue( (ValueImpl) values );
            
            ni.addChildProperty( p );            
        }
    
        //
        //  Children
        //
        
        List<Path> children = listNodes( impl, ni.getInternalPath() );
        
        ni.setChildren( children );
        
        return ni;
    }

}
