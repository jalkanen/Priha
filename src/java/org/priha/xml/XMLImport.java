package org.priha.xml;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jcr.*;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.priha.core.JCRConstants;
import org.priha.core.NodeImpl;
import org.priha.core.SessionImpl;
import org.priha.core.namespace.NamespaceRegistryImpl;
import org.priha.nodetype.QNodeType;
import org.priha.nodetype.QNodeTypeManager;
import org.priha.nodetype.QPropertyDefinition;
import org.priha.util.Base64;
import org.priha.util.FileUtil;
import org.priha.util.InvalidPathException;
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
    
    private NodeImpl  m_currentNode;
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
        spf.setNamespaceAware( true );
        spf.setValidating( false );
        spf.setFeature( "http://xml.org/sax/features/namespace-prefixes", true );
        
        SAXParser parser = spf.newSAXParser();

        boolean s = m_session.setSuper( true );
        
        try
        {
            parser.parse( xmlDoc, this );
        }
        finally
        {
            m_session.setSuper( s );
        }
    }
    
    private void checkSystem() throws SAXException
    {
        if( m_style == ParserStyle.DOCUMENT ) throw new SAXException("System View elements found in a Document View document!");
        m_style = ParserStyle.SYSTEM;        
    }
    
    private void checkDocument() throws SAXException
    {
        if( m_style == ParserStyle.SYSTEM ) throw new SAXException("Document view elements found in a System level document.");
        m_style = ParserStyle.DOCUMENT;
    }
    
    private PropertyStore getProp(NodeStore ns, QName name)
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
        PropertyStore psp = getProp(ns,JCRConstants.Q_JCR_PRIMARYTYPE);
        String primaryType;
        String uuid       = null;
        NodeImpl uuidNode = null;
        
        if( psp != null && psp.m_values.size() > 0 )
            primaryType = psp.m_values.get(0).getString();
        else
            primaryType = "nt:unstructured"; // FIXME: Stupid guess.
        
        String path = m_currentPath.toString();
                
        log.finest("Deserializing node at "+path);
        if( m_session.itemExists(path) )
        {
            throw new ItemExistsException("There already exists a node at "+path);
        }
        
        //
        //  Find the UUID and, if it exists, a possible previous Node which
        //  has the same UUID.
        //
        PropertyStore uuidPS = ns.findProperty( JCRConstants.Q_JCR_UUID );
        
        if( uuidPS != null ) 
        {
            uuid = uuidPS.m_values.get(0).getString();
            try
            {
                uuidNode = m_session.getNodeByUUID( uuid );
            }
            catch( ItemNotFoundException e ) {}
        }
        
        NodeImpl nd;

        //
        //  Do we need to replace an existing Node with the same UUID?
        //
        if( m_uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING 
            && uuid != null 
            && uuidNode != null )
        {
            NodeImpl parent = uuidNode.getParent();
            uuidNode.remove();

            m_currentNode = parent;
        }
        
        nd = m_currentNode.addNode( m_currentStore.m_nodeName, primaryType );

        //
        //  If there's an UUID, we have to figure out what to do with it.
        //
        if( uuid != null )
        {
            //nd.addMixin( "mix:referenceable" );

            switch( m_uuidBehavior )
            {
                case ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW:
                    if( uuidNode != null )
                        throw new ItemExistsException("UUID "+uuid+" already exists in repository, and IMPORT_UUID_COLLISION_THROW was defined.");
                    
                    nd.setProperty( "jcr:uuid", uuid );
                    break;
                    
                case ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW:
                    // Will be created automatically on save()
                    break;
                    
                case ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING:
                    if( uuidNode != null && uuidNode.getInternalPath().isParentOf( m_currentPath ) )
                    {
                        throw new ConstraintViolationException("Importing this node ("+nd+") would result in one of its parents being removed - "+
                                                               "which, in general, is kinda like traveling back in time and shooting your "+
                                                               "grandparents.  Most time traveling guidelines strongly recommend against this, as "+
                                                               "it will lead to paradoxes.  This is also why it is not possible within JCR.  Not that "+
                                                               "this is exactly time travel technology, far from it, but it just keeps stuff neat "+
                                                               "and tidy, you know.");
                    }
                    uuidNode.remove();
                    nd.setProperty( "jcr:uuid", uuid );
                    break;
                    
                case ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING:
                    nd.setProperty( "jcr:uuid", uuid );
                    break;
            }
        }
        
        m_currentNode = nd;
        
        for( PropertyStore ps : ns.m_properties )
        {
            //
            // Known property names which need to be handled separately.
            //
            if( ps.m_propertyName.equals(JCRConstants.Q_JCR_PRIMARYTYPE) ) continue;
            
            if( ps.m_propertyName.equals(JCRConstants.Q_JCR_MIXINTYPES) )
            {
                for( Value v : ps.m_values )
                {
                    nd.addMixin( v.getString() );
                }
                continue;
            }
            
            // Already handled
            if( ps.m_propertyName.equals(JCRConstants.Q_JCR_UUID) ) continue;
            
            
            //
            //  Start the real unmarshalling
            //
            log.finest("   Property: "+ps.m_propertyName);
                                    
            //
            //  Now we try to figure out whether this should be a multi or a single property.
            //  The problem is when it is a multi property, but it was written as a single value.
            //
            //  If there is a multidefinition, but no single definition, we assume this
            //  to be a multiproperty.  If both exist, we'll assume a single property, unless
            //  the single property is matched to be a wildcard.
            //
            QPropertyDefinition pdmulti = nd.findPropertyDefinition( ps.m_propertyName, true );
            QPropertyDefinition pdsingle = nd.findPropertyDefinition( ps.m_propertyName, false );
            
            boolean ismultiproperty = (pdmulti != null && 
                (pdsingle == null || (pdsingle != null && pdsingle.isWildCard() && !pdmulti.isWildCard()) ) );
            
            if( ps.m_values.size() == 1 && !ismultiproperty )
            {
                nd.setProperty( m_session.fromQName( ps.m_propertyName ), ps.m_values.get(0) );
                
                //System.out.println("  "+ps.m_propertyName+" => SINGLE = "+ps.m_values.get(0));
            }
            else
            {
                nd.setProperty( m_session.fromQName( ps.m_propertyName ), 
                                ps.m_values.toArray( new Value[ps.m_values.size()] ) );

                //System.out.println("  "+ps.m_propertyName+" => MULTI = "+ps.m_values.size()+" items");
            }
        }
        
