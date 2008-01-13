package org.jspwiki.priha.core;

import java.util.Collection;
import java.util.List;
import java.util.prefs.Preferences;

import javax.jcr.Credentials;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;

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
 *
 */
public class ProviderManager
{
    private static final int   DEFAULT_CACHE_SIZE = 1;
    private RepositoryProvider m_provider;
    public static final String DEFAULT_PROVIDER = "org.jspwiki.priha.providers.FileProvider";
    private RepositoryImpl     m_repository;
    
    private Cache              m_nodeCache;
    
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

        m_nodeCache = new Cache( true, false, false, false, "com.opensymphony.oscache.base.algorithm.LRUCache", DEFAULT_CACHE_SIZE );
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
        try
        {
            NodeImpl ni = (NodeImpl) m_nodeCache.getFromCache( path.toString() );
            
            return true;
        }
        catch (NeedsRefreshException e)
        {
            m_nodeCache.cancelUpdate( path.toString() );
            return m_provider.nodeExists( ws, path );
        }
    }
    
    public void open(Credentials credentials, String workspaceName) 
        throws NoSuchWorkspaceException, RepositoryException
    {
        m_provider.open( m_repository, credentials, workspaceName );
    }

    /**
     *  Convenience method for storing an entire NodeImpl.  Will only store
     *  modified properties.
     *  
     *  @param ws The workspace which wishes to make the save.
     *  @param node
     *  @throws RepositoryException
     */
    public void saveNode(WorkspaceImpl ws, NodeImpl node) throws RepositoryException
    {
        if( node.isNew() )
        {
            m_provider.addNode( ws, node.getInternalPath() );
        }
        
        for( PropertyIterator pi = node.getProperties(); pi.hasNext(); )
        {
            PropertyImpl p = (PropertyImpl)pi.nextProperty();
        
            if( p.isNew() || p.isModified() )
            {
                m_provider.putPropertyValue( ws, p );
            }
        }

        m_nodeCache.putInCache( node.getPath().toString(), node );
    }

    public List<String> listProperties(WorkspaceImpl impl, Path path) throws RepositoryException
    {
        return m_provider.listProperties( impl, path );
    }

    private Object getPropertyValue(WorkspaceImpl impl, Path ptPath) throws RepositoryException
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

    /**
     *  Removes the item at the end of the path.
     *  
     *  @param impl
     *  @param path
     *  @throws RepositoryException
     */
    public void remove(WorkspaceImpl impl, Path path) throws RepositoryException
    {
        m_nodeCache.removeEntry( path.toString() );
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
    public NodeImpl loadNode( WorkspaceImpl ws, Path path ) throws RepositoryException
    {
        NodeImpl ni = null;
        
        try
        {
            //
            //  We'll check the cache for this node.  If it exists, we create
            //  a clone (because the state of the node is always relevant to the Session)
            //  and send it back up.
            //
            ni = (NodeImpl)m_nodeCache.getFromCache( path.toString() );
            
            if( ni.getSession() == ws.getSession() )
                return ni;
            
            return new NodeImpl( ni, (SessionImpl)ws.getSession() );
        }
        catch( NeedsRefreshException e )
        {
            List<String> properties = listProperties( ws, path );
    
            Path ptPath = path.resolve("jcr:primaryType");
            PropertyImpl primaryType = ws.createPropertyImpl( ptPath );
    
            ValueImpl v = (ValueImpl)getPropertyValue( ws, ptPath );
        
            if( v == null )
                throw new RepositoryException("Repository did not return a primary type for path "+path);
    
            primaryType.setValue( v );
        
            NodeTypeManagerImpl ntm = (NodeTypeManagerImpl)ws.getNodeTypeManager();
            GenericNodeType type = (GenericNodeType) ntm.getNodeType( primaryType.getString() );
    
            NodeDefinition nd = ntm.findNodeDefinition( primaryType.getString() );
    
            ni = new NodeImpl( (SessionImpl)ws.getSession(), path, type, nd );
    
            properties.remove("jcr:primaryType"); // Already handled.
        
            for( String name : properties )
            {
                ptPath = path.resolve(name);
            
                Object values = getPropertyValue( ws, ptPath );
    
                PropertyImpl p = ws.createPropertyImpl( ptPath );
    
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
            
            List<Path> children = listNodes( ws, ni.getInternalPath() );
            
            ni.setChildren( children );
            
            m_nodeCache.putInCache( path.toString(), ni );
            
            return ni;
        }
        finally
        {
            if( ni == null ) m_nodeCache.cancelUpdate( path.toString() );
        }
            
    }

}
