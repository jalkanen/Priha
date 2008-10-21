package org.priha.query.xpath;

import javax.jcr.RepositoryException;

import org.priha.core.NodeImpl;
import org.priha.core.SessionImpl;
import org.priha.util.NodeIteratorImpl;
import org.priha.util.Path;
import org.w3c.dom.*;

public class DOMElement extends DOMNode implements Element
{

    public DOMElement( NodeImpl ni ) throws RepositoryException
    {
        super( ni );
    }

    public DOMElement( SessionImpl session, Path path )
    {
        super( session, path );
    }

    public String getAttribute( String name )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getAttributeNS( String namespaceURI, String localName ) throws DOMException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Attr getAttributeNode( String name )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Attr getAttributeNodeNS( String namespaceURI, String localName ) throws DOMException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public NodeList getElementsByTagName( String name )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public NodeList getElementsByTagNameNS( String namespaceURI, String localName ) throws DOMException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public TypeInfo getSchemaTypeInfo()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getTagName()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean hasAttribute( String name )
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean hasAttributeNS( String namespaceURI, String localName ) throws DOMException
    {
        // TODO Auto-generated method stub
        return false;
    }

    public Node getPreviousSibling()
    {
        try
        {
            NodeImpl parent = (NodeImpl)m_session.getItem(m_path.getParentPath());
            
            NodeIteratorImpl ni = parent.getNodes();
            
            while( ni.hasNext() )
            {
                NodeImpl nd = ni.nextNode();
                
                if( nd.getInternalPath().equals(m_path) )
                {
                    return new DOMElement( ni.get( (int) (ni.getPosition()-1) ) );
                }
            }
        }
        catch( RepositoryException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return null;
    }

    //
    //  FIXME: This is relatively slow
    public Node getNextSibling()
    {
        try
        {
            NodeImpl parent = (NodeImpl)m_session.getItem(m_path.getParentPath());
            
            NodeIteratorImpl ni = parent.getNodes();
            
            while( ni.hasNext() )
            {
                NodeImpl nd = ni.nextNode();
                
                if( nd.getInternalPath().equals(m_path) && ni.hasNext() )
                {
                    return new DOMElement( ni.nextNode() );
                }
            }
        }
        catch( RepositoryException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return null;
    }
    
    public void removeAttribute( String name ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public void removeAttributeNS( String namespaceURI, String localName ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public Attr removeAttributeNode( Attr oldAttr ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public void setAttribute( String name, String value ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public void setAttributeNS( String namespaceURI, String qualifiedName, String value ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public Attr setAttributeNode( Attr newAttr ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public Attr setAttributeNodeNS( Attr newAttr ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public void setIdAttribute( String name, boolean isId ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public void setIdAttributeNS( String namespaceURI, String localName, boolean isId ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public void setIdAttributeNode( Attr idAttr, boolean isId ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "No modification allowed");
    }

    public short getNodeType()
    {
        return ELEMENT_NODE;
    }

    public String toString()
    {
        return "["+getNodeName()+" : "+getNamespaceURI()+" : "+m_path+"]";
    }
}
