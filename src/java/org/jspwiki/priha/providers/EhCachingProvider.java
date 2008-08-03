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
package org.jspwiki.priha.providers;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.jcr.Credentials;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.management.MBeanServer;

import net.sf.ehcache.*;
import net.sf.ehcache.constructs.blocking.LockTimeoutException;
import net.sf.ehcache.management.ManagementService;

import org.jspwiki.priha.core.PropertyImpl;
import org.jspwiki.priha.core.ProviderManager;
import org.jspwiki.priha.core.RepositoryImpl;
import org.jspwiki.priha.core.WorkspaceImpl;
import org.jspwiki.priha.util.ConfigurationException;
import org.jspwiki.priha.util.Path;

/**
 *  Uses EHCache for an intermediate level cache.  It lies on top of a real provider,
 *  like FileProvider, and provides caching for most of the items that can be fetched
 *  from the cache.  Configurable properties are:
 *  <ul>
 *    <li><b>realProvider</b> - The nickname of the underlying provider to use.</li>
 *    <li><b>cacheName</b> - A name for the cache under which it gets registered to
 *                           the EHCache CacheManager.  If a cache by this name already
 *                           exists (e.g. from the ehcache configuration XML file),
 *                           it will be used.  If it does not exist, we create one.</li>
 *    <li><b>size</b> - The size of the cache (in items; default is 5000)</li>
 *  </ul>
 *  Priha creates a memory only cache.
 */
public class EhCachingProvider implements RepositoryProvider
{
    private static final int    DEFAULT_CACHESIZE = 5000;
    private static final String DEFAULT_CACHENAME = "priha.ehCache";
    
    private Logger log = Logger.getLogger(EhCachingProvider.class.getName());
    private CacheManager m_cacheManager;
    private Ehcache m_valueCache;
    private RepositoryProvider m_realProvider;
    
    public EhCachingProvider()
    {
    }

    public void start(RepositoryImpl repository, Properties properties) throws ConfigurationException
    {
        m_cacheManager = CacheManager.getInstance();
        
        String realProviderNick = properties.getProperty("realProvider");
        
        Properties props = ProviderManager.filterProperties(repository, realProviderNick);
        
        String className = props.getProperty("class");
        
        String workspaceList = properties.getProperty("workspaces");
        if( workspaceList != null ) props.setProperty("workspaces", workspaceList);
        
        m_realProvider = ProviderManager.instantiateProvider(repository, className, props);

        String cacheName = props.getProperty("cacheName", DEFAULT_CACHENAME);
        
        int size = Integer.parseInt( props.getProperty("size", Integer.toString(DEFAULT_CACHESIZE) ) );
        
        Ehcache myCache = m_cacheManager.getEhcache( cacheName );
        
        if( myCache == null )
        {
            myCache = new Cache(cacheName, size, false, true, 30, 20);

            //
            //  For some completely unfathomable reason, the BlockingCache blocks
            //  on the same thread every 5-10 calls to getPropertyValue() on OSX 10.4,
            //  both Java5 and Java6.  So until I figure out what is going on,
            //  we can't use it :-(
            //
            
//            myCache = new BlockingCache(myCache));
//            
//            ((BlockingCache)myCache).setTimeoutMillis( 10*1000 );

            m_cacheManager.addCache(myCache);
        }
        
        m_valueCache = m_cacheManager.getEhcache( cacheName );
        
        //
        //  Register to MBeanServer
        //
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        
        try
        {
            ManagementService.registerMBeans(m_cacheManager, mBeanServer, false, false, false, true);
        }
        catch( CacheException e )
        {
            // Failure is not fatal.
        }

        log.fine("Started EHCache with real provider class "+m_realProvider.getClass().getName());
    }

    public void stop(RepositoryImpl rep)
    {
        m_realProvider.stop(rep);
        m_cacheManager.shutdown();
        log.fine("Stopped EHCache");
    }

    /**
     *  Gives an unique ID with which to use as the cache key for Values.
     *  
     *  @param ws
     *  @param path
     *  @return
     */
    private final String getVid(WorkspaceImpl ws, Path path)
    {
        return ws.getName()+";"+path.toString()+";V";
    }

    /** For the Property name lists */
    private final String getPid(WorkspaceImpl ws, Path path)
    {
        return ws.getName()+";"+path.toString()+";P";
    }

    /** For the Node child lists */
    private final String getNid(WorkspaceImpl ws, Path path)
    {
        return ws.getName()+";"+path.toString()+";N";
    }

    public void open(RepositoryImpl rep, Credentials credentials, String workspaceName) throws RepositoryException, NoSuchWorkspaceException
    {
        m_realProvider.open(rep, credentials, workspaceName);
    }


    public void close(WorkspaceImpl ws)
    {
        Statistics s = m_valueCache.getStatistics();
        
        log.fine("EHCache statistics right before close(): "+s.toString());
        
        m_realProvider.close(ws);
        m_valueCache.removeAll();
    }