//        System.out.println("Imported new node "+nd);
        if( m_immediateCommit ) 
        {
            m_session.save();
        }
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
            try
            {
                m_currentPath = m_currentPath.resolve(m_session.toQName( nodeName ) );
            }
            catch( RepositoryException e )
            {
                throw new SAXException(e);
            }
            
            return;
        }
        else if( name.equals("sv:property") )
        {
            checkSystem();
            
            try
            {
                NamespaceRegistryImpl nr = m_session.getWorkspace().getNamespaceRegistry();
                String propName = attrs.getValue("sv:name");
                String propType = attrs.getValue("sv:type");
            
                m_currentProperty = new PropertyStore();
                m_currentProperty.m_propertyName = nr.toQName( propName );
                m_currentProperty.m_propertyType = PropertyType.valueFromName( propType );
            }
            catch( Exception e )
            {
                throw new SAXException("Unable to parse property name",e);
            }
            return;
        }
        else if( name.equals("sv:value") )
        {
            checkSystem();
            
            m_readingValue = true;
            
            return;
        }

        //
        //  Alright, this is then a Document view document.
        //
        checkDocument();
        
        String nodeName = name;

        m_currentStore = new NodeStore();
        m_currentStore.m_nodeName = nodeName;
        try
        {
            for( int i = 0; i < attrs.getLength(); i++ )
            {
                String attname = attrs.getQName(i);
                if( attname.equals( "" ) ) attname = attrs.getLocalName(i);

                m_currentProperty = new PropertyStore();
            
                String attvalue = attrs.getValue(i);
            
                NamespaceRegistryImpl nr = m_session.getWorkspace().getNamespaceRegistry();

                m_currentProperty.m_propertyName = nr.toQName( attname );
                m_currentProperty.m_propertyType = PropertyType.STRING;
                m_currentProperty.addValue( m_session.getValueFactory().createValue( attvalue ) );
                
                m_currentStore.m_properties.add( m_currentProperty );
            }
            m_currentPath = m_currentPath.resolve(m_session.toQName( nodeName ) );

            deserializeStore( m_currentStore );
        }
        catch( RepositoryException e )
        {
            throw new SAXException(e);
        }
        
        return;
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        String name = qName != null ? qName : localName;
        
        if( name.equals("sv:node") )
        {
            boolean isSuper = m_session.setSuper( true );
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
                log.log( Level.WARNING, "Node deserialization failed", e );
                throw new SAXException("Could not deserialize node",e);
            }
            finally
            {
                m_session.setSuper( isSuper );
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
        else
        {
            // Document view
            try
            {
                m_currentPath = m_currentPath.getParentPath();
                m_currentNode = m_currentNode.getParent();
            }
            catch( Exception e )
            {
                throw new SAXException(e);
            }
        }
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException
    {
//        System.out.println(prefix + " = " + uri );
        try
        {
            NamespaceRegistry r = m_session.getWorkspace().getNamespaceRegistry();
            
            r.registerNamespace( prefix, uri );
        }
        catch( NamespaceException e )
        {
            // Ignore quietly; this is usually because there are mappings in the
            // document which are known to us.
            // e.printStackTrace();
        }
        catch( RepositoryException e )
        {
            throw new SAXException(e);
        }
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
        if( m_style == ParserStyle.DOCUMENT )
        {
            try
            {
                String valueString = new String(ch,start,length);
                NodeImpl xmlText = m_currentNode.addNode( "jcr:xmltext" );
                xmlText.setProperty( "jcr:xmlcharacters", valueString );
            }
            catch( RepositoryException e )
            {
                throw new SAXException(e);
            }
            return;
        }
        
        // System
        if( m_readingValue )
        {
            try
            {
                Value v;
                String valueString = new String(ch,start,length);
                if( m_currentProperty.m_propertyType == PropertyType.BINARY )
                {
                    InputStream in = new Base64.InputStream( new ByteArrayInputStream(valueString.getBytes("UTF-8")) );
                    v = m_session.getValueFactory().createValue( in );
                }
                else
                {
                    v = m_session.getValueFactory().createValue( valueString, 
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
            catch( RepositoryException e )
            {
                throw new SAXException("Something horrible happened",e);
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
        
        public PropertyStore findProperty( QName name )
        {
            for( PropertyStore ps : m_properties )
            {
                if( ps.m_propertyName.equals(name) ) return ps;
            }
            
            return null;
        }
        
        @Override
        public String toString()
        {
            return m_nodeName;
        }
    }
    
    private static class PropertyStore
    {
        public QName        m_propertyName;
        public int          m_propertyType;
        private List<Value>  m_values = new ArrayList<Value>();
        
        public void addValue(Value v)
        {
            m_values.add( v );
        }
        
        @Override
        public String toString()
        {
            return m_propertyName.toString();
        }
    }
}
