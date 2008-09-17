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
import org.jspwiki.priha.version.VersionHistoryImpl;
import org.jspwiki.priha.version.VersionManager;

/**
 *  This is a front-end class for managing multiple providers
 *  for a single repository.
 *  <p>
 *  The ProviderManager is a singleton per Repository.
 */
public class ProviderManager implements ItemStore
{
    private HashMap<String,ProviderInfo> m_workspaceAccess;
    private ProviderInfo[]       m_providers;
    
    private RepositoryImpl       m_repository;
    
    private Logger               log = Logger.getLogger(ProviderManager.class.getName());
    
    public ProviderManager( RepositoryImpl repository ) throws ConfigurationException
    {
        m_repository = repository;

        initialize();
    }

    public static final String PROP_PRIHA_PROVIDERS       = "priha.providers";
    public static final String PROP_PRIHA_PROVIDER_PREFIX = "priha.provider.";
    public static final String DEFAULT_PROVIDERLIST       = "defaultProvider";
    
    private void initialize() throws ConfigurationException
    {
        m_workspaceAccess = new HashMap<String,ProviderInfo>();

        String providerList = m_repository.getProperty( PROP_PRIHA_PROVIDERS );
        
        String[] providers = providerList.split("\\s");
        
        if( providers.length == 0 )
            throw new ConfigurationException("Required property missing",PROP_PRIHA_PROVIDERS);

        m_providers = new ProviderInfo[providers.length];
        
        for( int i = 0; i < providers.length; i++ )
        {
            Properties props = filterProperties(m_repository,providers[i]);
        
            String className = props.getProperty("class");
            
            if( className == null )
                throw new ConfigurationException("Provider "+providers[i]+" does not declare a class name. "+
                                                 "There should be a property called '"+PROP_PRIHA_PROVIDER_PREFIX+providers[i]+".class' in your properties.");                
            
            String workspaceList = props.getProperty("workspaces","default");
            
            String[] workspaces = workspaceList.split("\\s");
            
            log.fine("Provider "+i+": "+providers[i]+" is a "+className);
            RepositoryProvider p = instantiateProvider( m_repository, className, props );

            m_providers[i] = new ProviderInfo();
            m_providers[i].provider   = p;
            m_providers[i].workspaces = workspaces;
            
            for( String ws : workspaces )
            {
                if( m_workspaceAccess.containsKey(ws) )
                    throw new ConfigurationException("Workspace "+ws+" is defined multiple times.  Very likely you have forgotten to declare a workspace property for a Provider declaration.");
                
                m_workspaceAccess.put(ws, m_providers[i]);
            }
        }
    }
    
    /**
     *  [0] = /largefiles/
     *  [1] = /
     *  
     *  "/"  => 1
     *  "/largefiles" => 0
     *  "/largefiles2" => 1
     *  "/largefiles/foo" => 0
     *  
     * @param p
     * @return
     * @throws ConfigurationException
     */
    private final RepositoryProvider getProvider(WorkspaceImpl wi, Path p) throws ConfigurationException
    {
        ProviderInfo pi = m_workspaceAccess.get(wi.getName());

        if( pi != null )
            return pi.provider;
        
        throw new ConfigurationException("Nonexistant workspace: "+wi.getName());
    }
    
    /**
     *  Filters the properties set when the Repository was created to find
     *  the property set for a provider.
     *  <p>
     *  Essentially returns a property set where all instances of
     *  <code>priha.provider.[providerName].property = value</code>
     *  are replaced with <code>property = value</code>.
     *  
     * @param repository The repository from which the properties are read
     * @param providerName The name to filter with
     * @return A valid set of Properties.  It can also be empty, if there
     *         were no properties defined for this providerName.
     * @see ProviderManager#instantiateProvider(RepositoryImpl, String, Properties) 
     */
    public static Properties filterProperties( RepositoryImpl repository, String providerName )
    {
        Properties props = new Properties();
        
        String prefix = PROP_PRIHA_PROVIDER_PREFIX + providerName + ".";
        
        for( Enumeration<String> e = repository.getPropertyNames(); e.hasMoreElements(); )
        {
            String key = e.nextElement();
            
            if( key.startsWith(prefix) )
            {
                String val = repository.getProperty(key);
                key = key.substring(prefix.length());
                
                props.setProperty(key, val);
            }
        }
        
        return props;
    }
    
