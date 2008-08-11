package org.jspwiki.priha.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.jspwiki.priha.core.values.ValueFactoryImpl;
import org.jspwiki.priha.util.InvalidPathException;
import org.jspwiki.priha.util.Path;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *  Does the chunky work of importing stuff into the repository.  Always requires
 *  a Session within which the stuff is sent to the repo; however, you can
 *  either ask the importer to commit stuff on its own, or just skip it.
 */
public class XMLImport extends DefaultHandler
{
    private Session m_session;
    private boolean m_immediateCommit;
    private Path    m_startPath;
    private int     m_uuidBehavior;
    private ParserStyle m_style = ParserStyle.UNKNOWN;
    
    private Node    m_currentNode;
    private NodeStore m_currentStore;
    private PropertyStore m_currentProperty;
    private boolean m_readingValue = false;
    
    public XMLImport( Session session, boolean immediateCommit, Path startPath, int uuidBehavior ) throws RepositoryException
    {
        if( !session.getRootNode().hasNode(startPath.toString()) )
            throw new PathNotFoundException("The parent path does not exist, so cannot import to it!");
        
        m_session = session;
        m_immediateCommit = immediateCommit;
        m_startPath = startPath;
        m_uuidBehavior = uuidBehavior;
        m_currentNode = session.getRootNode().getNode( startPath.toString() );
    }
    
    public void doImport( InputStream xmlDoc ) throws ParserConfigurationException, SAXException, IOException
    {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        
        SAXParser parser = spf.newSAXParser();
        
        parser.parse( xmlDoc, this );
    }
    
    private void checkSystem() throws SAXException
    {
        if( m_style == ParserStyle.DOCUMENT ) throw new SAXException("System View elements found in a Document View document!");
        m_style = ParserStyle.SYSTEM;        
    }
    
    private PropertyStore getProp(NodeStore ns, String name)
    {
        for( PropertyStore ps : ns.m_properties )
        {
            if( ps.m_propertyName.equals(name) ) return ps;
        }
        return null;
    }
    
    private void deserializeStore( NodeStore ns ) throws ValueFormatException, IllegalStateException, RepositoryException
    {
        String primaryType = getProp(ns,"jcr:primaryType").m_values.get(0).getString();
        
        String path = m_currentNode.getPath() + "/" + ns.m_nodeName;
        
        if( m_session.itemExists(path) )
        {
            throw new ItemExistsException("There already exists a node at "+path);
        }
        
        // System.out.println("Importing "+path);
        
        Node nd = m_currentNode.addNode( path, primaryType );
        
        m_currentNode = nd;
    }
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException
    {
        String name = qName != null ? qName : localName;
        
        if( name.equals("sv:node") )
        {
            checkSystem();
            
            String nodeName = attrs.getValue("sv:name");
            
            m_currentStore = new NodeStore();
            
            if( nodeName.equals("jcr:root") )
            {
                nodeName = "/";
            }
            
            m_currentStore.m_nodeName = nodeName;
            
            return;
        }
        else if( name.equals("sv:property") )
        {
            checkSystem();
            
            String propName = attrs.getValue("sv:name");
            String propType = attrs.getValue("sv:type");
            
            m_currentProperty = new PropertyStore();
            m_currentProperty.m_propertyName = propName;
            m_currentProperty.m_propertyType = PropertyType.valueFromName( propType );
            
            return;
        }
        else if( name.equals("sv:value") )
        {
            checkSystem();
            
            m_readingValue = true;
            
            return;
        }
        
        m_style = ParserStyle.DOCUMENT;

        // System.out.println("name="+name);
        
        throw new SAXException("Document view import not yet supported");
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        String name = qName != null ? qName : localName;
        
        if( name.equals("sv:node") )
        {
            try
            {
                deserializeStore( m_currentStore );

                m_currentNode = m_currentNode.getParent();
            }
            catch (RepositoryException e)
            {
                throw new SAXException("Could not deserialize node",e);
            }
        }
        else if( name.equals("sv:value") )
        {
            m_readingValue = false;
        }
        else if( name.equals("sv:property") )
        {
            m_currentStore.m_properties.add( m_currentProperty );
            m_currentProperty = null;
        }
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException
    {
        // System.out.println(prefix + " = " + uri );
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException
    {
        if( m_readingValue )
        {
            try
            {
                Value v = ValueFactoryImpl.getInstance().createValue( new String(ch, start, length), 
                                                                      m_currentProperty.m_propertyType );
                m_currentProperty.m_values.add( v );
            }
            catch (ValueFormatException e)
            {
                throw new SAXException("Value creation failed",e);
            }
        }
    }

    /** Just stores the state of the parser. */
    private static enum ParserStyle
    {
        DOCUMENT,
        SYSTEM,
        UNKNOWN
    }
    
    private static class NodeStore
    {
        public String        m_nodeName;
        public List<PropertyStore> m_properties = new ArrayList<PropertyStore>();
    }
    
    private static class PropertyStore
    {
        public String       m_propertyName;
        public int          m_propertyType;
        public List<Value>  m_values = new ArrayList<Value>();
    }
}
