package org.jspwiki.priha.core;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.*;

import org.xml.sax.helpers.NamespaceSupport;

public class NamespaceRegistryImpl implements NamespaceRegistry
{
    public static final String NS_JCP_SV  = "http://www.jcp.org/jcr/sv/1.0";
    public static final String NS_JCP_MIX = "http://www.jcp.org/jcr/mix/1.0";
    public static final String NS_JCP_NT  = "http://www.jcp.org/jcr/nt/1.0";
    public static final String NS_JCP     = "http://www.jcp.org/jcr/1.0";
    
    /** Maps prefixes to URIs.  Prefixes are always unique, therefore they are the keys */ 
    private HashMap<String,String> m_nsmap = new HashMap<String,String>();
    
    public NamespaceRegistryImpl()
    {
        m_nsmap.put("jcr", NS_JCP);
        m_nsmap.put("nt",  NS_JCP_NT);
        m_nsmap.put("mix", NS_JCP_MIX);
        m_nsmap.put("xml", NamespaceSupport.XMLNS);
        m_nsmap.put("sv",  NS_JCP_SV);
        m_nsmap.put("", "");
    }
    
    public String getPrefix(String uri) throws NamespaceException, RepositoryException
    {
        for( Map.Entry<String,String> entry : m_nsmap.entrySet() )
        {
            if( entry.getValue().equals(uri) ) return entry.getKey();
        }
        
        throw new NamespaceException("Prefix for URI does not exist: "+uri);
    }

    public String[] getPrefixes() throws RepositoryException
    {
        return m_nsmap.keySet().toArray(new String[0]);
    }

    public String getURI(String prefix) throws NamespaceException, RepositoryException
    {
        String u = m_nsmap.get(prefix);
        
        if( u == null ) throw new NamespaceException( "No such prefix: "+prefix );
        
        return u;
    }

    public String[] getURIs() throws RepositoryException
    {
        return m_nsmap.values().toArray( new String[0] );
    }

    public void registerNamespace(String prefix, String uri)
                                                            throws NamespaceException,
                                                                UnsupportedRepositoryOperationException,
                                                                AccessDeniedException,
                                                                RepositoryException
    {
        throw new UnsupportedRepositoryOperationException("NamespaceRegistry.registerNamespace()");
    }

    public void unregisterNamespace(String prefix)
                                                  throws NamespaceException,
                                                      UnsupportedRepositoryOperationException,
                                                      AccessDeniedException,
                                                      RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException("NamespaceRegistry.unregisterNamespace()");

    }

    /**
     *  Turns a string of the form "prefix:name" to "{url}name"
     *  
     * @param val
     * @return
     * @throws RepositoryException 
     * @throws NamespaceException 
     */
    public String toQName( String val ) throws NamespaceException, RepositoryException
    {
        int idx = val.indexOf(':');
        if( idx != -1 )
        {
            String prefix = val.substring(0,idx);
            String name   = val.substring(idx+1);
            
            String uri = getURI(prefix);
     
            return "{"+uri+"}"+name;
        }
        
        return val;
    }
    
    /**
     * Turns a string of the form "{uri}name" to "prefix:name".
     * 
     * @param val
     * @return
     * @throws NamespaceException
     * @throws RepositoryException
     */
    public String fromQName( String val ) throws NamespaceException, RepositoryException
    {
        int idx = val.indexOf('}');
        if( idx != -1 )
        {
            String uri = val.substring(1,idx);
            String name = val.substring(idx+1);
            
            String prefix = getPrefix(uri);
            
            return prefix+":"+name;
        }
        return val;
    }
}