    /**
     *  Instantiates a RepositoryProvider using the given class name and
     *  the properties, and calls its start() method.
     *  
     *  @param rep The RepositoryImpl who will own this RepositoryProvider
     *  @param className The FQN of the class.
     *  @param props A filtered set of Properties
     *  @return A started RepositoryProvider.
     *  
     *  @throws ConfigurationException If the provider cannot be instantiated or the configuration
     *                                 is faulty.
     *                                 
     *  @see ProviderManager#filterProperties(RepositoryImpl, String)
     */
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
    
    public void open(Credentials credentials, String workspaceName) 
        throws NoSuchWorkspaceException, RepositoryException
    {
        ProviderInfo pi = m_workspaceAccess.get(workspaceName);

        if( pi == null ) throw new NoSuchWorkspaceException("No such workspace: '"+workspaceName+"'");
        
        pi.provider.open( m_repository, credentials, workspaceName );
    }


    private Object getPropertyValue(WorkspaceImpl impl, Path ptPath) throws RepositoryException
    {
        Object stored = getProvider(impl,ptPath).getPropertyValue( impl, ptPath );
            
        return stored;
    }

    /**
     *  Returns the set of workspaces declared in the config file.
     *  
     *  @throws ConfigurationException 
     */
    public Collection<String> listWorkspaces() throws ConfigurationException
    {
        return m_workspaceAccess.keySet();
    }

    public List<Path>listNodes(WorkspaceImpl impl, Path path) throws RepositoryException
    {
        ArrayList<Path> list = new ArrayList<Path>();
        
        for( ProviderInfo pi : m_providers )
        {
            list.addAll( pi.provider.listNodes( impl, path ) );
        }
        
        return list;
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
        getProvider(impl,path).remove( impl, path );
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
        
        Path ptPath = path.resolve("jcr:primaryType");
        PropertyImpl primaryType = ws.createPropertyImpl( ptPath );
    
        ValueImpl v = (ValueImpl)getPropertyValue( ws, ptPath );
        
        if( v == null )
            throw new RepositoryException("Repository did not return a primary type for path "+path);
    
        primaryType.loadValue( v );
        
        NodeTypeManagerImpl ntm = (NodeTypeManagerImpl)ws.getNodeTypeManager();
        GenericNodeType type = (GenericNodeType) ntm.getNodeType( primaryType.getString() );
    
        NodeDefinition nd = ntm.findNodeDefinition( primaryType.getString() );
    
        if( VersionManager.isVersionHistoryPath(path) )
        {
            ni = VersionHistoryImpl.getInstance( ws.getSession(), path );
        }
        else
        {
            ni = new NodeImpl( ws.getSession(), path, type, nd, false );
        }
        
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
        
        if( !path.isRoot() && !nodeExists(ws, path.getParentPath()) )
            throw new ConstraintViolationException("Parent path is missing");
        
        getProvider(ws,path).addNode(ws, path);
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
        
    }

    public boolean nodeExists(WorkspaceImpl ws, Path path) throws ConfigurationException
    {
        return getProvider(ws,path).nodeExists(ws, path);
    }

    public void putProperty(WorkspaceImpl ws, PropertyImpl pi) throws RepositoryException
    {
        getProvider(ws,pi.getInternalPath()).putPropertyValue( ws, pi );   
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
            
            result.add( nd.getProperty(path.getLastComponent()) );
        }
        
        return result;
    }

    public List<String> listProperties(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        return getProvider(ws,path).listProperties(ws, path);
    }

    private class ProviderInfo
    {
        public String[]           workspaces;
        public RepositoryProvider provider;
    }
}
