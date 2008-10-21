package org.priha.query.xpath;

import java.util.ArrayList;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.xml.namespace.QName;

import org.priha.core.ItemImpl;
import org.priha.core.JCRConstants;
import org.priha.core.NodeImpl;
import org.priha.core.SessionImpl;
import org.priha.util.InvalidPathException;
import org.priha.util.NodeIteratorImpl;
import org.priha.util.Path;
import org.priha.util.PropertyIteratorImpl;
import org.w3c.dom.*;
import org.xml.sax.helpers.NamespaceSupport;

public abstract class DOMNode implements Node
{
    protected SessionImpl m_session;
    protected Path        m_path;
    
    public DOMNode( ItemImpl ni ) throws RepositoryException
    {
        m_session = ni.getSession();
        m_path    = ni.getInternalPath();
    }
    
    public DOMNode( SessionImpl session, Path path )
    {
        m_session = session;
        m_path    = path;
    }
    
    public Node appendChild( Node newChild ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, 
                               "Changing of the DOM is not allowed.");
    }

    public Node cloneNode( boolean deep )
    {
        return null;
    }

    public short compareDocumentPosition( Node other ) throws DOMException
    {
        DOMNode nd = (DOMNode)other;
        
        return (short) m_path.compareTo( nd.m_path );
    }

    public NamedNodeMap getAttributes()
    {
        try
        {
            NodeImpl ni = (NodeImpl)m_session.getItem( m_path );

            PropertyIteratorImpl iter = ni.getProperties();

            NamedNodeMap nm = new NamedNodeMapImpl(iter, m_path.isRoot());
            
            return nm;
        }
        catch( RepositoryException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

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
            return new NodeListImpl(getJCRNode().getNodes());
        }
        catch( RepositoryException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public Object getFeature( String feature, String version )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Node getFirstChild()
    {
        try
        {
            if( hasChildNodes() )
            {
                NodeImpl ni = getJCRNode().getNodes().get( 0 );
            
                return new DOMElement(ni);
            }
            return null;
        }
        catch( RepositoryException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public Node getLastChild()
    {
        try
        {
            NodeIteratorImpl nii = getJCRNode().getNodes();
            NodeImpl ni = nii.get( (int) (nii.getSize()-1) );
            
            return new DOMElement(ni);
        }
        catch( RepositoryException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public String getLocalName()
    {
        if( m_path.isRoot() ) return "root";
        
        String lName = m_path.getLastComponent().getLocalPart();
        
        return lName;
    }

    public String getNamespaceURI()
    {
        if( m_path.isRoot() ) return JCRConstants.NS_JCP;
        
        return m_path.getLastComponent().getNamespaceURI();
    }



    public String getNodeName()
    {
        if( m_path.isRoot() ) return "jcr:root";
        
        String nName = m_session.fromQName( m_path.getLastComponent() );
        
        return nName;
    }

    public String getNodeValue() throws DOMException
    {
        return null;
    }

    public Document getOwnerDocument()
    {
        return null;
    }

    public Node getParentNode()
    {
        try
        {
            return new DOMElement( m_session, m_path.getParentPath() );
        }
        catch( InvalidPathException e )
        {
            return null;
        }
    }

    public String getPrefix()
    {
        try
        {
            if( m_path.isRoot() ) return "jcr";
            
            return m_session.getNamespacePrefix( getNamespaceURI() );
        }
        catch( RepositoryException e )
        {
            return null;
        }
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

    protected NodeImpl getJCRNode() throws PathNotFoundException, RepositoryException
    {
        return (NodeImpl)m_session.getItem( m_path );
    }
    
    public boolean hasAttributes()
    {
        try
        {
            return getJCRNode().hasProperties();
        }
        catch( RepositoryException e )
        {
            return false;
        }
    }

    public boolean hasChildNodes()
    {
        try
        {
            return getJCRNode().hasNodes();
        }
        catch( RepositoryException e )
        {
            return false;
        }
    }

    public Node insertBefore( Node newChild, Node refChild ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, 
                               "Changing of the DOM is not allowed.");
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
        if( feature.equals( "Core" ) ) return true;
        
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
        // TODO Auto-generated method stub
        
    }

    public Node removeChild( Node oldChild ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, 
                               "Changing of the DOM is not allowed.");
    }

    public Node replaceChild( Node newChild, Node oldChild ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, 
                               "Changing of the DOM is not allowed.");
    }

    public void setNodeValue( String nodeValue ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, 
                               "Changing of the DOM is not allowed.");
    }

    public void setPrefix( String prefix ) throws DOMException
    {
        // TODO Auto-generated method stub
        
    }

    public void setTextContent( String textContent ) throws DOMException
    {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, 
                               "Changing of the DOM is not allowed.");
    }

    public Object setUserData( String key, Object data, UserDataHandler handler )
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public class NamedNodeMapImpl implements NamedNodeMap
    {
        private PropertyIteratorImpl m_iter;
        private boolean              m_root;
        private String[]             m_prefixes;
        
        public NamedNodeMapImpl( PropertyIteratorImpl iter, boolean isRoot ) throws RepositoryException
        {
            m_iter = iter;
            m_root = isRoot;
            
            ArrayList<String> prefixes = new ArrayList<String>();
            
            for( String prefix : m_session.getNamespacePrefixes() )
            {
                //
                //  XML and default prefixes are not exported.
                //
                if( !prefix.startsWith("xml") && !prefix.equals( "" ) )
                    prefixes.add( prefix );
            }
            
            m_prefixes = prefixes.toArray(new String[0]);
        }

        public int getLength()
        {
            return (int) m_iter.getSize() + (m_root ? m_prefixes.length : 0);
        }

        public Node getNamedItem( String name )
        {
            // TODO Auto-generated method stub
            return null;
        }

        public Node getNamedItemNS( String namespaceURI, String localName ) throws DOMException
        {
            // TODO Auto-generated method stub
            return null;
        }

        public Node item( int index )
        {
            try
            {
                if( m_root && index < m_prefixes.length )
                {
                    String p = m_prefixes[index];
                    
                    return new SimpleAttr(m_session,
                                          QName.valueOf( "{"+NamespaceSupport.NSDECL+"}"+p ),
                                          m_session.getNamespaceURI( p ));
                }
                return new DOMAttr( m_iter.get(index-(m_root?m_prefixes.length:0)) );
            }
            catch( RepositoryException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }

        public Node removeNamedItem( String name ) throws DOMException
        {
            throw new DOMException( DOMException.NO_MODIFICATION_ALLOWED_ERR,
                                    "No modification" );
        }

        public Node removeNamedItemNS( String namespaceURI, String localName ) throws DOMException
        {
            throw new DOMException( DOMException.NO_MODIFICATION_ALLOWED_ERR,
                                    "No modification" );
        }

        public Node setNamedItem( Node arg ) throws DOMException
        {
            throw new DOMException( DOMException.NO_MODIFICATION_ALLOWED_ERR,
                                    "No modification" );
        }

        public Node setNamedItemNS( Node arg ) throws DOMException
        {
            throw new DOMException( DOMException.NO_MODIFICATION_ALLOWED_ERR,
                                    "No modification" );
        }
        
    }
}
