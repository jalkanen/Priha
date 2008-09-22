package org.priha.core.namespace;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.*;
import javax.xml.namespace.QName;

import org.priha.util.Path;

public class NamespaceRegistryImpl implements NamespaceRegistry, NamespaceAware
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
     *  @param val
     *  @return
     *  @throws RepositoryException 
     *  @throws NamespaceException If the mapping cannot be accomplished. 
     */
    public QName toQName(String val) throws NamespaceException, RepositoryException
    {
        int idx = val.indexOf(':');
        if( idx != -1 )
        {
            String prefix = val.substring(0,idx);
            String name   = val.substring(idx+1);
            
            String uri = getURI(prefix);
    
            return new QName( uri, name, prefix );
        }
        
        return new QName( val );
    }

    /**
     * Turns a string of the form "{uri}name" to "prefix:name".
     * 
     * @param val
     * @return
     * @throws NamespaceException
     * @throws RepositoryException
     */
    public String fromQName(QName val) throws NamespaceException, RepositoryException
    {
        try
        {
            String uri = val.getNamespaceURI();
            
            if( uri.length() == 0 ) return val.getLocalPart();
            
            String prefix = getPrefix( val.getNamespaceURI() );
            
            return prefix+":"+val.getLocalPart();
        }
        catch( NamespaceException e )
        {
            return val.getLocalPart();
        }
    }

    /**
     *  Return true, if this NamespaceRegistryImpl has any mappings.
     *  
     *  @return True, if there are any mappings to care about.
     */
    public boolean hasMappings()
    {
        return m_nsmap.size() != 0;
    }

    public Path fromQPath(Path path)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Path toQPath(Path path)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
