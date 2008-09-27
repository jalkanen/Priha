/*
    Priha - A JSR-170 implementation library.

    Copyright (C) 2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    Licensed under the Apache License, Version 2.0 (the "License"); 
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at 
    
      http://www.apache.org/licenses/LICENSE-2.0 
      
    Unless required by applicable law or agreed to in writing, software 
    distributed under the License is distributed on an "AS IS" BASIS, 
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
    See the License for the specific language governing permissions and 
    limitations under the License. 
 */
package org.priha.nodetype;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.*;
import javax.jcr.version.OnParentVersionAction;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.priha.core.RepositoryImpl;
import org.priha.core.WorkspaceImpl;
import org.priha.core.namespace.NamespaceMapper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *  This class is essentially a singleton per repository.
 *  
 *  @author jalkanen
 *
 */
public class QNodeTypeManager
{
    private SortedMap<String,QNodeType> m_primaryTypes = new TreeMap<String,QNodeType>();
    private SortedMap<String,QNodeType> m_mixinTypes   = new TreeMap<String,QNodeType>();

    private Logger log = Logger.getLogger( getClass().getName() );

    private static QNodeTypeManager c_instance;

    // TODO: When created, there is no Session object available.

    private QNodeTypeManager()
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

    public static synchronized QNodeTypeManager getInstance() throws RepositoryException
    {
        if( c_instance == null )
        {
            c_instance = new QNodeTypeManager();
        }

        return c_instance;
    }
    
    
    public static synchronized Impl getManager(WorkspaceImpl ws)
        throws RepositoryException
    {
        return getInstance().new Impl(ws.getSession());
    }

    private void initializeNodeTypeList() throws ParserConfigurationException, IOException
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputStream in = null;

