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
package org.priha.core;

import static org.priha.core.JCRConstants.JCR_UUID;
import static org.priha.core.JCRConstants.Q_JCR_BASEVERSION;
import static org.priha.core.JCRConstants.Q_JCR_CREATED;
import static org.priha.core.JCRConstants.Q_JCR_ISCHECKEDOUT;
import static org.priha.core.JCRConstants.Q_JCR_MIXINTYPES;
import static org.priha.core.JCRConstants.Q_JCR_PRIMARYTYPE;
import static org.priha.core.JCRConstants.Q_JCR_UUID;
import static org.priha.core.JCRConstants.Q_MIX_VERSIONABLE;
import static org.priha.core.JCRConstants.Q_NT_BASE;
import static org.priha.core.JCRConstants.Q_NT_UNSTRUCTURED;

import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.*;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.xml.namespace.QName;

import org.priha.core.locks.LockImpl;
import org.priha.core.locks.LockManager;
import org.priha.core.values.ValueFactoryImpl;
import org.priha.core.values.ValueImpl;
import org.priha.nodetype.QNodeDefinition;
import org.priha.nodetype.QNodeType;
import org.priha.nodetype.QNodeTypeManager;
import org.priha.nodetype.QPropertyDefinition;
import org.priha.util.*;
import org.priha.version.VersionHistoryImpl;
import org.priha.version.VersionImpl;
import org.priha.version.VersionManager;

public class NodeImpl extends ItemImpl implements Node, Comparable<Node>
{
    private static final String JCR_PREDECESSORS = "jcr:predecessors";
    private static final String JCR_SUCCESSORS = "jcr:successors";

    private QNodeDefinition      m_definition;
    
    private QNodeType            m_primaryType;

    static Logger log = Logger.getLogger( NodeImpl.class.getName() );

    /**
     *  Creates a clone of the NodeImpl, and places it to the given Session
     *  
     *  @param original
     *  @param session
     *  @throws RepositoryException 
     *  @throws IllegalStateException 
     *  @throws ValueFormatException 
     */
    protected NodeImpl( NodeImpl original, SessionImpl session ) throws ValueFormatException, IllegalStateException, RepositoryException
    {
        super( original, session );
        
        m_primaryType = original.m_primaryType;
        m_definition  = original.m_definition;
    }
    
    protected NodeImpl( SessionImpl session, Path path, QNodeType primaryType, QNodeDefinition nDef, boolean populateDefaults )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        super( session, path );

        m_primaryType = primaryType;
        m_definition  = nDef;
        
