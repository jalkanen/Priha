package org.jspwiki.priha.nodetype;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.*;
import javax.jcr.version.OnParentVersionAction;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class NodeTypeManagerImpl implements NodeTypeManager
{
    private SortedMap<String,NodeType> m_primaryTypes = new TreeMap<String,NodeType>();
    private SortedMap<String,NodeType> m_mixinTypes   = new TreeMap<String,NodeType>();
    
    private Logger log = Logger.getLogger( getClass().getName() );

    private static NodeTypeManagerImpl c_instance;
    
    // TODO: When created, there is no Session object available.
    
    private NodeTypeManagerImpl()
        throws RepositoryException
    {
        try
        {
            initializeNodeTypeList();
        }
        catch( Exception e )
        {
            e.printStackTrace();
            throw new RepositoryException("Cannot start NodeTypeManager",e);
        }
    }

    // FIXME: Should really return only a singleton per Repository.  Now, clashes are possible,
    //        when multiple sessions are opened.
    public static NodeTypeManagerImpl getInstance(Workspace ws)
        throws RepositoryException
    {
        if( c_instance == null )
        {
            c_instance = new NodeTypeManagerImpl();
        }
        
        return c_instance;
    }
    
    private void initializeNodeTypeList() throws ParserConfigurationException, IOException
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputStream in = null;
        
        try
        {
            in = getClass().getClassLoader().getResourceAsStream( "org/jspwiki/priha/nodetype/builtin_nodetypes.xml" );
            
            Document doc = builder.parse( in );
            
            XPathFactory xpf = XPathFactory.newInstance();
            
            XPath xp = xpf.newXPath();
            
            NodeList types = (NodeList)xp.evaluate( "/nodetypes/nodetype", doc, XPathConstants.NODESET );
         
            if( types.getLength() < 1 )
            {
                log.severe("No default nodes were found!  Everything is likely to be broken!");
            }
            
            for( int i = 0; i < types.getLength(); i++ )
            {
                parseSingleNodeType( types.item(i) );
            }
        }
        catch( SAXException e )
        {
            e.printStackTrace();
        }
        catch (XPathExpressionException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (NoSuchNodeTypeException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (RepositoryException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally
        {
            if( in != null ) try { in.close(); } catch( Exception e ) {}
        }
        
    }
    
    private void parseSingleNodeType(Node node) throws XPathExpressionException, NoSuchNodeTypeException, RepositoryException
    {
        XPath xpath = XPathFactory.newInstance().newXPath();
        
        String name = xpath.evaluate( "name", node );
        log.finest( "Loading nodetype "+name );

        GenericNodeType gnt = new GenericNodeType(name);
        
        //
        //  Basic Node Type properties
        //
        gnt.m_ismixin = getBooleanProperty(xpath,"isMixin", node );
        
        gnt.m_hasOrderableChildNodes = getBooleanProperty(xpath, "hasOrderableChildNodes", node );
                
        String primaryItemName = xpath.evaluate( "primaryItemName", node );
        
        if( primaryItemName != null && primaryItemName.length() > 0 )
            gnt.m_primaryItemName = primaryItemName;

        String superNode = xpath.evaluate( "supertypes", node );

        if( superNode != null && superNode.length() > 0 )
            gnt.m_parent = getNodeType( superNode );
        
        //
        //  Property definitions
        //
        NodeList propertyDefinitions = (NodeList) xpath.evaluate( "propertyDefinition", node, XPathConstants.NODESET );
        
        ArrayList<PropertyDefinition> pdlist = new ArrayList<PropertyDefinition>();
        for( int i = 0; i < propertyDefinitions.getLength(); i++ )
        {
            PropertyDefinition p = parsePropertyDefinition( gnt, propertyDefinitions.item(i) );
            pdlist.add( p );
        }
        
        gnt.m_declaredPropertyDefinitions = (PropertyDefinition[]) pdlist.toArray(new PropertyDefinition[0]);
        
        //  Add parent definitions

        if( gnt.m_parent != null )
        {
            for( PropertyDefinition p : gnt.m_parent.getPropertyDefinitions() )
            {
                pdlist.add( p );
            }
        }
        
        gnt.m_propertyDefinitions = (PropertyDefinition[]) pdlist.toArray(new PropertyDefinition[0]);
        
        //
        //  Add it to the proper place
        //
        
        if( gnt.isMixin() )
            m_mixinTypes.put( name, gnt );
        else
            m_primaryTypes.put( name, gnt );
    }

    private boolean getBooleanProperty( XPath xpath, String expression, Node node ) 
        throws XPathExpressionException
    {
        String res = xpath.evaluate( expression, node );
        
        return "true".equals(res);
    }
    
    private PropertyDefinition parsePropertyDefinition( GenericNodeType parent, Node node ) throws XPathExpressionException
    {
        XPath xpath = XPathFactory.newInstance().newXPath();
        
        String name = xpath.evaluate( "name", node );
        log.finest( "Loading propertyDefinition "+name );
        
        PropertyDefinitionImpl pdi = new PropertyDefinitionImpl(parent,name);
        
        pdi.m_isAutoCreated = getBooleanProperty(xpath, "autoCreated", node);
        pdi.m_isMandatory   = getBooleanProperty(xpath, "mandatory",   node);
        pdi.m_isMultiple    = getBooleanProperty(xpath, "multiple",    node);
        pdi.m_isProtected   = getBooleanProperty(xpath, "protected",   node);
        
        String requiredType = xpath.evaluate( "requiredType", node );
        pdi.m_requiredType  = PropertyType.valueFromName( requiredType );
        
        String onParentVersion = xpath.evaluate( "onParentVersion", node );
        pdi.m_onParentVersion  = OnParentVersionAction.valueFromName( onParentVersion );
        
        return pdi;
    }

    public void addPrimaryNodeType( NodeType nt )
    {
        m_primaryTypes.put( nt.getName(), nt );
    }
    
    public NodeTypeIterator getAllNodeTypes() throws RepositoryException
    {
        List<NodeType> ls = new ArrayList<NodeType>();
        
        ls.addAll( m_primaryTypes.values() );
        ls.addAll( m_mixinTypes.values() );
        
        return new NodeTypeIteratorImpl(ls);
    }

    public NodeTypeIterator getMixinNodeTypes() throws RepositoryException
    {
        List<NodeType> ls = new ArrayList<NodeType>();

        ls.addAll( m_mixinTypes.values() );
        
        return new NodeTypeIteratorImpl(ls);
    }

    public NodeType getNodeType(String nodeTypeName) throws NoSuchNodeTypeException, RepositoryException
    {
        NodeType n = m_primaryTypes.get(nodeTypeName);
        if( n == null )
        {
            n = m_mixinTypes.get(nodeTypeName);
        }
        
        if( n == null ) throw new NoSuchNodeTypeException("No such node type: "+nodeTypeName);
        
        return n;
    }

    public NodeTypeIterator getPrimaryNodeTypes() throws RepositoryException
    {
        List<NodeType> ls = new ArrayList<NodeType>();

        ls.addAll( m_primaryTypes.values() );
        
        return new NodeTypeIteratorImpl(ls);
    }
}
