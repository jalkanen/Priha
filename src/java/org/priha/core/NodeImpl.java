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

import org.priha.RepositoryManager;
import org.priha.core.locks.LockManager;
import org.priha.core.locks.QLock;
import org.priha.core.values.ValueFactoryImpl;
import org.priha.core.values.ValueImpl;
import org.priha.nodetype.QNodeDefinition;
import org.priha.nodetype.QNodeType;
import org.priha.nodetype.QNodeTypeManager;
import org.priha.nodetype.QPropertyDefinition;
import org.priha.path.InvalidPathException;
import org.priha.path.Path;
import org.priha.path.PathFactory;
import org.priha.util.LazyNodeIteratorImpl;
import org.priha.util.PropertyIteratorImpl;
import org.priha.util.QName;
import org.priha.util.TextUtil;
import org.priha.version.VersionHistoryImpl;
import org.priha.version.VersionImpl;
import org.priha.version.VersionManager;

/**
 *  Implements a Node.  This is one of the most heavy classes in Priha, with a lot
 *  of stuff happening.  The NodeImpl class does some basic caching for some state
 *  objects, so keeping references can be faster in some cases.
 */
public class NodeImpl extends ItemImpl implements Node, Comparable<Node>
{
    private static final String JCR_PREDECESSORS = "jcr:predecessors";
    private static final String JCR_SUCCESSORS   = "jcr:successors";

    /** 
     *  A compile-time flag for allowing/disallowing Same Name Sibling support. 
     *  addNode() will throw an exception if you disallow these.  This is sometimes
     *  useful for debugging. 
     */
    private static final boolean ALLOW_SNS = true;
    
    private QNodeDefinition      m_definition;
    
    private QNodeType            m_primaryType;

    protected String             m_cachedUUID;
    
    private ArrayList<Path>      m_childOrder;
    
    static Logger log = Logger.getLogger( NodeImpl.class.getName() );

    /** 
     *  Do not change.  This is the fixed UUID for the root node for each workspace. If you
     *  change this, horrible things may happen.
     *  
     *  The UUID is a standard Type 3 UUID, generated from the byte array [ 0x00 ].
     */
    private static final UUID ROOT_UUID      = UUID.nameUUIDFromBytes( new byte[] { 0x00 } );
        
    protected NodeImpl( SessionImpl session, Path path, QNodeType primaryType, QNodeDefinition nDef, boolean populateDefaults )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        super( session, path );

        m_primaryType = primaryType;
        m_definition  = nDef;
        
