package org.jspwiki.priha.core;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.jcr.*;

import org.jspwiki.priha.Release;
import org.jspwiki.priha.providers.RepositoryProvider;

public class RepositoryImpl implements Repository
{
    private static final String  STR_TRUE  = "true";
    private static final String  STR_FALSE = "false";

    public static final String   DEFAULT_WORKSPACE = "default";

    private NamespaceRegistry    m_namespaceRegistry;

    private Preferences          m_preferences;

    private static String[] DESCRIPTORS = {
        Repository.SPEC_NAME_DESC,                "Content Repository for Java Technology API",
        Repository.SPEC_VERSION_DESC,             "1.0",
        Repository.REP_NAME_DESC,                 Release.APPNAME,
        Repository.REP_VENDOR_DESC,               "jspwiki.org",
        Repository.REP_VENDOR_URL_DESC,           "http://www.jspwiki.org/",
        Repository.REP_VERSION_DESC,              Release.VERSTR,
        Repository.LEVEL_1_SUPPORTED,             STR_TRUE,
        Repository.LEVEL_2_SUPPORTED,             STR_TRUE,
        Repository.OPTION_TRANSACTIONS_SUPPORTED, STR_FALSE,
        Repository.OPTION_VERSIONING_SUPPORTED,   STR_FALSE,
        Repository.OPTION_LOCKING_SUPPORTED,      STR_FALSE,
        Repository.OPTION_OBSERVATION_SUPPORTED,  STR_FALSE,
        Repository.OPTION_QUERY_SQL_SUPPORTED,    STR_FALSE,
        Repository.QUERY_XPATH_POS_INDEX,         STR_FALSE,
        Repository.QUERY_XPATH_DOC_ORDER,         STR_FALSE
    };

    private Properties m_properties = new Properties();

    private Logger log = Logger.getLogger( getClass().getName() );

    private SessionManager m_sessionManager = new SessionManager();
    private ProviderManager m_providerManager;
    
    public RepositoryImpl( Preferences prefs ) throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        m_preferences = prefs;

        m_providerManager = new ProviderManager( this, prefs );
        
        log.info( "G'day, Matilda!  Priha has been initialized." );
        log.fine( "Using configuration from "+prefs.toString() );
    }

    public String getProperty(String key)
    {
        return m_properties.getProperty(key);
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
        return m_properties.keys();
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

    public Preferences getPreferences()
    {
        return m_preferences;
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

    public NamespaceRegistry getGlobalNamespaceRegistry()
    {
        if( m_namespaceRegistry == null )
        {
            m_namespaceRegistry = new NamespaceRegistryImpl();
        }

        return m_namespaceRegistry;
    }

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
}
