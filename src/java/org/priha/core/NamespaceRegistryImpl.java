package org.priha.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

public class NamespaceRegistryImpl
{
    /** Maps prefixes to URIs.  Prefixes are always unique, therefore they are the keys */
    protected HashMap<String,String> m_nsmap = new HashMap<String,String>();

    public NamespaceRegistryImpl()
    {
        super();
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

    public void registerNamespace(String prefix, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException
    {
        m_nsmap.put( prefix, uri );
    }

    public void unregisterNamespace(String prefix) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException
    {
        m_nsmap.remove( prefix );
    }

    /**
     *  Turns a string of the form "prefix:name" to "{url}name"
     *  
     * @param val
     * @return
     * @throws RepositoryException 
     * @throws NamespaceException 
     */
    public String toQName(String val) throws NamespaceException, RepositoryException
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
    public String fromQName(String val) throws NamespaceException, RepositoryException
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