        if( populateDefaults )
        {
            enterState(ItemState.NEW);
            PropertyImpl pt =internalSetProperty( Q_JCR_PRIMARYTYPE, 
                                                  session.fromQName( m_primaryType.getQName() ), // FIXME: Not very efficient 
                                                  PropertyType.NAME );
            pt.enterState( ItemState.NEW );
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
        if( relPath.endsWith("]") && !m_session.isSuper() )
        {
            throw new RepositoryException("Cannot add an indexed entry unless using a superSession");
        }
        
        Path absPath = getInternalPath().resolve(m_session,relPath);

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

            if( parent.getState() == ItemState.REMOVED )
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
            
            // Residual definitions
            if( assignedNodeDef.getQName().toString().equals("*") )
            {
                assignedNodeDef = assignedType.findNodeDefinition( absPath.getLastComponent() );
            }
            
            //
            //  Check for same name siblings.  If they are allowed, and they already
            //  exist, we figure out the number of other existing nodes and modify the absPath
            //  accordingly for the new path.
            //
            if( m_session.itemExists(absPath) )
            {

                NodeDefinition nd = parent.getDefinition();

                if( !nd.allowsSameNameSiblings() )
                {
                    throw new ItemExistsException("Node "+absPath+" already exists, and the parent node does not allow same name siblings!");
                }
                
                NodeIterator iter = parent.getNodes( absPath.getLastComponent().toString( m_session ) );
                
                int newPos = ((int)iter.getSize())+1;
                
                absPath = new Path( absPath.getParentPath(), 
                                    new Path.Component(absPath.getLastComponent(),newPos) );
                
                if( !ALLOW_SNS ) throw new RepositoryException("TURNED OFF FOR NOW");
            }

            //
            //  Check if parent allows adding this.
            //
            QNodeType parentnt = parent.getPrimaryQNodeType();
            
            if( !parentnt.canAddChildNode(absPath.getLastComponent(), 
                                          primaryNodeTypeName != null ? m_session.toQName(primaryNodeTypeName) : null ) )
            {                    
                throw new ConstraintViolationException("Parent node does not allow adding nodes of name "+absPath.getLastComponent());
            }

            //
            //  Node type and definition are now okay, so we'll create the node
            //  and add it to our session.
            //
            ni = m_session.createNode(absPath, assignedType, assignedNodeDef,true);

            ni.sanitize();

            ni.enterState( ItemState.NEW );
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

    public QNodeDefinition getQDefinition()
    {
        return m_definition;
    }
    
    public int getIndex() throws RepositoryException
    {
        return getInternalPath().getLastComponent().getIndex();
    }

    protected List<Path> getChildOrder()
    {
        if( m_childOrder != null )
            return Collections.unmodifiableList(m_childOrder);
        
        return null;
    }
    
    protected void setChildOrder(List<Path> list)
    {
        if( list == null ) m_childOrder = null;
        else
        {
            m_childOrder = new ArrayList<Path>();
            m_childOrder.addAll(list);
        }
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
        return getNode( getInternalPath().resolve(m_session,relPath) );
    }
    
    public NodeImpl getNode(QName name) throws PathNotFoundException, RepositoryException
    {
        return getNode( getInternalPath().resolve(name) );
    }

    /**
     *  Returns the children of this Node sorted according to the child order.  If there
     *  is no child order, does nothing.
     *  
     *  @param list
     *  @return
     *  @throws PathNotFoundException
     *  @throws RepositoryException
     */
    private List<Path> sortChildren(List<Path> list) throws PathNotFoundException, RepositoryException
    {
        if( m_childOrder != null )
        {
            list.removeAll(m_childOrder);
            
            ArrayList<Path> newOrder = new ArrayList<Path>();
            newOrder.addAll(m_childOrder);
            newOrder.addAll(list);
            list = newOrder;
        }
        
        return list;
    }
    
    /**
     *  Returns a lazy iterator over the children of this Node.
     */
    public NodeIterator getNodes() throws RepositoryException
    {
        List<Path> children = m_session.listNodes( getInternalPath() );

        children = sortChildren(children);
        
        return new LazyNodeIteratorImpl(m_session,children);
    }

    public NodeIterator getNodes(String namePattern) throws RepositoryException
    {
        Pattern p = TextUtil.parseJCRPattern(namePattern);

        ArrayList<Path> matchedpaths = new ArrayList<Path>();

        List<Path> children = m_session.listNodes( getInternalPath() );

        children = sortChildren(children);

        for( Path path : children )
        {
            // This crummy code turns a Path.Component to a QName, i.e. drops
            // the index, if it exists.
            String s = m_session.fromQName( new QName(path.getLastComponent().getNamespaceURI(),
                                                      path.getLastComponent().getLocalPart()) );
            
            Matcher match = p.matcher( s );

            if( match.matches() )
            {
                matchedpaths.add( path );
            }
        }

        return new LazyNodeIteratorImpl(m_session,matchedpaths);
    }

    public ItemImpl getPrimaryItem() throws ItemNotFoundException, RepositoryException
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
        ItemImpl ii = m_session.m_provider.getItem( getInternalPath().resolve(m_session,name) );
        
        if( ii.isNode() ) throw new ItemNotFoundException("Found a Node, not a Property");
        
        return (PropertyImpl) ii;
    }

