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
package org.priha.core;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Logger;

import javax.jcr.*;

import org.priha.Release;
import org.priha.core.namespace.NamespaceRegistryImpl;
import org.priha.util.ConfigurationException;
import org.priha.util.FileUtil;

public class RepositoryImpl implements Repository
{
    public static final String   DEFAULT_WORKSPACE = "default";

    private static NamespaceRegistryImpl c_namespaceRegistry = new NamespaceRegistryImpl();

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

    private SessionManager m_sessionManager = new SessionManager();
    private ProviderManager m_providerManager;
    
    public RepositoryImpl( Properties prefs ) throws ConfigurationException
    {
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
    }

    
    public String getProperty(String key)
    {
        return m_properties.getProperty(key);
    }

    public String getProperty( String key, String defValue )
    {
        return m_properties.getProperty(key,defValue);
    }
    
    protected ProviderManager getProviderManager() throws ConfigurationException
    {
        if( m_providerManager == null )
        {
            log.info( "Initializing providers..." );
            m_providerManager = new ProviderManager(this);
        }
        
        return m_providerManager;
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
        if( workspaceName == null ) workspaceName = getDefaultWorkspace();

        getProviderManager().open( credentials, workspaceName );

        SessionImpl session = m_sessionManager.openSession( credentials, workspaceName );

        session.refresh(false);

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
        synchronized( m_sessionManager.m_sessions )
        {
            for( Iterator<WeakReference<SessionImpl>> i = m_sessionManager.m_sessions.iterator(); i.hasNext(); )
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
        m_sessionManager.closeSession(s);
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
            log.fine( "Running shutdown process..." );
            if( m_providerManager != null ) m_providerManager.stop();
        }
        
    }
}
