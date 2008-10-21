package org.priha.query.xpath;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.priha.core.JCRConstants;
import org.priha.core.PropertyImpl;
import org.w3c.dom.*;

public class DOMAttr extends DOMNode implements Node, Attr
{

    public DOMAttr( PropertyImpl ni ) throws RepositoryException
    {
        super( ni );
    }

    public String getName()
    {
        return m_session.fromQName( m_path.getLastComponent() );
    }

    public Element getOwnerElement()
    {
        try
        {
            return new DOMElement( getProperty().getParent() );
        }
        catch( RepositoryException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return null;
    }

    public TypeInfo getSchemaTypeInfo()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean getSpecified()
    {
        return true;
    }

    private PropertyImpl getProperty() throws PathNotFoundException, RepositoryException
    {
        return (PropertyImpl)m_session.getItem( m_path );
    }
    
    @Override
    public String getNodeValue()
    {
        return getValue();
    }
    
    public String getValue()
    {
        try
        {
            return getProperty().getString();
        }
        catch( Exception e )
        {
        }
        return null;
    }

    public boolean isId()
    {
        return m_path.getLastComponent().equals( JCRConstants.Q_JCR_UUID );
    }

    public void setValue( String value ) throws DOMException
    {
        throw new DOMException( DOMException.NO_MODIFICATION_ALLOWED_ERR, "Setting of attribute value not allowed");
    }

    public Node getNextSibling()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public short getNodeType()
    {
        return ATTRIBUTE_NODE;
    }

    public Node getPreviousSibling()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