    public PropertyImpl getProperty( QName propName ) throws PathNotFoundException, RepositoryException
    {
        Path abspath = getInternalPath().resolve(propName);
        
        ItemImpl item = m_session.getItem(abspath);

        if( item != null && !item.isNode() && item.getState() != ItemState.REMOVED && item.getState() != ItemState.MOVED )
        {
            return (PropertyImpl) item;
        }

        throw new PathNotFoundException( abspath.toString() );        
    }
    
    public PropertyImpl getProperty(String relPath) throws PathNotFoundException, RepositoryException
    {
        Path abspath = getInternalPath().resolve(m_session,relPath);

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
//            VersionManager.createVersionHistory( this );
        }

        for( QPropertyDefinition pd : nt.getQPropertyDefinitions() )
        {
            if( pd.isAutoCreated() && !hasProperty(pd.getQName()) )
            {
                log.finest("Autocreating property "+pd.getQName());

                Path p = getInternalPath().resolve(pd.getQName());

                PropertyImpl pi = new PropertyImpl(m_session,
                                                   p,
                                                   pd);

                // FIXME: Add default value generation

                if( Q_JCR_UUID.equals(pi.getQName()) )
                {
                    UUID uuid;
                    if( getInternalPath().isRoot() )
                        uuid = ROOT_UUID;
                    else
                        uuid = UUID.randomUUID();
                    
                    pi.loadValue( vfi.createValue( uuid.toString() ) );
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
                    throw new UnsupportedRepositoryOperationException("Automatic setting of property "+pi.getPath()+ " is not supported.");
                }
                pi.enterState( ItemState.NEW );
                //addChildProperty( pi );
            }
        }
    }

    public PropertyIterator getReferences() throws RepositoryException
    {
        List<PropertyImpl> references = m_session.getReferences( getUUID() );

        return new PropertyIteratorImpl(references);
    }

    /**
     *  Returns the UUID for this node.  Utilizes an internal cache for the UUID,
     *  so is fast after the first call.
     */
    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException
    {
        if( m_cachedUUID != null ) return m_cachedUUID;
        
        if( isNodeType("mix:referenceable") )
        {
            try
            {
                Property uuid = getProperty(JCR_UUID);

                String u = uuid.getValue().getString();
                m_cachedUUID = u;
                return u;
            }
            catch( PathNotFoundException e )
            {
                // Fine, let's just fall through, and end up throwing an exception
            }
        }

        throw new UnsupportedRepositoryOperationException("No UUID defined for "+getPath());
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
        Path absPath = getInternalPath().resolve(m_session,relPath);
        return m_session.hasNode(absPath);
    }

    /**
     *  Returns true, if this Node has a child with the given QName.
     *  
     *  @param name A QName.
     *  @return True, if there is a child by this name.
     *  @throws RepositoryException
     */
    public boolean hasNode(QName name) throws RepositoryException
    {
        Path absPath = getInternalPath().resolve(name);
        return m_session.hasNode(absPath);
    }

    public boolean hasNodes() throws RepositoryException
    {
        // FIXME: Slow.
        return getNodes().getSize() > 0;
    }

    public boolean hasProperties() throws RepositoryException
    {
        // FIXME: Slow.
        return getProperties().getSize() > 0;
    }

    public boolean hasProperty( QName propName ) throws RepositoryException
    {
        Path abspath = getInternalPath().resolve(propName);
        return m_session.hasProperty(abspath);
    }
    
    public boolean hasProperty(String relPath) throws RepositoryException
    {
        Path abspath = getInternalPath().resolve(m_session,relPath);
        
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
        //
        //  The usual sanity checks.
        //
        if( !getPrimaryNodeType().hasOrderableChildNodes() )
        {
            throw new UnsupportedRepositoryOperationException("This Node does not support orderable child nodes.");
        }
        
        if( !isCheckedOut() )
            throw new VersionException("Node not checked out");
        
        if( isLockedWithoutToken() )
            throw new LockException("Node is locked");
        
        if( srcChildRelPath.equals( destChildRelPath ) ) return;
        
        if( srcChildRelPath.indexOf( '/' ) != -1 || (destChildRelPath != null && destChildRelPath.indexOf( '/' ) != -1 )) 
            throw new ConstraintViolationException("Child path depth must be 1");
        
        if( !hasNode(srcChildRelPath) ) throw new ItemNotFoundException("Source child does not exist");
        if( destChildRelPath != null && !hasNode(destChildRelPath) ) throw new ItemNotFoundException("Dest child does not exist");
        
        //
        //  Get the current order
        //
        Path srcPath = getInternalPath().resolve(m_session,srcChildRelPath);
        Path dstPath = destChildRelPath != null ? getInternalPath().resolve(m_session,destChildRelPath) : null;
        
        List<Path> children;
        
        if( m_childOrder != null ) children = m_childOrder;
        else children = m_session.listNodes( getInternalPath() );
        
        ArrayList<Path> newOrder = new ArrayList<Path>();
        
        //
        //  Figure out the new order
        //
        newOrder.addAll(children);
        int srcIndex = newOrder.indexOf( srcPath );
        int dstIndex = newOrder.indexOf( dstPath );

        if( srcIndex == -1 ) throw new ItemNotFoundException("Cannot locate source child, WTF?");
          
        //
        //  Make sure locks are also transferred.
        //
        QLock lock = m_lockManager.getLock( srcPath );
        if( lock != null )
        {
            m_lockManager.removeLock( lock );
        }
        

        //
        //  Now the not-so-fun thing; we must make sure that also any SNS's are rearranged appropriately.
        //
        //  Case 1:  A[1], A[2], A[3] => orderBefore("A[3]","A[1]") => A[3], A[1], A[2].
        //  Case 2:  A[1], A[2], A[3] => orderBefore("A[1]","A[3]") => A[2], A[1], A[3].
        //

        boolean isSuper = m_session.setSuper(true);
        
        try
        {
            if( srcChildRelPath.indexOf('[') != -1 || hasNode(srcChildRelPath+"[2]" ) )
            {
                // Yes, we are moving a SNS so this needs reordering.
                Path tmpPath = getInternalPath().resolve( m_session, "priha:tmpmove" );
                
                // First, store the old version
                m_session.internalMove( srcPath, 
                                        tmpPath, 
                                        false );

                String childName = srcChildRelPath.replaceAll( "\\[\\d+\\]", "" );
                
                if( dstPath == null ) 
                {
                    NodeIterator ni = getNodes(childName);
                    dstPath = getInternalPath().resolve( m_session,
                                                         childName+"["+ni.getSize()+"]" );
                }
                                
                int dir;
                int startIdx;
                int endIdx;
                if( srcPath.getLastComponent().getIndex() < dstPath.getLastComponent().getIndex() )
                {
                    // Moving stuff forwards
                    dir = -1;
                    startIdx = srcPath.getLastComponent().getIndex()+1;
                    endIdx   = dstPath.getLastComponent().getIndex()+1;
                }
                else
                {
                    dir = 1;
                    startIdx = srcPath.getLastComponent().getIndex()-1;
                    endIdx   = dstPath.getLastComponent().getIndex()-1;
                }
                
                for( int i = startIdx; i != endIdx; i -= dir )
                {
                    String oldName = childName + "[" + i + "]";
                    String newName = childName + "[" + (i + dir) + "]";
                    
                    System.out.println("Reordering SNS : "+oldName+" to "+newName);
                        
                    Path path1   = getInternalPath().resolve(m_session,oldName);
//                  Path tmppath = getInternalPath().resolve(m_session,"priha:tmpmove");
                    Path newPath = getInternalPath().resolve(m_session,newName);
                        
                    m_session.internalMove( path1, newPath, false );
                    
//                    int  oldI = newOrder.indexOf(newPath);
                  
//                    Path oldP = newOrder.set(newOrder.indexOf(path1),newPath);
                    
//                    newOrder.set(oldI, oldP);
                    
                }
                
                m_session.internalMove( tmpPath, 
                                        dstPath,
                                        false );
            }
            else
            {
                //
                //  Removal of the node may cause a jump in the indices, so we need to subtract one sometimes.
                //
                Path p = newOrder.remove(srcIndex);
                if( dstIndex != -1 ) 
                    newOrder.add(dstIndex - (dstIndex > srcIndex ? 1 : 0),p);
                else 
                    newOrder.add(p);

            }
        }
        finally
        {
            m_session.setSuper(isSuper);
        }
        
        //
        //  Lock back up
        //
        
        if( lock != null )
        {
            m_lockManager.moveLock( lock, dstPath );
        }
        
        //
        //  Finish.
        //
        enterState( ItemState.UPDATED );
        m_childOrder = newOrder;
    }


    private PropertyImpl prepareProperty( String name, Object value ) throws PathNotFoundException, RepositoryException
    {
        return prepareProperty( m_session.toQName(name), value );
    }
    
    /**
     *  Finds a property and checks if we're supposed to remove it or not.  It also creates
     *  the property if it does not exist.  The property value itself is empty until the
     *  property is loaded with loadValue()
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
                throw new ConstraintViolationException( getInternalPath()+" has already been assigned a primary type!");
            }

            //  We know where this belongs to.
            QNodeType gnt = QNodeTypeManager.getInstance().getNodeType( Q_NT_BASE );

            QPropertyDefinition primaryDef = gnt.findPropertyDefinition( JCRConstants.Q_JCR_PRIMARYTYPE,
                                                                         false);

            prop = new PropertyImpl( m_session,
                                     getInternalPath().resolve(name),
                                     primaryDef );

//            prop.m_state = ItemState.NEW;
//            addChildProperty( prop ); //  Again, a special case.  First add the property to the lists.
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
            //
            //  Handle new property
            //
            Path propertypath = getInternalPath().resolve(name);

            Path p = propertypath.getParentPath();

            try
            {
                NodeImpl parentNode = (NodeImpl) m_session.getItem(p);

                boolean ismultiple = value instanceof Object[];

                QNodeType parentType = parentNode.getPrimaryQNodeType();
            
                QPropertyDefinition pd = parentType.findPropertyDefinition(name,ismultiple);
            
                if( pd == null ) 
                {
                    //
                    //  Let's add one of our definitions
                    //
                    if( name.getNamespaceURI().equals(RepositoryManager.NS_PRIHA) )
                    {
                        pd = QPropertyDefinition.PRIHA_INTERNAL;
                    }
                    else
                    {
                        throw new RepositoryException("No propertydefinition found for "+parentType+" and "+name);
                    }
                }
            
                prop = new PropertyImpl( m_session, propertypath, pd );
                prop.m_isNew = true;
//                prop.setState( ItemState.NEW );
            }
            catch( PathNotFoundException e )
            {
                throw new InvalidItemStateException("Parent not located; the item is in indeterminate state.");
            }
        }
        else
        {
//            prop.setState( ItemState.UPDATED );
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
        prop.enterState( ItemState.REMOVED );
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

    PropertyImpl internalSetProperty(QName name, Value[] values, int type) throws PathNotFoundException, RepositoryException
    {
        PropertyImpl p = prepareProperty( name, values );

        p.loadValue( values, type );
        
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

        p.setValue( values, type );

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

        return p;
    }

    private PropertyImpl internalSetProperty(QName name, String value, int type) throws PathNotFoundException, RepositoryException
    {
        PropertyImpl prop = prepareProperty(name,value);
        
        prop.loadValue( m_session.getValueFactory().createValue(value,type) );
        
        return prop;
    }

    /**
     *  Tag a Node with the given transient property.  Priha uses this internally
     *  to store metadata (or a property) which is never saved. Transient properties
     *  are never saved.
     *  
     *  @see PropertyImpl#isTransient()
     *  
     *  @param name
     *  @throws ValueFormatException
     *  @throws VersionException
     *  @throws LockException
     *  @throws ConstraintViolationException
     *  @throws RepositoryException
     */
    public void tag(String name) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        PropertyImpl p = setProperty(name, Boolean.TRUE);
        p.setTransient(true);
    }
    
    /**
     *  Tags a Node with a given transient property with a String value. In practice,
     *  this sets a String property and marks it transient (i.e. never saved).
     *  
     *  @see PropertyImpl#isTransient()
     *  @param name Name of the property to set
     *  @param value Value for the property
     *  @throws ValueFormatException
     *  @throws VersionException
     *  @throws LockException
     *  @throws ConstraintViolationException
     *  @throws RepositoryException
     */
    public void tag(String name, String value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        PropertyImpl p = setProperty(name, value);
        p.setTransient(true);        
    }
    
    /**
     *  Returns true, if this Node has the named tag (i.e. it has a transient
     *  Property by this name).
     *  
     *  @param name Tag to check for
     *  @return True, if the tag exists. False otherwise.
     *  @throws RepositoryException If the Repository is b0rken.
     *  @see PropertyImpl#isTransient()
     */
    public boolean hasTag(String name) throws RepositoryException
    {
        try
        {
            if( hasProperty( name ) )
            {
                return getProperty(name).isTransient();
            }
        }
        catch( PathNotFoundException e )
        {}
        
        return false;
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
        if( m_session.hasPendingChanges() )
            throw new InvalidItemStateException("A Session must not have unsaved changes prior to calling update()");
        
        SessionImpl srcSession = m_session.getRepository().login(srcWorkspaceName);
        
        // FIXME: This should really reuse the Session somehow.  Now we do two logins, one here
        //        and one in getCorrespondingNodePath()
        try
        {
            String correspondingPath = getCorrespondingNodePath( srcWorkspaceName );

            String destPath = getParent().getPath()+"/"+getName();
            
            remove();
            m_session.save();
            
            m_session.getWorkspace().copy( srcSession, correspondingPath, destPath, true );
            m_session.save();
        }
        catch( ItemNotFoundException e )
        {
            // Return quietly; nothing happens in this case.
        }
        finally
        {
            srcSession.logout();
        }
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
        if( getState() == ItemState.NEW )
            throw new InvalidItemStateException("Cannot call save on newly added node "+getInternalPath());

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
        remove(false);
    }
    
    /**
     *  If isRemoving = true, will remove subnodes without question.
     */
    private void remove(boolean isRemoving) throws VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        if( getState() == ItemState.REMOVED )
        {
//            System.out.println(getPath()+" has already been removed");
            return; // Die nicely
        }
        
        Path path = getInternalPath();
        
        if( !m_session.m_provider.nodeExistsInRepository( path ) && m_session.m_provider.m_changedItems.get(path) == null )
        {
            m_session.m_provider.m_changedItems.dump();
            throw new InvalidItemStateException("Item has already been removed by another Session "+getPath());
        }
        
        if( path.isRoot() || path.equals( JCRConstants.Q_JCR_SYSTEM_PATH ) )
        {
            return; // Refuse to remove
        }
        
        NodeImpl parent = getParent();
        NodeType parentType = parent.getPrimaryNodeType();
    
        if( !getSession().isSuper() )
        {
            // Anything from under /jcr:system cannot be deleted by the user
            if( JCRConstants.Q_JCR_SYSTEM_PATH.isParentOf( path )) 
                return;
            
            if( getParent().isLockedWithoutToken() )
                throw new LockException("The parent is locked, so you cannot remove it.");
        
            if( !parentType.canRemoveItem(getName()) &&
                getParent().getState() != ItemState.REMOVED && !isRemoving )
            {
                throw new ConstraintViolationException("Attempted to delete a mandatory child node:"+getInternalPath());
            }
        }
        
        QLock li = m_lockManager.getLock( path );
        
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
            catch( UnsupportedRepositoryOperationException e )
            {
                // This may happen if you've just created a Node, then hit remove() on it immediately, since
                // in that case, we do not have an UUID yet.
            }
            finally
            {
                m_session.setSuper( isSuper );
            }
        }
        
        //
        //  Removal happens in a depth-first manner.
        //
        
        //
        //  Remove children.  We do this in a reverse order in order
        //  not to force annoying moves for same-name siblings.  It's just faster.
        //
        LazyNodeIteratorImpl ndi = (LazyNodeIteratorImpl) getNodes();
        ndi.skip( ndi.getSize() );
        
        while( ndi.hasPrevious() )
        {
            NodeImpl nd = ndi.previousNode();

//            System.out.println("REMOVING "+nd.getPath());
            
            nd.remove(true);
        }

        
        boolean isSuper = m_session.setSuper(true);

        //
        //  Remove properties
        //
        for( PropertyIterator pit = getProperties(); pit.hasNext(); )
        {
            pit.nextProperty().remove();
        }
        
        // This is a hack which just resets the state and then adds this to the remove queue. FIXME!
