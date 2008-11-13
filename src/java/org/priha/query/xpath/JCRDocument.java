package org.priha.query.xpath;

import java.util.ArrayList;

import javax.jcr.RepositoryException;

import org.priha.core.NodeImpl;
import org.priha.core.SessionImpl;
import org.priha.util.NodeIteratorImpl;
import org.w3c.dom.*;

public class JCRDocument implements Document
{
    private DOMElement m_root;
    private SessionImpl m_session;
    
    public JCRDocument( SessionImpl session, DOMElement root ) throws RepositoryException
    {
        m_session = session;
        m_root = root;
    }

    public Node adoptNode( Node source ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public Attr createAttribute( String name ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public Attr createAttributeNS( String namespaceURI, String qualifiedName ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public CDATASection createCDATASection( String data ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public Comment createComment( String data )
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public DocumentFragment createDocumentFragment()
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public Element createElement( String tagName ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public Element createElementNS( String namespaceURI, String qualifiedName ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public EntityReference createEntityReference( String name ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public ProcessingInstruction createProcessingInstruction( String target, String data ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public Text createTextNode( String data )
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public DocumentType getDoctype()
    {
        return null;
    }

    public Element getDocumentElement()
    {
        return m_root;
    }

    public String getDocumentURI()
    {
        return null;
    }

    public DOMConfiguration getDomConfig()
    {
        return null;
    }

    public Element getElementById( String elementId )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public NodeList getElementsByTagName( String tagname )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public NodeList getElementsByTagNameNS( String namespaceURI, String localName )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public DOMImplementation getImplementation()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getInputEncoding()
    {
        return "UTF-8";
    }

    public boolean getStrictErrorChecking()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public String getXmlEncoding()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean getXmlStandalone()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public String getXmlVersion()
    {
        return "1.0";
    }

    public Node importNode( Node importedNode, boolean deep ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public void normalizeDocument()
    {
    }

    public Node renameNode( Node n, String namespaceURI, String qualifiedName ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public void setDocumentURI( String documentURI )
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public void setStrictErrorChecking( boolean strictErrorChecking )
    {
    }

    public void setXmlStandalone( boolean xmlStandalone ) throws DOMException
    {
    }

    public void setXmlVersion( String xmlVersion ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public Node getNextSibling()
    {
        return null;
    }

    public short getNodeType()
    {
        return DOCUMENT_NODE;
    }

    public String getNodeName()
    {
        return "#document";
    }

    public Node getPreviousSibling()
    {
        return null;
    }

    public Node getParentNode()
    {
        return null;
    }

    public Node appendChild( Node newChild ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public Node cloneNode( boolean deep )
    {
        return null;
    }

    public short compareDocumentPosition( Node other ) throws DOMException
    {
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
        try
        {
            NodeImpl nd = (NodeImpl)m_session.getItem( m_root.m_path );
            
            ArrayList<NodeImpl> list = new ArrayList<NodeImpl>();
            list.add(nd);
            
            return new NodeListImpl(new NodeIteratorImpl(list));
        }
        catch( RepositoryException e )
        {
            return null;
        }
    }

    public Object getFeature( String feature, String version )
    {
        return null;
    }

    public Node getFirstChild()
    {
        return getChildNodes().item( 0 );
    }

    public Node getLastChild()
    {
        return getChildNodes().item( 0 );
    }

    public String getLocalName()
    {
        return getNodeName();
    }

    public String getNamespaceURI()
    {
        return null;
    }

    public String getNodeValue() throws DOMException
    {
        return null;
    }

    public Document getOwnerDocument()
    {
        return null;
    }

    public String getPrefix()
    {
        return null;
    }

    public String getTextContent() throws DOMException
    {
        return null;
    }

    public Object getUserData( String key )
    {
        return null;
    }

    public boolean hasAttributes()
    {
        return false;
    }

    public boolean hasChildNodes()
    {
        return true;
    }

    public Node insertBefore( Node newChild, Node refChild ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public boolean isDefaultNamespace( String namespaceURI )
    {
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
        // TODO Auto-generated method stub
        return false;
    }

    public String lookupNamespaceURI( String prefix )
    {
        try
        {
            return m_session.getNamespaceURI( prefix );
        }
        catch( RepositoryException e )
        {
            return null;
        }
    }

    public String lookupPrefix( String namespaceURI )
    {
        try
        {
            return m_session.getNamespacePrefix( namespaceURI );
        }
        catch( RepositoryException e )
        {
            return null;
        }
    }

    public void normalize()
    {
        // TODO Auto-generated method stub
        
    }

    public Node removeChild( Node oldChild ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public Node replaceChild( Node newChild, Node oldChild ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public void setNodeValue( String nodeValue ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public void setPrefix( String prefix ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public void setTextContent( String textContent ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public Object setUserData( String key, Object data, UserDataHandler handler )
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

}
