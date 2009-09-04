package org.priha.util;

import java.io.Serializable;

import javax.xml.XMLConstants;

/**
 *  Reimplements {@link javax.xml.namespace.QName}, because it prevents subclassing
 *  by declaring equals() as final.
 *  <p>
 *  QNames are immutable, i.e. once they are created, they cannot be changed.
 */
public class QName implements Serializable
{
    private String m_uri;
    private String m_localName;
    private String m_prefix;
    
    private transient String m_cachedString;
    
    private static final long serialVersionUID = 1L;
    
    public QName( String uri, String name, String prefix )
    {
        m_uri = (uri != null) ? uri.intern() : XMLConstants.NULL_NS_URI;
        m_localName = name.intern();
        m_prefix = prefix;
    }

    public QName( String localName )
    {
        this( null, localName, XMLConstants.DEFAULT_NS_PREFIX );
    }

    public QName( String namespaceURI, String localpart )
    {
        this( namespaceURI, localpart, XMLConstants.DEFAULT_NS_PREFIX );
    }

    public String getNamespaceURI()
    {
        return m_uri;
    }

    public String getLocalPart()
    {
        return m_localName;
    }

    public String getPrefix()
    {
        return m_prefix;
    }

    public static QName valueOf( String val )
    {
        if( val == null ) throw new IllegalArgumentException("Null value given to QName.valueOf()");
        
        if( val.length() == 0 ) return new QName(val);
        
        if( val.charAt(0) == '{' )
        {
            int end = val.indexOf( '}' );
            if( end == -1 ) throw new IllegalArgumentException("Missing }: "+val);
            
            return new QName( val.substring( 1,end ), 
                              val.substring( end+1 ), 
                              XMLConstants.DEFAULT_NS_PREFIX );
        }
        
        return new QName(val);
    }
    
    public boolean equals( Object o )
    {
        if( o == this ) return true;
        
        if( o != null && o instanceof QName )
        {
            QName q = (QName) o;
            return q.m_localName.equals(m_localName) && q.m_uri.equals( m_uri );
        }
        
        return false;
    }
    
    public int hashCode()
    {
        return getLocalPart().hashCode() ^ getNamespaceURI().hashCode();
    }
    
    public String toString()
    {
        if( m_cachedString == null )
        {
            if( m_uri.equals( XMLConstants.NULL_NS_URI ) ) 
                m_cachedString = m_localName;
            else
                m_cachedString = "{"+m_uri+"}"+m_localName;
        }
        return m_cachedString;
    }
}
