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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Logger;

import javax.jcr.*;

import org.jspwiki.priha.Release;
import org.jspwiki.priha.util.ConfigurationException;
import org.jspwiki.priha.util.FileUtil;

public class RepositoryImpl implements Repository
{
    private static final String  STR_TRUE  = "true";
    private static final String  STR_FALSE = "false";

    public static final String   DEFAULT_WORKSPACE = "default";

    private NamespaceRegistry    m_namespaceRegistry;

    private static String[] DESCRIPTORS = {
        Repository.SPEC_NAME_DESC,                "Content Repository for Java Technology API",
        Repository.SPEC_VERSION_DESC,             "1.0",
        Repository.REP_NAME_DESC,                 Release.APPNAME,
        Repository.REP_VENDOR_DESC,               "Janne Jalkanen",
        Repository.REP_VENDOR_URL_DESC,           "http://www.ecyrd.com/",
        Repository.REP_VERSION_DESC,              Release.VERSTR,
        Repository.LEVEL_1_SUPPORTED,             STR_TRUE,
        Repository.LEVEL_2_SUPPORTED,             STR_TRUE,
        Repository.OPTION_TRANSACTIONS_SUPPORTED, STR_FALSE,
        Repository.OPTION_VERSIONING_SUPPORTED,   STR_FALSE,
        Repository.OPTION_LOCKING_SUPPORTED,      STR_TRUE,
        Repository.OPTION_OBSERVATION_SUPPORTED,  STR_FALSE,
        Repository.OPTION_QUERY_SQL_SUPPORTED,    STR_FALSE,
        Repository.QUERY_XPATH_POS_INDEX,         STR_FALSE,
        Repository.QUERY_XPATH_DOC_ORDER,         STR_FALSE
    };


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
            m_properties = new Properties( FileUtil.findProperties(DEFAULT_PROPERTIES) );
        }
        catch (IOException e)
        {
            throw new ConfigurationException("Loading of default properties failed");
        }
        if( prefs.isEmpty() )
            log.warning("No \"priha.properties\" found, using just the default properties");

        m_properties.putAll( prefs );

        m_providerManager = new ProviderManager( this );
        
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
    
    protected ProviderManager getProviderManager()
    {
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

    public Enumeration getPropertyNames()
    {
        return m_properties.propertyNames();
    }

    public String getDescriptor(String key)
    {
        for( int i = 0; i < DESCRIPTORS.length; i += 2 )
        {
            if( DESCRIPTORS[i].equals(key) )
            {
                return DESCRIPTORS[i+1];
            }
        }

        return null;
    }

    public String[] getDescriptorKeys()
    {
        String[] keys = new String[DESCRIPTORS.length/2];

        for( int i = 0; i < DESCRIPTORS.length; i += 2 )
        {
            keys[i/2] = DESCRIPTORS[i];
        }

        return keys;
    }

    public Session login(Credentials credentials, String workspaceName)
        throws LoginException,
               NoSuchWorkspaceException,
               RepositoryException
    {
        if( workspaceName == null ) workspaceName = DEFAULT_WORKSPACE;

        m_providerManager.open( credentials, workspaceName );

        SessionImpl session = m_sessionManager.openSession( credentials, workspaceName );

        session.refresh(false);

        return session;
    }

    public Session login(Credentials credentials) throws LoginException, RepositoryException
    {
        return login( credentials, null );
    }

    public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException
    {
        return login( null, workspaceName );
    }

    public Session login() throws LoginException, RepositoryException
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
        SessionImpl s = (SessionImpl)login( null, workspaceName );
        
        s.setSuper(true);
        
        return s;
    }
    
    public NamespaceRegistry getGlobalNamespaceRegistry()
    {
        if( m_namespaceRegistry == null )
        {
            m_namespaceRegistry = new NamespaceRegistryImpl();
        }

        return m_namespaceRegistry;
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
            SessionImpl session = new SessionImpl( RepositoryImpl.this, 
                                                   credentials, 
                                                   workspaceName );
            
            m_sessions.add( new WeakReference<SessionImpl>(session) );
            return session;
        }

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
            m_providerManager.stop();
        }
        
    }
}
