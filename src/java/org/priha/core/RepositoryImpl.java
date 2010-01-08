/*
    Priha - A JSR-170 implementation library.

    Copyright (C) 2007-2009 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jcr.*;

import org.priha.Release;
import org.priha.core.namespace.NamespaceRegistryImpl;
import org.priha.util.ConfigurationException;
import org.priha.util.FileUtil;

/**
 *  Provides the main Repository class for Priha. You may use this
 *  by simply instantiating it with a suitable Properties object.
 *  <p>
 *  Any property may be overridden also from the command line using
 *  a system property.
 */
public class RepositoryImpl implements Repository
{
    public static final String   DEFAULT_WORKSPACE = "default";

    private static NamespaceRegistryImpl c_namespaceRegistry = new NamespaceRegistryImpl();

    private RepositoryState m_state = RepositoryState.DEAD; 
    
    /**
     *  Defines which paths are attempted to locate the default property file.
     */
    private static final String[] DEFAULT_PROPERTIES = 
    {
       "/priha_default.properties",
       "/WEB-INF/priha_default.properties"
    };
    
    private Properties m_properties;

    private Logger log = Logger.getLogger( getClass().getName() );

    private SessionManager  m_sessionManager;
    private ProviderManager m_providerManager;
    
    /**
     *  Create a new Repository using the given properties.  Any property which is not
     *  set will be read from the default properties (priha_default.properties).
     *  
     *  @param prefs Properties to use.  Must not be null.
     *  @throws ConfigurationException If the properties are wrong.
     */
    public RepositoryImpl( Properties prefs ) throws ConfigurationException
    {
        m_state = RepositoryState.STARTING;
        
        try
        {
            Properties defaultProperties = FileUtil.findProperties(DEFAULT_PROPERTIES);
            
            if( defaultProperties.isEmpty() )
                throw new ConfigurationException("Default properties not found - broken distribution!?!");
   
            m_properties = new Properties(defaultProperties);
        }
        catch (IOException e)
        {
            throw new ConfigurationException("Loading of default properties failed");
        }
                
        if( prefs.isEmpty() )
            log.warning("No \"priha.properties\" found, using just the default properties");

        m_properties.putAll( prefs );

        Runtime.getRuntime().addShutdownHook( new ShutdownThread() );
        
        log.info( "G'day, Matilda!  Priha "+Release.VERSTR+" has been initialized." );
        log.fine( "Using configuration from "+prefs.toString() );
        
        m_state = RepositoryState.LIVE;
    }

    
    public String getProperty(String key)
    {
        String prop = System.getProperty(key, m_properties.getProperty(key));
        
        return prop;
    }

    public String getProperty( String key, String defValue )
    {
        String prop = System.getProperty( key, m_properties.getProperty(key,defValue) );
        
        return prop;
    }
    
    protected ProviderManager getProviderManager() throws ConfigurationException
    {
        if( m_providerManager == null )
        {
            log.info( "Initializing providers..." );
            long start = System.currentTimeMillis();
            m_providerManager = new ProviderManager(this);
            long end = System.currentTimeMillis();
            log.info( "Repository initialization took "+(end-start)+" ms." );
        }
        
        return m_providerManager;
    }
    
    protected SessionManager getSessionManager()
    {
        if( m_sessionManager == null )
        {
            log.info( "Initializing SessionManager..." );
            m_sessionManager = new SessionManager();
        }
        
        return m_sessionManager;
    }
    
    /**
     *  Set transient properties for this repository.  These are not saved anywhere, but they
     *  might be something that you can use to control the Repository behaviour with.
     *  @param key
     *  @param property
     */
    public void setProperty(String key, String property)
    {
        m_properties.setProperty(key,property);
    }

    @SuppressWarnings("unchecked")
    public Enumeration<String> getPropertyNames()
    {
        return (Enumeration<String>)m_properties.propertyNames();
    }

    public String getDescriptor(String key)
    {
        for( int i = 0; i < Release.DESCRIPTORS.length; i += 2 )
        {
            if( Release.DESCRIPTORS[i].equals(key) )
            {
                return Release.DESCRIPTORS[i+1];
            }
        }

        return null;
    }

    public String[] getDescriptorKeys()
    {
        String[] keys = new String[Release.DESCRIPTORS.length/2];

        for( int i = 0; i < Release.DESCRIPTORS.length; i += 2 )
        {
            keys[i/2] = Release.DESCRIPTORS[i];
        }

        return keys;
    }

    private String getDefaultWorkspace() throws ConfigurationException
    {
        return getProviderManager().getDefaultWorkspace();
    }
    
