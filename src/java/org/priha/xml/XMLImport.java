package org.priha.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.jcr.*;
import javax.jcr.nodetype.PropertyDefinition;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.priha.core.JCRConstants;
import org.priha.core.SessionImpl;
import org.priha.core.values.ValueFactoryImpl;
import org.priha.nodetype.GenericNodeType;
import org.priha.util.Base64;
import org.priha.util.Path;
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
    private SessionImpl m_session;
    private boolean m_immediateCommit;
    private Path    m_currentPath;
    private int     m_uuidBehavior;
    private ParserStyle m_style = ParserStyle.UNKNOWN;
    
    private Node    m_currentNode;
    private NodeStore m_currentStore;
    private PropertyStore m_currentProperty;
    private boolean m_readingValue = false;
    
    private static Logger log = Logger.getLogger(XMLImport.class.getName());
    
    public XMLImport( SessionImpl session, boolean immediateCommit, Path startPath, int uuidBehavior ) throws RepositoryException
    {
        if( !session.getRootNode().hasNode(startPath.toString()) )
            throw new PathNotFoundException("The parent path does not exist, so cannot import to it!");
        
        m_session = session;
        m_immediateCommit = immediateCommit;
        m_currentPath = startPath;
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
    
    /**
     *  This method takes the item which has just been read from the XML
     *  event stream, and creates the corresponding Nodes and Properties in the repository.
     *  
     *  @param ns The NodeStore
     *  @throws ValueFormatException
     *  @throws IllegalStateException
     *  @throws RepositoryException
     */
    private void deserializeStore( NodeStore ns ) throws ValueFormatException, IllegalStateException, RepositoryException
    {
        String primaryType = getProp(ns,JCRConstants.JCR_PRIMARY_TYPE).m_values.get(0).getString();
        
        String path = m_currentPath.toString();
                
        log.finest("Deserializing node at "+path);
        if( m_session.itemExists(path) )
        {
            throw new ItemExistsException("There already exists a node at "+path);
        }
        
        Node nd = m_currentNode.addNode( path, primaryType );
        
        m_currentNode = nd;
        
        for( PropertyStore ps : ns.m_properties )
        {
            //
            // Known property names which need to be handled separately.
            //
            if( ps.m_propertyName.equals(JCRConstants.JCR_PRIMARY_TYPE) ) continue;
            
            if( ps.m_propertyName.equals(JCRConstants.JCR_MIXIN_TYPES) )
            {
                for( Value v : ps.m_values )
                {
                    nd.addMixin( v.getString() );
                }
                continue;
            }
            
            //
            //  Start the real unmarshalling
            //
            log.finest("   Property: "+ps.m_propertyName);
                        
            GenericNodeType parentType = (GenericNodeType)nd.getPrimaryNodeType();
            
            //
            //  Now we try to figure out whether this should be a multi or a single property.
            //  The problem is when it is a multi property, but it was written as a single value.
            //
            PropertyDefinition pdmulti = parentType.findPropertyDefinition( ps.m_propertyName, true );
            //PropertyDefinition pdsingle = parentType.findPropertyDefinition( ps.m_propertyName, false );
            
            boolean ismultiproperty = (pdmulti != null && !pdmulti.getName().equals("*"));
            
            if( ps.m_values.size() == 1 && !ismultiproperty )
            {
                nd.setProperty( ps.m_propertyName, ps.m_values.get(0) );
            }
            else
            {
                nd.setProperty( ps.m_propertyName, ps.m_values.toArray( new Value[ps.m_values.size()] ) );
            }
        }
        
        if( m_immediateCommit ) m_session.save();
    }
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException
    {
        String name = qName != null ? qName : localName;
        
        if( name.equals("sv:node") )
        {
            checkSystem();
            
            String nodeName = attrs.getValue("sv:name");
            
            // Hitting a new Node means that all the properties have been read,
            // so we can deserialize the last store.
            
            if( m_currentStore != null )
            {
                try
                {
                    log.finest("New -> ");
                    deserializeStore( m_currentStore );
                }
                catch( Exception e )
                {
                    throw new SAXException("Deserialization failed",e);
                }
            }
            
            m_currentStore = new NodeStore();
            
            if( nodeName.equals("jcr:root") )
            {
                nodeName = "/";
            }
            
            m_currentStore.m_nodeName = nodeName;
            m_currentPath = m_currentPath.resolve(m_session.toQName( nodeName ) );
            
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
                log.finest("End -> ");
                if( m_currentStore != null ) deserializeStore( m_currentStore );

                m_currentNode = m_currentNode.getParent();
                m_currentPath = m_currentPath.getParentPath();
                m_currentStore = null;
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

    /**
     *  Parse the character data coming in; that is, create the Values which are put
     *  into the properties.
     *  <p>
     *  Unfortunately, this method handles Binary properties by reading them completely
     *  into memory as a String, and turning that into a byte array, essentially taking
     *  3x the amount of memory that what it should.  Better solutions are welcomed...
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException
    {
        if( m_readingValue )
        {
            try
            {
                Value v;
                String valueString = new String(ch,start,length);
                if( m_currentProperty.m_propertyType == PropertyType.BINARY )
                {
                    InputStream in = new Base64.InputStream( new ByteArrayInputStream(valueString.getBytes("UTF-8")) );
                    v = ValueFactoryImpl.getInstance().createValue( in );
                }
                else
                {
                    v = ValueFactoryImpl.getInstance().createValue( valueString, 
                                                                    m_currentProperty.m_propertyType );
                }
                m_currentProperty.m_values.add( v );
            }
            catch (ValueFormatException e)
            {
                throw new SAXException("Value creation failed",e);
            }
            catch (UnsupportedEncodingException e)
            {
                throw new SAXException("You can't be serious that your platform does not support UTF-8!?!");
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