        try
        {
            in = getClass().getClassLoader().getResourceAsStream( "org/priha/nodetype/builtin_nodetypes.xml" );

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
        NamespaceMapper nsm = RepositoryImpl.getGlobalNamespaceRegistry();
        
        String name = xpath.evaluate( "name", node );
        log.finest( "Loading nodetype "+name );

        QNodeType gnt = new QNodeType( nsm.toQName( name ) );

        //
        //  Basic Node Type properties
        //
        gnt.m_ismixin = getBooleanProperty(xpath,"isMixin", node );

        gnt.m_hasOrderableChildNodes = getBooleanProperty(xpath, "hasOrderableChildNodes", node );

        String primaryItemName = xpath.evaluate( "primaryItemName", node );

        if( primaryItemName != null && primaryItemName.length() > 0 )
            gnt.m_primaryItemName = nsm.toQName( primaryItemName );

        String superNode = xpath.evaluate( "supertypes", node );

        if( superNode != null && superNode.length() > 0 )
        {
            String[] nodes = parseList( superNode );

            gnt.m_parents = new QNodeType[nodes.length];
            for( int i = 0; i < nodes.length; i++ )
            {
                gnt.m_parents[i] = getNodeType( nsm.toQName( nodes[i] ) );
            }
        }

        //
        //  Property definitions
        //
        NodeList propertyDefinitions = (NodeList) xpath.evaluate( "propertyDefinition", node, XPathConstants.NODESET );

        ArrayList<QPropertyDefinition> pdlist = new ArrayList<QPropertyDefinition>();
        for( int i = 0; i < propertyDefinitions.getLength(); i++ )
        {
            QPropertyDefinition p = parsePropertyDefinition( gnt, propertyDefinitions.item(i) );
            pdlist.add( p );
        }

        gnt.m_declaredPropertyDefinitions = pdlist.toArray(new QPropertyDefinition[0]);

        //  Add parent definitions

        if( gnt.m_parents != null )
        {
            for( QNodeType nt : gnt.m_parents )
            {
                for( QPropertyDefinition p : nt.m_propertyDefinitions )
                {
                    pdlist.add( p );
                }
            }
        }

        gnt.m_propertyDefinitions = pdlist.toArray(new QPropertyDefinition[0]);

        //
        //  Child node definitions
        NodeList nodeDefinitions = (NodeList) xpath.evaluate( "childNodeDefinition", node, XPathConstants.NODESET );

        ArrayList<QNodeDefinition> ndlist = new ArrayList<QNodeDefinition>();
        for( int i = 0; i < nodeDefinitions.getLength(); i++ )
        {
            QNodeDefinition p = parseChildNodeDefinition( gnt, nodeDefinitions.item(i) );
            ndlist.add( p );
        }

        gnt.m_childNodeDefinitions = ndlist.toArray(new QNodeDefinition[0]);

        //
        //  Add it to the proper place
        //

        if( gnt.m_ismixin )
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

    private QPropertyDefinition parsePropertyDefinition( QNodeType parent, Node node ) throws XPathExpressionException, NamespaceException
    {
        XPath xpath = XPathFactory.newInstance().newXPath();

        String name = xpath.evaluate( "name", node );
        log.finest( "Loading propertyDefinition "+name );

        QPropertyDefinition pdi = new QPropertyDefinition(parent,
                                                          RepositoryImpl.getGlobalNamespaceRegistry().toQName( name ) );

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

    private QNodeDefinition parseChildNodeDefinition( QNodeType parent, Node node) throws XPathExpressionException, NoSuchNodeTypeException, RepositoryException
    {
        NamespaceMapper nsm = RepositoryImpl.getGlobalNamespaceRegistry();
        XPath xpath = XPathFactory.newInstance().newXPath();

        String name = xpath.evaluate( "name", node );
        log.finest("Loading node definition "+name);

        QNodeDefinition nd = new QNodeDefinition( parent, nsm.toQName( name ) );

        String requiredType = xpath.evaluate( "requiredType", node );

        if( requiredType != null && requiredType.length() > 0 )
        {
            QName requiredQType = nsm.toQName( requiredType );
            QNodeType[] reqd = new QNodeType[1];

            if( requiredQType.equals( parent.getQName() ) )
                reqd[0] = parent;
            else
                reqd[0] = getNodeType( requiredQType );

            nd.m_requiredPrimaryTypes = reqd;
        }

        String defaultType = xpath.evaluate( "defaultPrimaryType", node );

        if( defaultType != null && defaultType.length() > 0 )
        {
            QName defaultQType = nsm.toQName( defaultType );
            if( defaultQType.equals(parent.getQName()) )
                nd.m_defaultPrimaryType = parent;
            else
                nd.m_defaultPrimaryType = getNodeType( defaultQType );
        }

        nd.m_isAutoCreated = getBooleanProperty(xpath, "autoCreated", node);
        nd.m_isMandatory   = getBooleanProperty(xpath, "mandatory", node);
        nd.m_isProtected   = getBooleanProperty(xpath, "protected", node);
        nd.m_allowsSameNameSiblings = getBooleanProperty(xpath, "sameNameSiblings", node);

        String onParentVersion = xpath.evaluate( "onParentVersion", node );
        nd.m_onParentVersion  = OnParentVersionAction.valueFromName( onParentVersion );

        return nd;
    }

    /**
     *  Finds a node definition from the complete array of all definitions
     *
     *  @param type
     *  @return
     */
    public QNodeDefinition findNodeDefinition(QName type)
    {
        for( QNodeType nt : m_primaryTypes.values() )
        {
            for( QNodeDefinition nd : nt.m_childNodeDefinitions )
            {
                if( nd.getQName().equals( type ) )
                    return nd;
            }
        }

        for( QNodeType nt : m_mixinTypes.values() )
        {
            for( QNodeDefinition nd : nt.m_childNodeDefinitions )
            {
                if( nd.getQName().equals( type ) )
                    return nd;
            }
        }

        //
        //  Find the default
        //
        for( QNodeType nt : m_primaryTypes.values() )
        {
            for( QNodeDefinition nd : nt.m_childNodeDefinitions )
            {
                if( nd.getQName().toString().equals( "*" ) )
                    return nd;
            }
        }

        return null;
    }


    private String[] parseList( String list )
    {
        StringTokenizer st = new StringTokenizer(list,", ");

        String[] result = new String[st.countTokens()];

        for( int i = 0; i < result.length; i++ )
        {
            result[i] = st.nextToken();
        }

        return result;
    }
    
    /**
     *  Find a QNodeType by this QName.
     *  
     *  @param qn QName to look for
     *  @return A QNodeType corresponding to this QName
     *  @throws NoSuchNodeTypeException If it could not be located.
     */
    public QNodeType getNodeType( QName qn ) throws NoSuchNodeTypeException
    {
        QNodeType n = m_primaryTypes.get(qn);
        if( n == null )
        {
            n = m_mixinTypes.get(qn);
        }

        if( n == null ) throw new NoSuchNodeTypeException("No such node type: "+qn);

        return n;
    }

    /**
     *  Implements the actual NodeTypeManager class, which, again, is
     *  Session-specific.
     */
    public class Impl implements NodeTypeManager
    {
        private NamespaceMapper m_mapper;
        
        public Impl( NamespaceMapper nsm )
        {
            m_mapper = nsm;
        }
        
        public NodeTypeIterator getAllNodeTypes() throws RepositoryException
        {
            List<NodeType> ls = new ArrayList<NodeType>();

            for( NodeTypeIterator ni = getPrimaryNodeTypes(); ni.hasNext(); )
            {
                ls.add( ni.nextNodeType() );
            }
            for( NodeTypeIterator ni = getMixinNodeTypes(); ni.hasNext(); )
            {
                ls.add( ni.nextNodeType() );
            }


            return new NodeTypeIteratorImpl(ls);
        }

        public NodeTypeIterator getMixinNodeTypes() throws RepositoryException
        {
            List<NodeType> ls = new ArrayList<NodeType>();

            for( QNodeType qnt : m_mixinTypes.values() )
            {
                ls.add( qnt.new Impl(m_mapper) );
            }
            
            return new NodeTypeIteratorImpl(ls);
        }

        public QNodeType.Impl getNodeType(String nodeTypeName) throws NoSuchNodeTypeException, RepositoryException
        {
            QName qn = m_mapper.toQName( nodeTypeName );
            
            QNodeType qnt = QNodeTypeManager.this.getNodeType(qn);
            
            return qnt.new Impl(m_mapper);
            
        }

        public NodeTypeIterator getPrimaryNodeTypes() throws RepositoryException
        {
            List<NodeType> ls = new ArrayList<NodeType>();

            for( QNodeType qnt : m_primaryTypes.values() )
            {
                ls.add( qnt.new Impl(m_mapper) );
            }

            return new NodeTypeIteratorImpl(ls);
        }

    }
}