    public synchronized SessionImpl login(Credentials credentials, String workspaceName)
        throws LoginException,
               NoSuchWorkspaceException,
               RepositoryException
    {
        if( m_state != RepositoryState.LIVE )
        {
            throw new RepositoryException("Repository is not alive.  It is "+m_state);
        }
        long start = System.currentTimeMillis();
        
        if( workspaceName == null ) workspaceName = getDefaultWorkspace();

        getProviderManager().open( credentials, workspaceName );

        SessionImpl session = getSessionManager().openSession( credentials, workspaceName );

        session.refresh(false);

        long end = System.currentTimeMillis();
        
        if( log.isLoggable(Level.FINER) )
            log.finer("Login completed and new Session created; took "+(end-start)+" ms.");
        
        return session;
    }

    public SessionImpl login(Credentials credentials) throws LoginException, RepositoryException
    {
        return login( credentials, null );
    }

    public SessionImpl login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException
    {
        return login( null, workspaceName );
    }

    public SessionImpl login() throws LoginException, RepositoryException
    {
        return login( null, null );
    }

    /**
     *  Shuts down the entire repository, stops all providers and releases
     *  resources.  After this call you can restart the repository by either
     *  calling login(), or creating a new RepositoryImpl instance.
     */
    public void shutdown()
    {
        m_state = RepositoryState.SHUTTING;
        
        //
        //  Close all open sessions.
        //
        visit( new SessionVisitor() 
        {
            public void visit(SessionImpl session)
            {
                session.logout();
            }   
        });
        
        //
        //  Stop the ProviderManager and release providers.
        //
        if( m_providerManager != null )
        {
            m_providerManager.stop();
        }
        
        m_providerManager = null;
        m_sessionManager  = null;
        
        m_state = RepositoryState.DEAD;
    }
    
    /**
     *  Returns a Session which has write permissions to the repository.  Normally, the
     *  user has no reason to use this method - it is used internally to sometimes modify
     *  the repo.
     *  <p>
     *  This method is guaranteed to always return a Session which has all permissions
     *  into the repository - assuming the underlying repository implementation does not
     *  have any limitations (which it normally should not have).
     *  
     *  @param workspaceName The workspace to which the login is done.
     *  @return A Priha SessionImpl object.
     *  @throws LoginException
     *  @throws NoSuchWorkspaceException
     *  @throws RepositoryException
     */
    public SessionImpl superUserLogin(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException
    {
        SessionImpl s = login( null, workspaceName );
        
        s.setSuper(true);
        
        return s;
    }
    
    public static NamespaceRegistryImpl getGlobalNamespaceRegistry()
    {
        return c_namespaceRegistry;
    }

    /**
     *  Visits all current Sessions
     * 
     *  @param v
     */
    protected void visit( SessionVisitor v )
    {
        synchronized( getSessionManager().m_sessions )
        {
            for( Iterator<WeakReference<SessionImpl>> i = getSessionManager().m_sessions.iterator(); i.hasNext(); )
            {
                WeakReference<SessionImpl> wr = i.next();
            
                SessionImpl si = wr.get();
            
                if( si != null ) v.visit(si);
                else i.remove();
            }
        }
    }
    
    protected void removeSession(SessionImpl s)
    {
        getSessionManager().closeSession(s);
    }
    
    /**
     *  The SessionManager holds a list of Sessions currently owned by this Repository.  This
     *  is used to manage the proper shutdown of the Sessions.
     *
     */
    // FIXME: Should probably have a ReferenceQueue for cleaning away things like locks, etc.
    private class SessionManager
    {
        private ArrayList<WeakReference<SessionImpl>> m_sessions = new ArrayList<WeakReference<SessionImpl>>();
        
        public SessionImpl openSession( Credentials credentials, String workspaceName ) 
            throws RepositoryException
        {
            synchronized(m_sessions)
            {
                SessionImpl session = new SessionImpl( RepositoryImpl.this, 
                                                       credentials, 
                                                       workspaceName );
            
                m_sessions.add( new WeakReference<SessionImpl>(session) );
                return session;
            }
        }
        
        public void closeSession( SessionImpl s )
        {
            //
            //  Make sure that if we're in the midst of shutdown, these get removed
            //  automatically.
            //
            if( m_state == RepositoryImpl.RepositoryState.SHUTTING )
                return;
            
            synchronized(m_sessions)
            {
                for( Iterator<WeakReference<SessionImpl>> i = m_sessions.iterator(); i.hasNext(); )
                {
                    WeakReference<SessionImpl> wr = i.next();
                    
                    SessionImpl si = wr.get();
                    
                    if( si == null || si == s )
                    {
                        i.remove();
                        break;
                    }
                }

                // FIXME: Should really check if there is a Session for this workspace
                //        so that we can call ProviderManager.close();
            }
        }
    }
    
    protected interface SessionVisitor
    {
        public void visit( SessionImpl session );
    }
    
    /**
     *  Stops the Provider which stores all the things in this Repository.  This is used to
     *  manage a sane shutdown.
     */
    private class ShutdownThread extends Thread
    {
        @Override
        public void run()
        {
            log.info( "Running shutdown process (repository closed due to JVM termination)" );
            shutdown();
        }
        
    }
    
    private enum RepositoryState {
        LIVE,
        SHUTTING,
        STARTING,
        DEAD
    };
    

}
