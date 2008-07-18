package org.jspwiki.priha.core;

import java.util.*;
import java.util.logging.Logger;

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
import org.jspwiki.priha.util.ConfigurationException;
import org.jspwiki.priha.util.InvalidPathException;
import org.jspwiki.priha.util.Path;

/**
 *  This is a front-end class for managing multiple providers
 *  for a single repository.
 *  <p>
 *  The ProviderManager is a singleton per Repository.
 */
public class ProviderManager implements ItemStore
{
    private ProviderInfo[]       m_providers;

    private RepositoryImpl       m_repository;
    
    private Logger               log = Logger.getLogger(ProviderManager.class.getName());
    
    public ProviderManager( RepositoryImpl repository ) throws ConfigurationException
    {
        m_repository = repository;

        initialize();
    }

    public static final String PROP_PRIHA_PROVIDERS = "priha.providers";
    public static final String PROP_PRIHA_PROVIDER_PREFIX = "priha.provider.";
    public static final String DEFAULT_PROVIDERLIST = "defaultProvider";
    
    private void initialize() throws ConfigurationException
    {
        String providerList = m_repository.getProperty( PROP_PRIHA_PROVIDERS );
        
        String[] providers = providerList.split("\\s");
        
        if( providers.length == 0 )
            throw new ConfigurationException("Required property missing",PROP_PRIHA_PROVIDERS);

        m_providers = new ProviderInfo[providers.length];
        for( int i = 0; i < providers.length; i++ )
        {
            Properties props = filterProperties(m_repository,providers[i]);
        
            String className = props.getProperty("class");
            String prefix    = props.getProperty("prefix","");

            log.fine("Provider "+i+": "+providers[i]+" is a "+className);
            RepositoryProvider p = instantiateProvider( m_repository, className, props );

            m_providers[i] = new ProviderInfo();
            m_providers[i].provider = p;
            m_providers[i].prefix   = prefix;            
        }
    }
    
    private final RepositoryProvider getProvider(Path p) throws ConfigurationException
    {
        String path = p.toString();
        
        for( ProviderInfo pi : m_providers )
        {
            if( path.startsWith(pi.prefix) ) return pi.provider;
        }
        
        throw new ConfigurationException("Unmapped path found: "+p);
    }
    
    public static Properties filterProperties( RepositoryImpl repository, String providerName )
    {
        Properties props = new Properties();
        
        String prefix = PROP_PRIHA_PROVIDER_PREFIX + providerName + ".";
        
        for( Enumeration e = repository.getPropertyNames(); e.hasMoreElements(); )
        {
            String key = (String)e.nextElement();
            
            if( key.startsWith(prefix) )
            {
                String val = repository.getProperty(key);
                key = key.substring(prefix.length());
                
                props.setProperty(key, val);
            }
        }
        
        return props;
    }
    
    public static RepositoryProvider instantiateProvider(RepositoryImpl rep, String className, Properties props) throws ConfigurationException
    {
        Class cl;
        RepositoryProvider provider;
        try
        {
            cl = Class.forName( className );
            provider = (RepositoryProvider) cl.newInstance();
            provider.start( rep, props );
        }
        catch (ClassNotFoundException e)
        {
            throw new ConfigurationException("Could not find provider class",className);
        }
        catch (InstantiationException e)
        {
            throw new ConfigurationException("Could not instantiate provider class",className);
        }
        catch (IllegalAccessException e)
        {
            throw new ConfigurationException("Could not access provider class",className);
        }
 
        return provider;
    }
    
    /**
     *  Checks whether a node exists in the repository.
     *  
     * @param path
     * @return
     * @throws InvalidPathException 
     */
    public boolean hasNode( WorkspaceImpl ws, Path path ) throws InvalidPathException, ConfigurationException
    {
        return getProvider(path).nodeExists( ws, path );
    }
    
    public void open(Credentials credentials, String workspaceName) 
        throws NoSuchWorkspaceException, RepositoryException
    {
        for( ProviderInfo pi : m_providers )
        {
            pi.provider.open( m_repository, credentials, workspaceName );
        }
    }


    private Object getPropertyValue(WorkspaceImpl impl, Path ptPath) throws RepositoryException
    {
        Object stored = getProvider(ptPath).getPropertyValue( impl, ptPath );
            
        return stored;
    }

    /**
     *  Workspaces are determined by the provider which maps against the root path.
     * @throws ConfigurationException 
     */
    public Collection<String> listWorkspaces() throws ConfigurationException
    {
        return getProvider(Path.ROOT).listWorkspaces();
    }

    public List<Path>listNodes(WorkspaceImpl impl, Path path) throws ConfigurationException
    {
        return getProvider(path).listNodes( impl, path );
    }

    public void close(WorkspaceImpl impl)
    {
        for( ProviderInfo pi : m_providers )
        {
            pi.provider.close( impl );
        }
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
        getProvider(path).remove( impl, path );
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
        ni.m_state = ItemState.EXISTS;

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
        p.m_state = ItemState.EXISTS;
        
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
        Path path = ni.getInternalPath();
        
        if( !path.isRoot() && !getProvider(path).nodeExists(ws, path.getParentPath()) )
            throw new ConstraintViolationException("Parent path is missing");
        
        getProvider(path).addNode(ws, path);
    }

    public void copy(WorkspaceImpl m_workspace, Path srcpath, Path destpath) throws RepositoryException
    {
        throw new UnsupportedRepositoryOperationException();
    }

    public NodeImpl findByUUID(WorkspaceImpl ws, String uuid) throws RepositoryException
    {
        for( ProviderInfo pi : m_providers )
        {
            try
            {
                Path path = pi.provider.findByUUID(ws, uuid);
        
                return loadNode(ws, path);
            }
            catch(ItemNotFoundException e)
            {}
        }
        
        throw new ItemNotFoundException("Could not locate a node by this UUID");
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
            
            return loadProperty( ws, ni, path, path.getLastComponent() );
        }
    }

    public void move(WorkspaceImpl m_workspace, Path srcpath, Path destpath) throws RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();
    }

    public boolean nodeExists(WorkspaceImpl ws, Path path) throws ConfigurationException
    {
        return getProvider(path).nodeExists(ws, path);
    }

    public void putProperty(WorkspaceImpl ws, PropertyImpl pi) throws RepositoryException
    {
        getProvider(pi.getInternalPath()).putPropertyValue( ws, pi );   
    }

    public void stop()
    {
        for( ProviderInfo pi : m_providers )
        {
            pi.provider.stop(m_repository);
        }
    }

    public Collection<? extends PropertyImpl> getReferences(WorkspaceImpl ws, String uuid) throws RepositoryException
    {
        ArrayList<PropertyImpl> result = new ArrayList<PropertyImpl>();
        
        List<Path> paths = new ArrayList<Path>();
        
        for( ProviderInfo pi : m_providers )
        {
            paths.addAll(pi.provider.findReferences( ws, uuid ));
        }
        
        for( Path path : paths )
        {
            NodeImpl nd = loadNode( ws, path.getParentPath() );
            
            result.add( (PropertyImpl)nd.getProperty(path.getLastComponent()) );
        }
        
        return result;
    }

    public List<String> listProperties(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        return getProvider(path).listProperties(ws, path);
    }

    private class ProviderInfo
    {
        public String             prefix;
        public RepositoryProvider provider;
    }
}