    public void addNode(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        if( !path.isRoot() )
            m_valueCache.remove( getNid(ws,path.getParentPath()) );
        
        m_realProvider.addNode(ws, path);
    }

    public void copy(WorkspaceImpl ws, Path srcpath, Path destpath) throws RepositoryException
    {
        m_realProvider.copy(ws, srcpath, destpath);
    }

    public Path findByUUID(WorkspaceImpl ws, String uuid) throws RepositoryException
    {
        return m_realProvider.findByUUID(ws, uuid);
    }

    public List<Path> findReferences(WorkspaceImpl ws, String uuid) throws RepositoryException
    {
        return m_realProvider.findReferences(ws, uuid);
    }

    public Object getPropertyValue(WorkspaceImpl ws, Path path) throws RepositoryException
    {
//        System.out.print("Property: "+Thread.currentThread());
//        System.out.flush();
//        long start = System.currentTimeMillis();
        try
        {
            Element e = m_valueCache.get( getVid(ws,path) );
            if( e != null )
            {
                return e.getObjectValue();
            }
        
            Object o = m_realProvider.getPropertyValue(ws, path);
        
            e = new Element( getVid(ws,path), o );
        
            m_valueCache.put( e );
            return o;
        }
        catch( LockTimeoutException e )
        {
            throw new RepositoryException("Lock timeout getting propery value");
        }
        catch( RuntimeException e )
        {
            // Release lock
            m_valueCache.put( new Element(getVid(ws,path),null) );
            throw new RepositoryException("Error getting propery value");
        }
        finally {
//            System.out.println(" --- released: "+(System.currentTimeMillis() - start));
        }
    }

    @SuppressWarnings("unchecked")
    public List<Path> listNodes(WorkspaceImpl ws, Path parentpath) throws RepositoryException
    {
//        System.out.print ("listNodes: "+Thread.currentThread());
//        long start = System.currentTimeMillis();

        try
        {
            Element e = m_valueCache.get( getNid(ws,parentpath) );
            if( e != null ) return (List<Path>)e.getValue();
        
            List<Path> list = m_realProvider.listNodes(ws, parentpath);
        
            e = new Element( getNid(ws,parentpath), list );
        
            m_valueCache.put( e );
        
            return list;
        }
        catch( LockTimeoutException e )
        {
            throw new RepositoryException("Lock timeout getting propery value");
        }
        catch( RuntimeException e )
        {
            // Release lock
            m_valueCache.put( new Element(getNid(ws,parentpath),null) );
            throw new RepositoryException("Error getting propery value");
        }
        finally {
//            System.out.println(" --- released: "+(System.currentTimeMillis() - start));
        }

    }

    @SuppressWarnings("unchecked")
    public List<String> listProperties(WorkspaceImpl ws, Path path) throws RepositoryException
    {
//        System.out.print ("listProperties: "+Thread.currentThread());
//        long start = System.currentTimeMillis();

        try
        {
            Element e = m_valueCache.get( getPid(ws,path) );
        
            if( e != null ) return (List<String>)e.getValue();
        
            List<String> list = m_realProvider.listProperties(ws, path);
        
            e = new Element( getPid(ws,path), list );
            m_valueCache.put( e );
            
            return list;
        }
        catch( LockTimeoutException e )
        {
            throw new RepositoryException("Lock timeout listing properties");
        }
        catch( RuntimeException e )
        {
            // Release lock
            m_valueCache.put( new Element(getPid(ws,path),null) );
            throw new RepositoryException("Error listing properties");
        }
        finally {
//            System.out.println(" --- released: "+(System.currentTimeMillis() - start));
        }

    }

    public Collection<String> listWorkspaces()
    {
        return m_realProvider.listWorkspaces();
    }

    public void move(WorkspaceImpl ws, Path srcpath, Path destpath) throws RepositoryException
    {
        m_realProvider.move(ws, srcpath, destpath);
    }

    public boolean nodeExists(WorkspaceImpl ws, Path path)
    {
        if( m_valueCache.isKeyInCache( getNid(ws,path) ) )
            return true;
        
        return m_realProvider.nodeExists(ws, path);
    }

    public void putPropertyValue(WorkspaceImpl ws, PropertyImpl property) throws RepositoryException
    {
        m_valueCache.remove( getVid(ws,property.getInternalPath() ) );
        m_realProvider.putPropertyValue(ws, property);
    }

    public void remove(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        m_realProvider.remove(ws,path);
        m_valueCache.remove( getVid(ws,path) );
        m_valueCache.remove( getPid(ws,path) );
        m_valueCache.remove( getNid(ws,path) );
       
        if( !path.isRoot() )
            m_valueCache.remove( getNid(ws,path.getParentPath()) );
    }
}