//        m_state = ItemState.UPDATED;
        enterState( ItemState.REMOVED );

        //
        //  Fix same name siblings, but don't bother if the parent is already removed.
        //  Again, we go for the super session.
        // 
        
        try
        {
            int myIndex   = getInternalPath().getLastComponent().getIndex();

            if( !isRemoving )
            {
                for( NodeIterator ni = getParent().getNodes( getName() ); ni.hasNext(); )
                {
                    NodeImpl n = (NodeImpl)ni.nextNode();
            
                    int siblingIndex = n.getInternalPath().getLastComponent().getIndex();
            
                    if( myIndex >= siblingIndex ) continue;
            
                    Path destPath = new Path(n.getParent().getInternalPath(),
                                             new Path.Component(getQName(),siblingIndex-1) );
            
//                    System.out.println("Moving "+n+" to "+destPath);
//                    m_session.m_provider.m_changedItems.dump();
                    getSession().move( n.getInternalPath().toString( m_session ), 
                                       destPath.toString( m_session ) );
                }
            }
        }
        finally
        {
            m_session.setSuper(isSuper);
        }
        
        
        
        log.finer("Removed "+getPath());
    }

    /**
     *  Locates a PropertyDefinition for the given property name from the array of
     *  the mixintypes and the primary type for this Node.
     *  
     *  @param propertyName The QName of the property to look for
     *  @param multiple Is this a multiproperty or a single property?
     *  @return A valid PropertyDefinition, or null, if it cannot be located.
     *  @throws RepositoryException If mixin node types cannot be determined.
     */
    public QPropertyDefinition findPropertyDefinition(QName propertyName,boolean multiple) throws RepositoryException
    {
        QPropertyDefinition qp;
        
        //
        //  Mixin types can override the primary type; especially since the
        //  primary type can contain a wildcard.
        //
        for( NodeType nt : getMixinNodeTypes() )
        {
            QNodeType qnt = ((QNodeType.Impl)nt).getQNodeType();
            
            qp = qnt.findPropertyDefinition(propertyName, multiple);
            
            if( qp != null ) return qp;
        }
        
        return getPrimaryQNodeType().findPropertyDefinition(propertyName, multiple);
    }
    
    /**
     *  Assumes nothing, goes through the properties, makes sure all things are correct.
     */
    public void sanitize() throws RepositoryException
    {
        // log.finest("Sanitizing node "+getInternalPath());

        if( m_definition == null )
        {
            try
            {
                @SuppressWarnings("unused")
                PropertyImpl primarytype = getProperty( Q_JCR_PRIMARYTYPE );
            }
            catch( Exception e )
            {
                if( getInternalPath().isRoot() )
                {
                    PropertyImpl pix = internalSetProperty( Q_JCR_PRIMARYTYPE, "nt:unstructured", PropertyType.NAME );
                    pix.enterState(ItemState.NEW);
                }
                else
                {
                    PropertyImpl pix = internalSetProperty( Q_JCR_PRIMARYTYPE,
                                                            assignChildType( getInternalPath().getLastComponent() ).toString(),
                                                            PropertyType.NAME );
                    pix.enterState(ItemState.NEW);
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
                throw new RepositoryException("Cannot assign a node definition for "+getInternalPath());
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
        super.preSave();
    }

    /**
     *  Checks the mandatory properties for this Nodetype and throws a ConstraintViolationException
     *  if it's not existing.
     *  
     * @param nt
     * @throws RepositoryException 
     */
    void checkMandatoryProperties(QNodeType nt) throws RepositoryException
    {
        for( QPropertyDefinition pd : nt.getQPropertyDefinitions() )
        {
            if( pd.isMandatory() && !hasProperty(pd.getQName()) )
            {
                throw new ConstraintViolationException("Node "+getInternalPath()+" is missing property "+pd.getQName());
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
            PropertyImpl pi = internalSetProperty( Q_JCR_MIXINTYPES, newval, PropertyType.NAME );
            pi.enterState(ItemState.UPDATED);
        }
        catch( PathNotFoundException e )
        {
            Value[] values = new Value[] { vf.createValue(mixinName,PropertyType.NAME) };
            PropertyImpl pi = internalSetProperty( Q_JCR_MIXINTYPES, values, PropertyType.NAME );
            pi.enterState(ItemState.NEW);
        }
        finally
        {
            m_session.setSuper( oldsuper );
        }
        //autoCreateProperties();
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

    boolean hasMixinType(String mixinType)
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
            throw new UnsupportedRepositoryOperationException("This node is not lockable: "+getInternalPath());
        
        if( getState() == ItemState.NEW )
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
        
        QLock lock = new QLock( this,
                                isDeep,
                                isSessionScoped );
      
        m_session.addLockToken( lock.getToken() );
        setProperty("jcr:lockOwner", lock.getLockOwner());
        setProperty("jcr:lockIsDeep", isDeep);
        
        save();
      
        m_lockManager.addLock( lock );
        
        return lock.getLockInstance(m_session);
    }

    public void unlock() throws UnsupportedRepositoryOperationException,
        LockException,
        AccessDeniedException,
        InvalidItemStateException,
        RepositoryException
    {
        QLock lock = m_lockManager.getLock( getInternalPath() );
        
        if( lock == null )
            throw new LockException("This Node has not been locked.");
        
        if( lock.getLockToken(m_session) == null )
            throw new LockException("This Session does not own this Lock, so it cannot be unlocked.");
        
        if( isModified() )
            throw new InvalidItemStateException("This Node must not be modified prior to unlocking");
        
        Property p = getProperty("jcr:lockOwner");
        p.remove();
        p = getProperty("jcr:lockIsDeep");
        p.remove();
        
        m_session.removeLockToken( lock.getLockToken(m_session) );
        m_lockManager.removeLock( lock );
        lock.invalidate();
        
        save();
    }


    public QLock.Impl getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException
    {
        QLock li = m_lockManager.findLock( getInternalPath() );
        
        //
        //  Must return a clone of the Lock which is particular to this session.
        //
        
        if( li != null )
            return li.getLockInstance(m_session);
        
        return null;
    }


    public boolean holdsLock() throws RepositoryException
    {
        QLock li = m_lockManager.getLock( getInternalPath() );

        return li != null;
    }
    

    public boolean isLocked() throws RepositoryException
    {
        QLock li = m_lockManager.findLock( getInternalPath() );

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
        if( !getInternalPath().isRoot() )
        {
            QLock li = m_lockManager.findLock(getInternalPath());
        
            if( li != null && li.getLockToken(m_session) == null )
            {
                return true;
            }
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
            save();
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
