package org.priha.core.namespace;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.*;

import org.priha.RepositoryManager;
import org.priha.core.JCRConstants;
import org.priha.path.PathFactory;
import org.priha.util.QName;
import org.xml.sax.helpers.NamespaceSupport;

public class NamespaceRegistryImpl implements NamespaceRegistry, NamespaceMapper
{
    /** Maps prefixes to URIs.  Prefixes are always unique, therefore they are the keys */
    protected HashMap<String,String> m_nsmap = new HashMap<String,String>();
    
    public NamespaceRegistryImpl()
    {
        super();
        m_nsmap.put("jcr",   JCRConstants.NS_JCP);
        m_nsmap.put("nt",    JCRConstants.NS_JCP_NT);
        m_nsmap.put("mix",   JCRConstants.NS_JCP_MIX);
        m_nsmap.put("xml",   NamespaceSupport.XMLNS);
        m_nsmap.put("xmlns", NamespaceSupport.NSDECL);
        m_nsmap.put("sv",    JCRConstants.NS_JCP_SV);
        m_nsmap.put("priha", RepositoryManager.NS_PRIHA);
        m_nsmap.put("test",  "http://www.priha.org/test/1.0");
        m_nsmap.put("",      "");
    }

    public String getPrefix(String uri) throws NamespaceException
    {
        for( Map.Entry<String,String> entry : m_nsmap.entrySet() )
        {
            if( entry.getValue().equals(uri) ) return entry.getKey();
        }      
        
        throw new NamespaceException("Prefix for URI does not exist: "+uri);
    }

    public String[] getPrefixes() throws RepositoryException
    {
        String[] uris = getURIs();

        Set<String> ls = new TreeSet<String>();
        
        for( String uri : uris )
        {
            ls.add( getPrefix( uri ) );
        }

        return ls.toArray(new String[0]);
    }
    
    public String getURI(String prefix) throws NamespaceException
    {
        String u = m_nsmap.get(prefix);
        
        if( u == null ) 
        {
            throw new NamespaceException( "No such prefix: "+prefix );
        }
        
        return u;
    }

    public String[] getURIs() throws RepositoryException
    {
        Set<String> ls = new TreeSet<String>();
        
        ls.addAll( m_nsmap.values() );
        
        return ls.toArray(new String[0]);
    }

    private boolean isBuiltinNamespace(String prefix)
    {
        return prefix.equals( "jcr" ) || prefix.equals( "nt" ) || prefix.equals("mix") || prefix.equals( "" )
               || prefix.startsWith("xml") || prefix.equals("priha") || prefix.equals("sv");
        
    }
    
    public void registerNamespace(String prefix, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException
    {
        if( isBuiltinNamespace(prefix) )
        {
            throw new NamespaceException("Prefix "+prefix+" may not be registered (7.2)");
        }
        
        PathFactory.reset();
        m_nsmap.put( prefix, uri );
    }

    public void unregisterNamespace(String prefix) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException
    {
        if( isBuiltinNamespace(prefix) )
        {
            throw new NamespaceException("Prefix "+prefix+" may not be unregistered (7.2)");            
        }
        
        if( m_nsmap.get( prefix ) == null )
        {
            throw new NamespaceException("Prefix "+prefix+" is not currently registered (7.2)");
        }
        
        //
        //  This is allowed for implementation-specific reasons.
        //
        throw new NamespaceException("Priha does currently not allow unregistering of namespaces (because we're lazy and don't keep count of what namespaces are actually used.)");
        //m_nsmap.remove( prefix );
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
        if( val.indexOf( '{' ) != -1 ) throw new RepositoryException("Already in QName format: "+val);
        
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
     * @throws NamespaceException If the mapping is unknown.
     */
    public String fromQName(QName val) throws NamespaceException
    {
        String uri = val.getNamespaceURI();
            
        if( uri.length() == 0 ) return val.getLocalPart();
            
        String prefix = getPrefix( val.getNamespaceURI() );
            
        return prefix+":"+val.getLocalPart();
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
}
