package org.priha.query.xpath;

import javax.jcr.NamespaceException;
import javax.xml.namespace.QName;

import org.priha.core.namespace.NamespaceMapper;
import org.w3c.dom.*;

public class SimpleAttr implements Node, Attr
{
    private QName  m_key;
    private String m_value;
    private NamespaceMapper m_ns;
    
    public SimpleAttr( NamespaceMapper ns, QName key, String value )
    {
        m_key = key;
        m_value = value;
        m_ns = ns;
    }

    public Node appendChild( Node newChild ) throws DOMException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Node cloneNode( boolean deep )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public short compareDocumentPosition( Node other ) throws DOMException
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public NamedNodeMap getAttributes()
    {
        return null;
    }

    public String getBaseURI()
    {
        return null;
    }

    public NodeList getChildNodes()
    {
        return null;
    }

    public Object getFeature( String feature, String version )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Node getFirstChild()
    {
        return null;
    }

    public Node getLastChild()
    {
        return null;
    }

    public String getLocalName()
    {
        return null;
        //return m_key.getLocalPart();
    }

    public String getNamespaceURI()
    {
        return null;
        //return m_key.getNamespaceURI();
    }

    public Node getNextSibling()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getNodeName()
    {
        try
        {
            return m_ns.fromQName( m_key );
        }
        catch( NamespaceException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public short getNodeType()
    {
        return ATTRIBUTE_NODE;
    }

    public String getNodeValue() throws DOMException
    {
        return m_value;
    }

    public Document getOwnerDocument()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Node getParentNode()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getPrefix()
    {
        return null;
    }

    public Node getPreviousSibling()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getTextContent() throws DOMException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getUserData( String key )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean hasAttributes()
    {
        return false;
    }

    public boolean hasChildNodes()
    {
        return false;
    }

    public Node insertBefore( Node newChild, Node refChild ) throws DOMException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isDefaultNamespace( String namespaceURI )
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isEqualNode( Node arg )
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isSameNode( Node other )
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isSupported( String feature, String version )
    {
        return false;
    }

    public String lookupNamespaceURI( String prefix )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String lookupPrefix( String namespaceURI )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void normalize()
    {
    }

    public Node removeChild( Node oldChild ) throws DOMException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Node replaceChild( Node newChild, Node oldChild ) throws DOMException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void setNodeValue( String nodeValue ) throws DOMException
    {
        // TODO Auto-generated method stub

    }

    public void setPrefix( String prefix ) throws DOMException
    {
        // TODO Auto-generated method stub

    }

    public void setTextContent( String textContent ) throws DOMException
    {
        // TODO Auto-generated method stub

    }

    public Object setUserData( String key, Object data, UserDataHandler handler )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getName()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Element getOwnerElement()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public TypeInfo getSchemaTypeInfo()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean getSpecified()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public String getValue()
    {
        return getNodeValue();
    }

    public boolean isId()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public void setValue( String value ) throws DOMException
    {
        // TODO Auto-generated method stub

    }

}
