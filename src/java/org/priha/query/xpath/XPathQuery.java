package org.priha.query.xpath;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.jcr.*;
import javax.jcr.Node;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.priha.core.SessionImpl;
import org.priha.query.JCRFunctionResolver;
import org.priha.util.Path;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

public class XPathQuery implements Query
{
    private XPathFactory m_factory = XPathFactory.newInstance();
    private String       m_statement;
    private SessionImpl  m_session;
    
    public XPathQuery( SessionImpl session, String statement )
    {
        m_statement = statement;
        m_session   = session;
        
        m_factory.setXPathFunctionResolver( new JCRFunctionResolver() );
    }

    public QueryResult execute() throws RepositoryException
    {
        XPath xp = m_factory.newXPath();
        xp.setNamespaceContext( new SessionNamespaceContext() );
        
        NodeList ns;
        try
        {
            Object o;
        
            if( true )
            {
                DOMElement n = new DOMElement(m_session,Path.ROOT);
                JCRDocument doc = new JCRDocument(m_session,n);
                
                write(doc);
                
                o  = xp.evaluate( m_statement, 
                                  doc, 
                                  XPathConstants.NODESET );
                
            }
            else
            {
                ByteArrayOutputStream ba = new ByteArrayOutputStream();
            
                m_session.exportDocumentView( "/", ba, true, false );
            
                System.out.println( new String(ba.toByteArray()) );
                o = xp.evaluate(  m_statement, 
                                  new InputSource(new ByteArrayInputStream(ba.toByteArray())), 
                                  XPathConstants.NODESET );
            }
            ns = (NodeList) o;

            System.out.println("Statement="+m_statement);

            return new XPathQueryResult(m_session,ns);
        }
        catch( XPathExpressionException e )
        {
            e.printStackTrace();
            throw new RepositoryException( "Invalid XPath expression", e );
        }
        catch( Exception e )
        {
            e.printStackTrace();
            throw new RepositoryException( "Invalid IO", e );
        }
    }

    public String getLanguage()
    {
        return Query.XPATH;
    }

    public String getStatement()
    {
        return m_statement;
    }

    public String getStoredQueryPath() throws ItemNotFoundException, RepositoryException
    {
        throw new ItemNotFoundException("This query is not stored");
    }

    public Node storeAsNode( String arg0 )
                                          throws ItemExistsException,
                                              PathNotFoundException,
                                              VersionException,
                                              ConstraintViolationException,
                                              LockException,
                                              UnsupportedRepositoryOperationException,
                                              RepositoryException
    {
        throw new UnsupportedRepositoryOperationException("Query.storeAsNode()");
    }

    private final class SessionNamespaceContext implements NamespaceContext
    {
        public String getNamespaceURI( String prefix )
        {
            try
            {
                return m_session.getNamespaceURI( prefix );
            }
            catch( RepositoryException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }

        public String getPrefix( String namespaceURI )
        {
            try
            {
                return m_session.getNamespacePrefix( namespaceURI );
            }
            catch( RepositoryException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }

        public Iterator<String> getPrefixes( String namespaceURI )
        {
            ArrayList<String> list = new ArrayList<String>();
            list.add( getPrefix(namespaceURI) );
            return list.iterator();
        }
    }
    
    // Debug
    private void write(org.w3c.dom.Node node) {
        boolean fCanonical = true;
        PrintStream fOut = System.out;
        
        // is there anything to do?
        if (node == null) {
            return;
        }

        short type = node.getNodeType();
        switch (type) {
            case org.w3c.dom.Node.DOCUMENT_NODE: {
                Document document = (Document)node;
                if (!fCanonical) {
                    fOut.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    fOut.flush();
                    write(document.getDoctype());
                }
                write(document.getDocumentElement());
                break;
            }

            case org.w3c.dom.Node.DOCUMENT_TYPE_NODE: {
                DocumentType doctype = (DocumentType)node;
                fOut.print("<!DOCTYPE ");
                fOut.print(doctype.getName());
                String publicId = doctype.getPublicId();
                String systemId = doctype.getSystemId();
                if (publicId != null) {
                    fOut.print(" PUBLIC '");
                    fOut.print(publicId);
                    fOut.print("' '");
                    fOut.print(systemId);
                    fOut.print('\'');
                }
                else {
                    fOut.print(" SYSTEM '");
                    fOut.print(systemId);
                    fOut.print('\'');
                }
                String internalSubset = doctype.getInternalSubset();
                if (internalSubset != null) {
                    fOut.println(" [");
                    fOut.print(internalSubset);
                    fOut.print(']');
                }
                fOut.println('>');
                break;
            }

            case org.w3c.dom.Node.ELEMENT_NODE: {
                fOut.print('<');
                fOut.print(node.getNodeName());
                NamedNodeMap attrs = node.getAttributes();
                for (int i = 0; i < attrs.getLength(); i++) {
                    Attr attr = (Attr) attrs.item( i );
                    fOut.print(' ');
                    fOut.print(attr.getNodeName());
                    fOut.print("=\"");
                    fOut.print(attr.getNodeValue());
                    fOut.print('"');
                }
                fOut.print(">\n");
                fOut.flush();

                org.w3c.dom.Node child = node.getFirstChild();
                while (child != null) {
                    write(child);
                    child = child.getNextSibling();
                }
                break;
            }

            case org.w3c.dom.Node.ENTITY_REFERENCE_NODE: {
                if (fCanonical) {
                    org.w3c.dom.Node child = node.getFirstChild();
                    while (child != null) {
                        write(child);
                        child = child.getNextSibling();
                    }
                }
                else {
                    fOut.print('&');
                    fOut.print(node.getNodeName());
                    fOut.print(';');
                    fOut.flush();
                }
                break;
            }
/*
            case Node.TEXT_NODE: {
                //normalizeAndPrint(node.getNodeValue());
                fOut.flush();
                break;
            }

            case Node.PROCESSING_INSTRUCTION_NODE: {
                fOut.print("<?");
                fOut.print(node.getNodeName());
                String data = node.getNodeValue();
                if (data != null && data.length() > 0) {
                    fOut.print(' ');
                    fOut.print(data);
                }
                fOut.println("?>");
                fOut.flush();
                break;
            }
            */
        }

        if (type == org.w3c.dom.Node.ELEMENT_NODE) {
            fOut.print("</");
            fOut.print(node.getNodeName());
            fOut.print('>');
            fOut.flush();
        }
    }

}