        if( populateDefaults )
        {
            internalSetProperty( Q_JCR_PRIMARYTYPE, 
                                 session.fromQName( m_primaryType.getQName() ), // FIXME: Not very efficient 
                                 PropertyType.NAME );
        }
    }


    protected NodeImpl( SessionImpl session, String path, QNodeType primaryType, QNodeDefinition nDef, boolean populateDefaults )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        this( session, PathFactory.getPath(session,path), primaryType, nDef, populateDefaults );
    }
        

    /**
     *  Figures out what type the child node should be.
     *  
     *  @param relpath A relative path to the child node.
     *  @return A NodeType for the path
     *  @throws RepositoryException If something goes wrong.
     */
    private QNodeType assignChildType(QName relpath) throws RepositoryException
    {
        QNodeDefinition nd = m_primaryType.findNodeDefinition(relpath);

        if( nd == null )
        {
            throw new ConstraintViolationException("Cannot assign a child type to this node, since there is no default type.");
        }
        
        QNodeType nt = nd.getDefaultPrimaryType();

        return nt;
    }

    public NodeImpl addNode(String relPath)
        throws ItemExistsException,
               PathNotFoundException,
               NoSuchNodeTypeException,
               LockException,
               VersionException,
               ConstraintViolationException,
               RepositoryException
    {
        NodeImpl nd = addNode(relPath, null);

        return nd;
    }

    public NodeImpl addNode(String relPath, String primaryNodeTypeName)
                                       throws ItemExistsException,
                                           PathNotFoundException,
                                           VersionException,
                                           ConstraintViolationException,
                                           LockException,
                                           RepositoryException
    {
        if( relPath.indexOf('[') != -1 )
        {
            throw new RepositoryException("Cannot add an indexed entry");
        }
        
        Path absPath = m_path.resolve(m_session,relPath);

        NodeImpl ni = null;
        try
        {
            //
            //  Are we locked without a token?
            //
            
            if( isLockedWithoutToken() )
                throw new LockException("This node is locked and cannot add a child Node!");
            
            //
            //  Check out if parent is okay with the addition of this node
            //
            Path parentPath = absPath.getParentPath();

            Item item = m_session.getItem(parentPath);

            if( !item.isNode() )
            {
                throw new ConstraintViolationException("Trying to add a node to a Property");
            }

            NodeImpl parent = (NodeImpl) item;

            if( parent.m_state == ItemState.REMOVED )
            {
                throw new ConstraintViolationException("Parent has been removed");
            }

            //
            //  Parent is okay with this addition, so we'll continue with
            //  figuring out what the node type of this node should be.
            //
            QNodeType        assignedType;
            QNodeDefinition  assignedNodeDef;

            if( primaryNodeTypeName == null )
            {
                assignedType = parent.assignChildType(absPath.getLastComponent());
            }
            else
            {
                assignedType = getNodeTypeManager().getNodeType( primaryNodeTypeName ).getQNodeType();
            }

            assignedNodeDef = m_primaryType.findNodeDefinition( absPath.getLastComponent() );

            if( assignedNodeDef == null )
            {
                throw new ConstraintViolationException("No node definition for this node type");
            }
            
            //
            //  Check for same name siblings
            //
            if( m_session.itemExists(absPath) )
            {

                NodeDefinition nd = parent.getDefinition();

                if( !nd.allowsSameNameSiblings() )
                {
                    // FIXME: This should really check if samenamesiblings are allowed
                    throw new ItemExistsException("Node "+absPath+" already exists!");
                }
            }

            //
            //  Check if parent allows adding this.
            //
            if( !parent.getPrimaryNodeType().canAddChildNode(getSession().fromQName(absPath.getLastComponent())) )
            {
                throw new ConstraintViolationException("Parent node does not allow adding nodes of name "+absPath.getLastComponent());
            }

            //
            //  Node type and definition are now okay, so we'll create the node
            //  and add it to our session.
            //
            ni = m_session.createNode(absPath, assignedType, assignedNodeDef,true);

            ni.sanitize();

            ni.markModified(false);
            //m_session.addNode( ni ); // Already taken care of by markModified
        }
        catch( InvalidPathException e)
        {
            throw new PathNotFoundException( e.getMessage(), e );
        }
        return ni;
    }



    private QNodeTypeManager.Impl getNodeTypeManager() throws RepositoryException
    {
        return m_session.getWorkspace().getNodeTypeManager();
    }

    public String getCorrespondingNodePath(String workspaceName)
                                                                throws ItemNotFoundException,
                                                                    NoSuchWorkspaceException,
                                                                    AccessDeniedException,
                                                                    RepositoryException
    {
        Session internalSession = m_session.getRepository().login(workspaceName);
        Path    correspondingPath;
        
        try
        {
            String uuid = null;
            NodeImpl nd = this;
            
            //
            //  Find the nearest parent with the correct UUID
            //
            while( uuid == null && !nd.getInternalPath().isRoot() )
            {
                try
                {
                    uuid = nd.getUUID();
                }
                catch( UnsupportedRepositoryOperationException e ) 
                {
                    nd = nd.getParent();
                }
            }
            
            correspondingPath = getInternalPath().getSubpath( nd.getDepth() );
            
            if( uuid != null )
            {
                NodeImpl nodeInOtherWS = (NodeImpl)internalSession.getNodeByUUID(uuid);
            
                return nodeInOtherWS.getInternalPath().resolve(m_session,correspondingPath.toString()).toString();
            }
            
            return ((NodeImpl)internalSession.getRootNode()).getNode(correspondingPath).getPath();
        }
        catch( PathNotFoundException e )
        {
            throw new ItemNotFoundException();
        }
        finally
        {
            internalSession.logout();
        }
    }

    public NodeDefinition getDefinition() throws RepositoryException
    {
        if( m_definition == null ) sanitize();

        return m_definition.new Impl(m_session);
    }

    public int getIndex() throws RepositoryException
    {
        // Not supported, so always constant
        return 1;
    }

    public NodeImpl getNode( Path absPath ) throws PathNotFoundException, RepositoryException
    {
        Item i = m_session.getItem(absPath);

        if( i.isNode() )
        {
            return (NodeImpl) i;
        }

        throw new PathNotFoundException("Path refers to a property: "+absPath.toString());
    }
    
    public NodeImpl getNode(String relPath) throws PathNotFoundException, RepositoryException
    {
        return getNode( m_path.resolve(m_session,relPath) );
    }

    public NodeImpl getNode(QName name) throws PathNotFoundException, RepositoryException
    {
        return getNode( m_path.resolve(name) );
    }

    public NodeIteratorImpl getNodes() throws RepositoryException
    {
        List<NodeImpl> ls = new ArrayList<NodeImpl>();

        Set<Path> children = m_session.listNodes( m_path );
        
        for( Path p : children )
        {
            NodeImpl nd = getNode( p );
            ls.add( nd );
        }

        NodeIteratorImpl it = new NodeIteratorImpl(ls);

        return it;
    }

    public NodeIterator getNodes(String namePattern) throws RepositoryException
    {
        Pattern p = TextUtil.parseJCRPattern(namePattern);

        ArrayList<NodeImpl> matchedpaths = new ArrayList<NodeImpl>();

        Set<Path> children = m_session.listNodes( m_path );
        
        for( Path path : children )
        {
            Matcher match = p.matcher( path.getLastComponent().toString() );

            if( match.matches() )
            {
                matchedpaths.add( getNode(path) );
            }
        }

        return new NodeIteratorImpl(matchedpaths);
    }

    public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException
    {
        NodeType nd = getPrimaryNodeType();

        String primaryItem = nd.getPrimaryItemName();

        if( primaryItem != null )
        {
            return getChildProperty( primaryItem );
        }

        throw new ItemNotFoundException( getPath()+" does not declare a primary item" );
    }

    public QNodeType getPrimaryQNodeType()
    {
        return m_primaryType;
    }
    
    public QNodeType.Impl getPrimaryNodeType() throws RepositoryException
    {
        return m_primaryType.new Impl(m_session);
    }

    public PropertyIteratorImpl getProperties() throws RepositoryException
    {
        List<PropertyImpl> ls = new ArrayList<PropertyImpl>();
        
        ls.addAll( m_session.m_provider.getProperties(getInternalPath()) );
        
        return new PropertyIteratorImpl( ls );
    }

    public PropertyIterator getProperties(String namePattern) throws RepositoryException
    {
        Pattern p = TextUtil.parseJCRPattern(namePattern);

        ArrayList<PropertyImpl> matchedpaths = new ArrayList<PropertyImpl>();
        
        for( PropertyIterator i = getProperties(); i.hasNext(); )
        {
            PropertyImpl prop = (PropertyImpl)i.nextProperty();
            Matcher match = p.matcher( prop.getName() );

            if( match.matches() )
            {
                matchedpaths.add( prop );
            }
        }

        return new PropertyIteratorImpl(matchedpaths);
    }

    public PropertyImpl getChildProperty( String name )
        throws RepositoryException
    {
        ItemImpl ii = m_session.m_provider.getItem( m_path.resolve(m_session,name) );
        
        if( ii.isNode() ) throw new ItemNotFoundException("Found a Node, not a Property");
        
        return (PropertyImpl) ii;
    }

    public PropertyImpl getProperty( QName propName ) throws PathNotFoundException, RepositoryException
    {
        Path abspath = m_path.resolve(propName);
        
        Item item = m_session.getItem(abspath);

        if( item != null && !item.isNode() )
        {
            return (PropertyImpl) item;
        }

        throw new PathNotFoundException( abspath.toString() );        
    }
    
    public PropertyImpl getProperty(String relPath) throws PathNotFoundException, RepositoryException
    {
        Path abspath = m_path.resolve(m_session,relPath);

        Item item = m_session.getItem(abspath);

        if( item != null && !item.isNode() )
        {
            return (PropertyImpl) item;
        }

        throw new PathNotFoundException( abspath.toString() );
    }

    void autoCreateProperties() throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        autoCreateProperties( getPrimaryQNodeType() );

        for( NodeType nt : getMixinNodeTypes() )
        {
            autoCreateProperties( ((QNodeType.Impl)nt).getQNodeType() ); //FIXME: Unnecessary
        }
    }

    /**
     *  This method autocreates the properties for this node, which are marked to be automatically created
     *  by the definition.
     *  
     *  @param nt The NodeType
     *  @throws RepositoryException
     *  @throws ValueFormatException
     *  @throws VersionException
     *  @throws LockException
     *  @throws ConstraintViolationException
     */
    private void autoCreateProperties(QNodeType nt) throws RepositoryException, ValueFormatException, VersionException, LockException, ConstraintViolationException
    {
        ValueFactoryImpl vfi = m_session.getValueFactory();
        
        //
        //  Special cases.
        //
        
        if( nt.getQName().equals( Q_MIX_VERSIONABLE ) )
        {
            VersionManager.createVersionHistory( this );
        }
        
        for( QPropertyDefinition pd : nt.getQPropertyDefinitions() )
        {
            if( pd.isAutoCreated() && !hasProperty(pd.getQName()) )
            {
                log.finest("Autocreating property "+pd.getQName());

                Path p = m_path.resolve(pd.getQName());

                PropertyImpl pi = new PropertyImpl(m_session,
                                                   p,
                                                   pd);

                // FIXME: Add default value generation

                if( Q_JCR_UUID.equals(pi.getQName()) )
                {
                    pi.loadValue( vfi.createValue( UUID.randomUUID().toString() ) );
                }
                else if( Q_JCR_CREATED.equals(pi.getQName() ))
                {
                    pi.loadValue( vfi.createValue( Calendar.getInstance() ) );
                }
                else if( Q_JCR_ISCHECKEDOUT.equals(pi.getQName()))
                {
                    pi.loadValue( vfi.createValue( true ) );
                }
                else if( Q_JCR_PRIMARYTYPE.equals(pi.getQName()))
                {
                    //
                    //  This is just a guess
                    //
                    pi.loadValue( vfi.createValue( Q_NT_UNSTRUCTURED, PropertyType.NAME ) );
                }
                else
                {
                    throw new UnsupportedRepositoryOperationException("Automatic setting of property "+pi.getQName()+ " is not supported.");
                }
                pi.markModified( true );
                //addChildProperty( pi );
            }
        }
    }

    public PropertyIterator getReferences() throws RepositoryException
    {
        List<PropertyImpl> references = m_session.getReferences( getUUID() );

        return new PropertyIteratorImpl(references);
    }

    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException
    {
        if( isNodeType("mix:referenceable") )
        {
            try
            {
                Property uuid = getProperty(JCR_UUID);

                return uuid.getValue().getString();
            }
            catch( PathNotFoundException e )
            {
                // Fine, let's just fall through, and end up throwing an exception
            }
        }

        throw new UnsupportedRepositoryOperationException("No UUID defined for this node");
    }

    public VersionHistoryImpl getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException
    {
        if( isNodeType("mix:versionable") )
        {
            // Locate version history

            Path versionPath = VersionManager.getVersionStoragePath( getUUID() );

            VersionHistoryImpl vh = (VersionHistoryImpl)getNode( versionPath );

            return vh;
        }

        throw new UnsupportedRepositoryOperationException("This node does not have a version history.");
    }

    public boolean hasNode(String relPath) throws RepositoryException
    {
        Path absPath = m_path.resolve(m_session,relPath);
        return m_session.hasNode(absPath);
    }


    public boolean hasNode(QName name) throws RepositoryException
    {
        Path absPath = m_path.resolve(name);
        return m_session.hasNode(absPath);
    }

    public boolean hasNodes() throws RepositoryException
    {
        return getNodes().getSize() > 0;
    }

    public boolean hasProperties() throws RepositoryException
    {
        // FIXME: Slow.
        return getProperties().getSize() > 0;
    }

    public boolean hasProperty( QName propName ) throws RepositoryException
    {
        Path abspath = m_path.resolve(propName);
        return m_session.hasProperty(abspath);
    }
    
    public boolean hasProperty(String relPath) throws RepositoryException
    {
        Path abspath = m_path.resolve(m_session,relPath);
        
        return m_session.hasProperty( abspath );
    }

    public boolean isNodeType(String nodeTypeName) throws RepositoryException
    {
        NodeType primary = getPrimaryNodeType();

        if( !primary.isNodeType( nodeTypeName ) )
        {
            NodeType[] mixins = getMixinNodeTypes();

            for( int i = 0; i < mixins.length; i++ )
            {
                if( mixins[i].isNodeType(nodeTypeName) ) return true;
            }

            //
            //  Not a primary, nor any of the mixins
            //
            return false;
        }

        return true;
    }


    public void orderBefore(String srcChildRelPath, String destChildRelPath)
                                                                            throws UnsupportedRepositoryOperationException,
                                                                                VersionException,
                                                                                ConstraintViolationException,
                                                                                ItemNotFoundException,
                                                                                LockException,
                                                                                RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();

    }


    private PropertyImpl prepareProperty( String name, Object value ) throws PathNotFoundException, RepositoryException
    {
        return prepareProperty( m_session.toQName(name), value );
    }
    
    /**
     *  Finds a property and checks if we're supposed to remove it or not.  It also creates
     *  the property if it does not exist.
     *
     *  @param name
     *  @param value
     *  @return
     *  @throws PathNotFoundException
     *  @throws RepositoryException
     */
    private PropertyImpl prepareProperty( QName name, Object value ) throws PathNotFoundException, RepositoryException
    {
        PropertyImpl prop = null;

        if( !getSession().isSuper() )
        {
            if( isLockedWithoutToken() )
                throw new LockException("Path is locked");
        
            if( !isCheckedOut() )
                throw new VersionException("Node is not checked out.");
            
            if( name.equals( Q_JCR_MIXINTYPES ) )
                throw new ConstraintViolationException("Manually setting mixinTypes is not allowed.");
        }
        
        //
        //  Because we rely quite a lot on the primary property, we need to go and
        //  handle it separately.
        //
        if( name.equals( Q_JCR_PRIMARYTYPE ) )
        {
            if( hasProperty( Q_JCR_PRIMARYTYPE ) )
            {
                throw new ConstraintViolationException("The object has already been assigned a primary type!");
            }

            //  We know where this belongs to.
            QNodeType gnt = QNodeTypeManager.getInstance().getNodeType( Q_NT_BASE );

            QPropertyDefinition primaryDef = gnt.findPropertyDefinition( JCRConstants.Q_JCR_PRIMARYTYPE,
                                                                         false);

            prop = new PropertyImpl( m_session,
                                     m_path.resolve(name),
                                     primaryDef );

            addChildProperty( prop ); //  Again, a special case.  First add the property to the lists.
            markModified(true,false); //  Then, mark this node modified, but don't mark the parent.
            return prop;
        }

        try
        {
            prop = getProperty(name);
        }
        catch( PathNotFoundException e ){}
        catch( ItemNotFoundException e ){}

        if( prop == null )
        {
            Path propertypath = m_path.resolve(name);

            Path p = propertypath.getParentPath();

            NodeImpl parentNode = (NodeImpl) m_session.getItem(p);

            boolean ismultiple = value instanceof Object[];

            QNodeType parentType = parentNode.getPrimaryQNodeType();
            
            QPropertyDefinition pd = parentType.findPropertyDefinition(name,ismultiple);
            
            if( pd == null ) throw new RepositoryException("No propertydefinition found for "+parentType+" and "+name);
            
            prop = new PropertyImpl( m_session, propertypath, pd );
            prop.markModified(false); // New properties are not considered modified
        }
        else
        {
            prop.markModified( true ); // But old properties are.
        }
        
        if( value == null )
        {
            removeProperty(prop);
        }
        else
        {
            //addChildProperty( prop );
        }
        return prop;
    }


    /**
     *  Removes a given property from the node.
     *  @param prop
     */
    protected void removeProperty(PropertyImpl prop) throws RepositoryException
    {
        prop.m_state = ItemState.REMOVED;
        markModified(true);
    }

    public PropertyImpl setProperty(String name, Value value)
                                                         throws ValueFormatException,
                                                             VersionException,
                                                             LockException,
                                                             ConstraintViolationException,
                                                             RepositoryException
    {
        PropertyImpl p = prepareProperty( name, value );

        p.setValue(value);
        
        return p;
    }

    public PropertyImpl setProperty(String name, Value value, int type)
                                                                   throws ValueFormatException,
                                                                       VersionException,
                                                                       LockException,
                                                                       ConstraintViolationException,
                                                                       RepositoryException
    {
        //
        //  Again, setting a value to null is the same as removing the property.
        //
        if( value == null )
        {
            PropertyImpl p = getProperty(name);
            p.remove();
            return p;
        }
        
        if( value.getType() != type )
        {
            throw new ConstraintViolationException("Type of the Value and the type parameter must match.");
        }
        
        PropertyImpl p = prepareProperty( name, value );

        p.setValue(value);

        return p;
    }

    public PropertyImpl setProperty(String name, Value[] values)
                                                            throws ValueFormatException,
                                                                VersionException,
                                                                LockException,
                                                                ConstraintViolationException,
                                                                RepositoryException
    {
        PropertyImpl p = prepareProperty( name, values );

        p.setValue(values);

        return p;
    }

    private PropertyImpl internalSetProperty(QName name, Value[] values, int type) throws PathNotFoundException, RepositoryException
    {
        PropertyImpl p = prepareProperty( name, values );

        p.loadValue( values );

        return p;       
    }


    public Property setProperty(String name, Value[] values, int type)
                                                                      throws ValueFormatException,
                                                                          VersionException,
                                                                          LockException,
                                                                          ConstraintViolationException,
                                                                          RepositoryException
    {
        if( values == null )
        {
            Property p = getProperty(name);
            p.remove();
            return p;
        }
        
        if( values.length > 0 && values[0] != null && type != values[0].getType() )
            throw new ValueFormatException("Do not know how to convert between types, sorry.");

        PropertyImpl p = prepareProperty( name, values );

        p.setValue( values );

        return p;
    }

    public Property setProperty(String name, String[] values)
                                                             throws ValueFormatException,
                                                                 VersionException,
                                                                 LockException,
                                                                 ConstraintViolationException,
                                                                 RepositoryException
    {
        PropertyImpl p = prepareProperty( name, values );

        p.setValue(values);

        return p;
    }

    public Property setProperty(String name, String[] values, int type)
                                                                       throws ValueFormatException,
                                                                           VersionException,
                                                                           LockException,
                                                                           ConstraintViolationException,
                                                                           RepositoryException
    {
        //
        //  For this method, we create a proper value array according to the type information.
        //
        if( values == null )
        {
            Property p = getProperty(name);
            p.remove();
            return p;
        }
        
        PropertyImpl p = prepareProperty( name, values );

        p.setValue( values );

        p.m_type = type;

        return p;
    }

    private PropertyImpl internalSetProperty(QName name, String value, int type) throws PathNotFoundException, RepositoryException
    {
        PropertyImpl prop = prepareProperty(name,value);
        
        prop.loadValue( m_session.getValueFactory().createValue(value,type) );
        
        return prop;
    }

    public PropertyImpl setProperty(String name, String value)
                                                          throws ValueFormatException,
                                                              VersionException,
                                                              LockException,
                                                              ConstraintViolationException,
                                                              RepositoryException
    {
        PropertyImpl prop = prepareProperty(name,value);
        prop.setValue(value);

        return prop;
    }

    public PropertyImpl setProperty(String name, String value, int type)
                                                                    throws ValueFormatException,
                                                                        VersionException,
                                                                        LockException,
                                                                        ConstraintViolationException,
                                                                        RepositoryException
    {
        if( value == null )
        {
            PropertyImpl p = getProperty(name);
            p.remove();
            return p;
        }
        
        try
        {
            Value val = m_session.getValueFactory().createValue( value, type );
            return setProperty( name, val );
        }
        catch( ValueFormatException e )
        {
            // This is kind of stupid to start throwing the same exception again.
            throw new ConstraintViolationException(e.getMessage());
        }
    }

    public PropertyImpl setProperty(String name, InputStream value)
                                                               throws ValueFormatException,
                                                                   VersionException,
                                                                   LockException,
                                                                   ConstraintViolationException,
                                                                   RepositoryException
    {
        PropertyImpl p = prepareProperty( name, value );

        p.setValue(value);

        return p;
    }

    public PropertyImpl setProperty(String name, boolean value)
                                                           throws ValueFormatException,
                                                               VersionException,
                                                               LockException,
                                                               ConstraintViolationException,
                                                               RepositoryException
    {
        PropertyImpl p = prepareProperty( name, value );

        p.setValue(value);

        return p;
    }

    public PropertyImpl setProperty(String name, double value)
                                                          throws ValueFormatException,
                                                              VersionException,
                                                              LockException,
                                                              ConstraintViolationException,
                                                              RepositoryException
    {
        PropertyImpl p = prepareProperty( name, value );

        p.setValue(value);

        return p;
    }

    public PropertyImpl setProperty(String name, long value)
                                                        throws ValueFormatException,
                                                            VersionException,
                                                            LockException,
                                                            ConstraintViolationException,
                                                            RepositoryException
    {
        PropertyImpl p = prepareProperty( name, value );

        p.setValue(value);

        return p;
    }

    public PropertyImpl setProperty(String name, Calendar value)
                                                            throws ValueFormatException,
                                                                VersionException,
                                                                LockException,
                                                                ConstraintViolationException,
                                                                RepositoryException
    {
        PropertyImpl p = prepareProperty( name, value );

        p.setValue(value);

        return p;
    }

    public PropertyImpl setProperty(String name, Node value)
                                                        throws ValueFormatException,
                                                            VersionException,
                                                            LockException,
                                                            ConstraintViolationException,
                                                            RepositoryException
    {
        PropertyImpl p = prepareProperty( name, value );

        p.setValue(value);

        return p;
    }


    public void update(String srcWorkspaceName)
                                               throws NoSuchWorkspaceException,
                                                   AccessDeniedException,
                                                   LockException,
                                                   InvalidItemStateException,
                                                   RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();

    }

    public boolean isNode()
    {
        return true;
    }


    protected void internalSave() throws RepositoryException
    {
        m_session.saveNodes( getInternalPath() );
    }

    // FIXME: No rollback support
    public void save()
        throws AccessDeniedException,
               ItemExistsException,
               ConstraintViolationException,
               InvalidItemStateException,
               ReferentialIntegrityException,
               VersionException,
               LockException,
               NoSuchNodeTypeException,
               RepositoryException
    {
        if( m_state == ItemState.NEW )
            throw new InvalidItemStateException("Cannot call save on newly added node "+m_path);

        internalSave();
    }

    public int compareTo(Node nd)
    {
        try
        {
            return getPath().compareTo(nd.getPath());
        }
        catch( RepositoryException e )
        {
            return 0;
        } // FIXME: This should never occur
    }

    public void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        if( m_state == ItemState.REMOVED )
            //throw new ConstraintViolationException(getPath()+" has already been removed");
            return; // Die nicely
            
        if( !m_session.m_provider.nodeExistsInRepository( m_path ) )
        {
            throw new InvalidItemStateException("Item has already been removed by another Session "+getPath());
        }
        
        if( getPath().equals("/") || getPath().equals("/jcr:system") ) return; // Refuse to remove

        NodeType parentType = getParent().getPrimaryNodeType();
    
        if( !getSession().isSuper() )
        {
            if( getParent().isLockedWithoutToken() )
                throw new LockException("The parent is locked, so you cannot remove it.");
        
            if( !parentType.canRemoveItem(getName()) &&
                getParent().getState() != ItemState.REMOVED )
            {
                throw new ConstraintViolationException("Attempted to delete a mandatory child node:"+getInternalPath());
            }
        }
        
        m_session.remove( this );
        markModified(true);
        m_state = ItemState.REMOVED;

        LockImpl li = m_lockManager.getLock( getInternalPath() );
        
        if( li != null )
            m_lockManager.removeLock( li );
        
        //
        //  Remove version history
        //
        if( isNodeType( "mix:versionable" ) )
        {
            //
            //  Version histories cannot be removed unless you have a
            //  superuser session.
            //
            boolean isSuper = m_session.setSuper(true);
            try
            {
                getVersionHistory().remove();
            }
            finally
            {
                m_session.setSuper( isSuper );
            }
        }
        
        //
        //  Remove properties
        //
        for( PropertyIterator pit = getProperties(); pit.hasNext(); )
        {
            pit.nextProperty().remove();
        }
        
        //
        //  Remove children
        //
        for( NodeIterator ndi = getNodes(); ndi.hasNext(); )
        {
            Node nd = ndi.nextNode();
          
            nd.remove();
        }
        
        log.finer("Removed "+getPath());
    }

    /**
     *  This method is allowed to add to the property list.
     *  This is not public.
     *  
     *  @param property
     * @throws RepositoryException 
     * @throws ValueFormatException 
     */
    public void addChildProperty(PropertyImpl property) throws ValueFormatException, RepositoryException
    {
        //
        //  Add to the internal list.
        //
        m_session.m_provider.putProperty( this, property ); 
    }
    
    /**
     *  Assumes nothing, goes through the properties, makes sure all things are correct
     *
     *
     */
    public void sanitize() throws RepositoryException
    {
        // log.finest("Sanitizing node "+m_path);

        if( m_definition == null )
        {
            try
            {
                @SuppressWarnings("unused")
                PropertyImpl primarytype = getProperty( Q_JCR_PRIMARYTYPE );
            }
            catch( Exception e )
            {
                if( m_path.isRoot() )
                {
                    internalSetProperty( Q_JCR_PRIMARYTYPE, "nt:unstructured", PropertyType.NAME );
                }
                else
                {
                    internalSetProperty( Q_JCR_PRIMARYTYPE,
                                         assignChildType( m_path.getLastComponent() ).toString(),
                                         PropertyType.NAME );
                }
            }

            if( getParent() != null )
            {
                QNodeType mytype = getPrimaryQNodeType();

                m_definition = getParent().getPrimaryQNodeType().findNodeDefinition( mytype.getQName() );
            }
            else
            {
                // FIXME: Not correct
                m_definition = QNodeTypeManager.getInstance().findNodeDefinition( Q_NT_UNSTRUCTURED );
            }

            if( m_definition == null )
            {
                throw new RepositoryException("Cannot assign a node definition for "+m_path);
            }

        }

        // autoCreateProperties();

        QNodeType mytype = getPrimaryQNodeType();

        for( PropertyIterator i = getProperties(); i.hasNext(); )
        {
            PropertyImpl pi = (PropertyImpl)i.next();
            if( pi.getDefinition() == null )
            {
                QPropertyDefinition pd = mytype.findPropertyDefinition( pi.getQName(), false ); // FIXME: Really?

                pi.m_definition = pd.new Impl(m_session); // FIXME: Inoptimal
            }
        }
    }

    /**
     *  We consider nodes to be equal if they have the exact same path, and
     *  all the properties are equal too.
     */
    @Override
    public boolean equals(Object obj)
    {
        if( obj == this ) return true;
        
        if( obj instanceof NodeImpl )
        {
            NodeImpl ni = (NodeImpl) obj;
            
            if( !ni.getInternalPath().equals(getInternalPath()) )
                return false;
            
            if( !ni.m_session.getWorkspace().getName().equals(m_session.getWorkspace().getName()) ) return false;
            
            // All tests have succeeded
            return true;
        }
        
        return false;
    }
    
    @Override
    protected void preSave() throws RepositoryException
    {
        WorkspaceImpl ws = m_session.getWorkspace();
        
        autoCreateProperties();
        
        //
        //  Check that parent still exists
        //
        
        if( !m_path.isRoot() ) 
        {
            if( !ws.nodeExists(m_path.getParentPath()) )
            {
                throw new InvalidItemStateException("No parent available.");
            }
        }
        
        //
        //  Check if nobody has removed us if we were still supposed to exist.
        //
        
        if( m_state != ItemState.NEW )
        {
            if( !ws.nodeExists(m_path) )
            {
                throw new InvalidItemStateException("Looks like this Node has been removed by another session.");
            }
            
            try
            {
                String uuid = getUUID();
                
                NodeImpl currentNode = getSession().getNodeByUUID( uuid );
                
                if( !currentNode.getInternalPath().equals(getInternalPath()) )
                    throw new InvalidItemStateException("Page has been moved");
            }
            catch( UnsupportedRepositoryOperationException e ){} // Not referenceable, so it's okay
        }
        
        //
        //  Check mandatory properties
        //
        checkMandatoryProperties( getPrimaryQNodeType() );

        for( NodeType nt : getMixinNodeTypes() )
        {
            checkMandatoryProperties( ((QNodeType.Impl)nt).getQNodeType() );
        }
        
        //
        //  If this node is versionable, then make sure there is a VersionHistory as well.
        //
        
        if( hasMixinType("mix:versionable") )
        {
            VersionManager.createVersionHistory( this );
        }
    }

    /**
     *  Checks the mandatory properties for this Nodetype and throws a ConstraintViolationException
     *  if it's not existing.
     *  
     * @param nt
     * @throws RepositoryException 
     */
    private void checkMandatoryProperties(QNodeType nt) throws RepositoryException
    {
        for( QPropertyDefinition pd : nt.getQPropertyDefinitions() )
        {
            if( pd.isMandatory() && !hasProperty(pd.getQName()) )
            {
                throw new ConstraintViolationException("Node is missing property "+pd.getQName());
            }
        }
    }
    
    /*  ============================================================
     *  
     *  Mixins
     *    
     */
    

    public void addMixin(String mixinName)
                                          throws NoSuchNodeTypeException,
                                              VersionException,
                                              ConstraintViolationException,
                                              LockException,
                                              RepositoryException
    {
        ValueFactory vf = m_session.getValueFactory();

        NodeType mixin = getNodeTypeManager().getNodeType(mixinName);

        if( !mixin.isMixin() )
            throw new NoSuchNodeTypeException("Type "+mixinName+" is not a mixin type!");

        if( isLockedWithoutToken() )
            throw new LockException( "Node is locked, so cannot add new mixin types." );
        
        if( !isCheckedOut() )
            throw new VersionException( "Node is not checked out, so cannot add new mixin types.");

        boolean oldsuper = m_session.setSuper( true );

        try
        {
            Property p;
            
            p = getProperty(Q_JCR_MIXINTYPES);

            Value[] v = p.getValues();

            Value[] newval = new Value[v.length+1];

            for( int i = 0; i < v.length; i++ )
            {
                newval[i] = v[i];
            }

            newval[newval.length-1] = vf.createValue(mixinName,PropertyType.NAME);
        }
        catch( PathNotFoundException e )
        {
            Value[] values = new Value[] { vf.createValue(mixinName,PropertyType.NAME) };
            internalSetProperty( Q_JCR_MIXINTYPES, values, PropertyType.NAME );
        }
        finally
        {
            m_session.setSuper( oldsuper );
        }
        //autoCreateProperties();

        markModified(true);
    }

    public boolean canAddMixin(String mixinName) throws NoSuchNodeTypeException, RepositoryException
    {
        if( isLocked() ) return false;
            
        NodeType nt = getNodeTypeManager().getNodeType( mixinName );

        if( !nt.isMixin() )
        {
            throw new NoSuchNodeTypeException(mixinName+" is not a mixin type!");
        }
        
        if( hasMixinType(nt.getName()) )
        {
            return false;
        }

        if( !isCheckedOut() ) return false;
        
        // FIXME: This is a bit complicated and slow.

        if( mixinName.equals("mix:versionable") && 
            m_session.getRepository().getDescriptor(Repository.OPTION_VERSIONING_SUPPORTED).equals("false") ) return false;
        
        return true;
    }

    private boolean hasMixinType(String mixinType)
    {
        try
        {
            Property pi = getProperty( Q_JCR_MIXINTYPES );
        
            for( Value v : pi.getValues() )
            {
                String mixin = v.getString();
            
                if( mixin.equals(mixinType) ) return true;
            }
        }
        catch( RepositoryException e ) {}
        return false;
    }



    public NodeType[] getMixinNodeTypes() throws RepositoryException
    {
        ArrayList<NodeType> mixinTypes = new ArrayList<NodeType>();
        
        //
        //  If there are no mixin types, then let's just return an empty array.
        //
        try
        {
            Property p = getProperty( Q_JCR_MIXINTYPES );
        
            for( Value v : p.getValues() )
            {
                NodeType nt = m_session.getWorkspace().getNodeTypeManager().getNodeType( v.getString() );
            
                mixinTypes.add( nt );
            }
        }
        catch( RepositoryException e ) {}
        
        return mixinTypes.toArray( new NodeType[0] );
    }


    public void removeMixin(String mixinName)
                                             throws NoSuchNodeTypeException,
                                                 VersionException,
                                                 ConstraintViolationException,
                                                 LockException,
                                                 RepositoryException
    {
        if( isLocked() ) throw new LockException("Node locked, cannot remove mixin");
        
        if( !isCheckedOut() ) throw new VersionException("Node is not checked out.");
        
        Property mixinTypes = getProperty("jcr:mixinTypes");
        
        Value[] vals = mixinTypes.getValues();
        
        boolean found = false;
        
        for( int i = 0; i < vals.length; i++ )
        {
            if( vals[i].getString().equals(mixinName) )
            {
                vals[i] = null;
                found = true;
            }
        }
        
        if( found )
        {
            mixinTypes.setValue( vals );
        }
        else
        {
            throw new NoSuchNodeTypeException("No such mixin type to remove: "+mixinName);
        }
    }

    /*==============================================================================
     *
     *  Locking
     *  
     */
    
    LockManager m_lockManager = LockManager.getInstance(getSession().getWorkspace());
    
    public Lock lock(boolean isDeep, boolean isSessionScoped)
                                                             throws UnsupportedRepositoryOperationException,
                                                                 LockException,
                                                                 AccessDeniedException,
                                                                 InvalidItemStateException,
                                                                 RepositoryException
    {
        if( !hasMixinType("mix:lockable") )
            throw new UnsupportedRepositoryOperationException("This node is not lockable: "+m_path);
        
        if( m_state == ItemState.NEW )
            throw new LockException("This node has no persistent state");
        
        if( isModified() )
            throw new InvalidItemStateException("You may only lock Nodes which are not currently modified");
        
        if( isDeep && m_lockManager.hasChildLock(getInternalPath()) )
        {
            throw new LockException("A child of this node already holds a lock, so you cannot deep lock this node.");
        }
        
        if( isLocked() )
        {
            throw new LockException("This Node is already locked.");
        }
        
        LockImpl lock = new LockImpl( m_session,
                                      getInternalPath(), 
                                      isDeep,
                                      isSessionScoped );
      
        m_session.addLockToken( lock.getToken() );
        setProperty("jcr:lockOwner", lock.getLockOwner());
        setProperty("jcr:lockIsDeep", isDeep);
        
        save();
      
        m_lockManager.addLock( lock );
        
        return lock;
    }

    public void unlock() throws UnsupportedRepositoryOperationException,
        LockException,
        AccessDeniedException,
        InvalidItemStateException,
        RepositoryException
    {
        LockImpl lock = m_lockManager.getLock(m_path);
        
        if( lock == null )
            throw new LockException("This Node has not been locked.");
        
        if( lock.getLockToken() == null )
            throw new LockException("This Session does not own this Lock, so it cannot be unlocked.");
        
        if( isModified() )
            throw new InvalidItemStateException("This Node must not be modified prior to unlocking");
        
        Property p = getProperty("jcr:lockOwner");
        p.remove();
        p = getProperty("jcr:lockIsDeep");
        p.remove();
        
        m_session.removeLockToken( lock.getLockToken() );
        m_lockManager.removeLock( lock );
        
        save();
    }


    public LockImpl getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException
    {
        LockImpl li = m_lockManager.findLock( getInternalPath() );
        
        //
        //  Must return a clone of the Lock which is particular to this session.
        //
        
        if( li != null )
            li = new LockImpl( li, m_session );
        
        return li;
    }


    public boolean holdsLock() throws RepositoryException
    {
        LockImpl li = m_lockManager.getLock( getInternalPath() );

        return li != null;
    }
    

    public boolean isLocked() throws RepositoryException
    {
        LockImpl li = m_lockManager.findLock( getInternalPath() );

        return li != null;
    }

    /**
     *  Returns true, if this Node is locked (that is, it or it's parents are locked)
     *   but the Session which owns this Session does not hold a token to modify it.
     *  
     *  @return True, if you cannot modify this Node due to missing token.
     */
    protected boolean isLockedWithoutToken()
    {
        try
        {
            if( !getInternalPath().isRoot() )
            {
                LockImpl li = getLock();
            
                if( li != null && li.getLockToken() == null )
                {
                    return true;
                }
            }
        
        }
        catch( RepositoryException e )
        {
            log.warning("Don't quite know what happened "+e.getMessage());
        }
        
        return false;
    }

    /* ====================================================
     * 
     *  Versioning
     * 
     */
    public boolean isCheckedOut() throws RepositoryException
    {
        try
        {
            PropertyImpl p = getProperty(Q_JCR_ISCHECKEDOUT);
            
            return p.getBoolean();
        }
        catch(PathNotFoundException e)
        {
            // Fine; no property exists.
        }
        
        //
        //  Check if this is a versionable node in the first place.
        //
        if( isNodeType( "mix:versionable" ) && !isNew() )
            return false;
        
        return true;
    }


    public void cancelMerge(Version version)
                                            throws VersionException,
                                                InvalidItemStateException,
                                                UnsupportedRepositoryOperationException,
                                                RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException("Node.cancelMerge()");

    }

    public VersionImpl checkin()
                            throws VersionException,
                                UnsupportedRepositoryOperationException,
                                InvalidItemStateException,
                                LockException,
                                RepositoryException
    {
        if( !hasMixinType("mix:versionable") )
        {
            throw new UnsupportedRepositoryOperationException("Node is not mix:versionable (8.2.5)");            
        }
        
        if( !isCheckedOut() )
        {
            return getBaseVersion();
        }
        
        if( isModified() )
        {
            throw new InvalidItemStateException("Node has unsaved changes (8.2.5)");
        }
        
        if( hasProperty("jcr:mergeFailed") )
        {
            throw new VersionException("Node has failed merges (8.2.5)");
        }
        
        //
        //  Phew!  Preconditions have been checked.  Now, let's get to real business.
        //
        
        boolean isSuper = getSession().setSuper(true);
             
        try
        {
            VersionHistoryImpl vh = getVersionHistory();
        
            int version = 0;
            if( !getBaseVersion().getName().equals("jcr:rootVersion") )
                version = Integer.parseInt( getBaseVersion().getName() );
        
            VersionImpl v = (VersionImpl) vh.addNode( Integer.toString( ++version ), "nt:version" );
        
            if(!hasProperty("nt:versionHistory"))
                setProperty( "nt:versionHistory", vh );
        
            v.setProperty( JCR_PREDECESSORS, getProperty(JCR_PREDECESSORS).getValues() );
            v.addMixin( "mix:referenceable" );
            v.setProperty( "jcr:uuid", UUID.randomUUID().toString() );
            v.setProperty( JCR_SUCCESSORS, new Value[0], PropertyType.REFERENCE );
            
            setProperty( JCR_PREDECESSORS, new Value[0], PropertyType.REFERENCE );
        
            PropertyImpl preds = v.getProperty(JCR_PREDECESSORS);

            for( Value val : preds.getValues() )
            {
                String uuid = val.getString();
                Node pred = m_session.getNodeByUUID(uuid);
         
                Value[] s = pred.getProperty( JCR_SUCCESSORS ).getValues();
            
                List<Value> successorList = new ArrayList<Value>();
                
                successorList.addAll( Arrays.asList(s) );
                successorList.add( m_session.getValueFactory().createValue(v) );

                pred.setProperty( JCR_SUCCESSORS, successorList.toArray(s) );
            }
        
            setProperty( "jcr:baseVersion", v );
            setProperty( "jcr:isCheckedOut", false );

            //
            //  Store the contents into the frozen node of the Version node. 
            //
            NodeImpl fn = v.addNode("jcr:frozenNode","nt:frozenNode");

            for( PropertyIteratorImpl pi = getProperties(); pi.hasNext(); )
            {
                PropertyImpl p = pi.nextProperty();
                
                if( p.getQName().equals( JCRConstants.Q_JCR_PRIMARYTYPE ) )
                {
                    fn.setProperty( "jcr:frozenPrimaryType", p.getValue() );
                }
                else if( p.getQName().equals( JCRConstants.Q_JCR_UUID ) )
                {
                    fn.setProperty( "jcr:frozenUuid", p.getValue() );
                }
                else if( p.getQName().equals( JCRConstants.Q_JCR_MIXINTYPES ) )
                {
                    fn.setProperty( "jcr:frozenMixinTypes", p.getValues() );
                }
                else if( p.getDefinition().getOnParentVersion() == OnParentVersionAction.COPY )
                {
                    // FIXME: SHould probably deal with the others as well.
                    if( p.getDefinition().isMultiple() )
                    {
                        fn.setProperty( p.getName(), p.getValues() );
                    }
                    else
                    {
                        fn.setProperty( p.getName(), p.getValue() );
                    }
                }
            }
            
            vh.save();
            // FIXME: Here.
            
            return v;
        }
        finally
        {
            getSession().setSuper( isSuper );
        }
    }

    public void checkout() throws UnsupportedRepositoryOperationException, LockException, RepositoryException
    {
        if( isCheckedOut() ) return; // Nothing happens
        
        if( !isNodeType( "mix:versionable" ) )
            throw new UnsupportedRepositoryOperationException("Not versionable (8.2.6)");

        boolean isSuper = getSession().setSuper( true );
        
        try
        {
            setProperty( "jcr:isCheckedOut", true );
        
            //
            //  We don't support multiple predecessors, so this is okay, I think.
            //
            setProperty( JCR_PREDECESSORS,
                         new ValueImpl[] { getSession().getValueFactory().createValue( getBaseVersion() ) } );
        
            setProperty( JCR_SUCCESSORS, 
                         new ValueImpl[0] );
            //
            //  Persist immediately.
            //
            save();
        }
        finally
        {
            getSession().setSuper( isSuper );
        }
    }

    public void doneMerge(Version version)
                                          throws VersionException,
                                              InvalidItemStateException,
                                              UnsupportedRepositoryOperationException,
                                              RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException("Node.doneMerge()");

    }

    public VersionImpl getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException
    {
        String bvUuid = getProperty( Q_JCR_BASEVERSION ).getString();
        
        return (VersionImpl) getSession().getNodeByUUID( bvUuid );
    }

    public void restore(String versionName, boolean removeExisting)
        throws VersionException,
            ItemExistsException,
            UnsupportedRepositoryOperationException,
            LockException,
            InvalidItemStateException,
            RepositoryException
    {
//      TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();

    }

    public void restore(Version version, boolean removeExisting)
        throws VersionException,
            ItemExistsException,
            UnsupportedRepositoryOperationException,
            LockException,
            RepositoryException
    {
//      TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();
        
    }

    public void restore(Version version, String relPath, boolean removeExisting)
                 throws PathNotFoundException,
                     ItemExistsException,
                     VersionException,
                     ConstraintViolationException,
                     UnsupportedRepositoryOperationException,
                     LockException,
                     InvalidItemStateException,
                     RepositoryException
    {
//      TODO Auto-generated method stub

        throw new UnsupportedRepositoryOperationException();
    }

    public void restoreByLabel(String versionLabel, boolean removeExisting)
            throws VersionException,
                ItemExistsException,
                UnsupportedRepositoryOperationException,
                LockException,
                InvalidItemStateException,
                RepositoryException
    {
//      TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();

    }



    public NodeIterator merge(String srcWorkspace, boolean bestEffort)
                                                                  throws NoSuchWorkspaceException,
                                                                      AccessDeniedException,
                                                                      MergeException,
                                                                      LockException,
                                                                      InvalidItemStateException,
                                                                      RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();
    }

}
