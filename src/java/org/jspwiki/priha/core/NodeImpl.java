package org.jspwiki.priha.core;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.*;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.*;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.jspwiki.priha.core.binary.MemoryBinarySource;
import org.jspwiki.priha.core.values.ValueFactoryImpl;
import org.jspwiki.priha.nodetype.GenericNodeType;
import org.jspwiki.priha.nodetype.NodeDefinitionImpl;
import org.jspwiki.priha.util.*;
import org.jspwiki.priha.version.VersionHistoryImpl;

public class NodeImpl extends ItemImpl implements Node, Comparable
{
    private static final String JCR_MIXIN_TYPES = "jcr:mixinTypes";
    public static final String JCR_UUID = "jcr:uuid";
    private NodeDefinition      m_definition;
    
    private GenericNodeType     m_primaryType;

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
    
    protected NodeImpl( SessionImpl session, Path path, GenericNodeType primaryType, NodeDefinition nDef, boolean populateDefaults )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        super( session, path );

        m_primaryType = primaryType;
        m_definition  = nDef;
        
        if( populateDefaults )
        {
            internalSetProperty( "jcr:primaryType", m_primaryType.getName(), PropertyType.NAME );
        }
    }


    protected NodeImpl( SessionImpl session, String path, GenericNodeType primaryType, NodeDefinition nDef, boolean populateDefaults )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        this( session, new Path(path), primaryType, nDef, populateDefaults );
    }
        

    public void addMixin(String mixinName)
                                          throws NoSuchNodeTypeException,
                                              VersionException,
                                              ConstraintViolationException,
                                              LockException,
                                              RepositoryException
    {
        ValueFactory vf = ValueFactoryImpl.getInstance();

        NodeType mixin = getNodeTypeManager().getNodeType(mixinName);

        if( !mixin.isMixin() )
            throw new NoSuchNodeTypeException("Type "+mixinName+" is not a mixin type!");

        Property p;
        try
        {
            p = getProperty(JCR_MIXIN_TYPES);

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
            internalSetProperty( JCR_MIXIN_TYPES, values, PropertyType.NAME );
        }

        autoCreateProperties();

        markModified(true);
    }

    /**
     *  Figures out what type the child node should be.
     *  
     *  @param relpath A relative path to the child node.
     *  @return A NodeType for the path
     *  @throws RepositoryException If something goes wrong.
     */
    private GenericNodeType assignChildType(String relpath) throws RepositoryException
    {
        NodeDefinition nd = m_primaryType.findNodeDefinition(relpath);

        if( nd == null )
        {
            throw new ConstraintViolationException("Cannot assign a child type to this node, since there is no default type.");
        }
        
        GenericNodeType nt = (GenericNodeType) nd.getDefaultPrimaryType();

        return nt;
    }

    public Node addNode(String relPath, String primaryNodeTypeName)
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
        
        Path absPath = m_path.resolve(relPath);

        NodeImpl ni = null;
        try
        {
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
            GenericNodeType assignedType;
            NodeDefinition  assignedNodeDef;

            if( primaryNodeTypeName == null )
            {
                assignedType = parent.assignChildType(relPath);
            }
            else
            {
                assignedType = (GenericNodeType) getNodeTypeManager().getNodeType( primaryNodeTypeName );
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
            if( !parent.getDefinition().getDeclaringNodeType().canAddChildNode(absPath.getLastComponent()) )
            {
                throw new ConstraintViolationException("Parent node does not allow adding nodes of name "+absPath.getLastComponent());
            }

            //
            //  Node type and definition are now okay, so we'll create the node
            //  and add it to our session.
            //
            ni = new NodeImpl( m_session, absPath, assignedType, assignedNodeDef, true );

            ni.sanitize();

            ni.markModified(false);
            m_session.addNode( ni );
        }
        catch( InvalidPathException e)
        {
            throw new PathNotFoundException( e.getMessage(), e );
        }
        return ni;
    }

    private NodeTypeManager getNodeTypeManager() throws RepositoryException
    {
        return m_session.getWorkspace().getNodeTypeManager();
    }

    public Node addNode(String relPath)
        throws ItemExistsException,
               PathNotFoundException,
               NoSuchNodeTypeException,
               LockException,
               VersionException,
               ConstraintViolationException,
               RepositoryException
    {
        Node nd = addNode(relPath, null);

        return nd;
    }

    public boolean canAddMixin(String mixinName) throws NoSuchNodeTypeException, RepositoryException
    {
        NodeType nt = getNodeTypeManager().getNodeType( mixinName );

        if( !nt.isMixin() )
        {
            throw new NoSuchNodeTypeException(mixinName+" is not a mixin type!");
        }
        
        if( hasMixinType(nt.getName()) )
        {
            return false;
        }

        // FIXME: This is rather permissive...

        return true;
    }

    private boolean hasMixinType(String mixinType)
    {
        try
        {
            Property pi = getProperty( JCR_MIXIN_TYPES );
        
            for( Value v : pi.getValues() )
            {
                String mixin = v.getString();
            
                if( mixin.equals(mixinType) ) return true;
            }
        }
        catch( RepositoryException e ) {}
        return false;
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

    public Version checkin()
                            throws VersionException,
                                UnsupportedRepositoryOperationException,
                                InvalidItemStateException,
                                LockException,
                                RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException("Node.checkin()");
    }

    public void checkout() throws UnsupportedRepositoryOperationException, LockException, RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException("Node.checkout()");

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

    public Version getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();
    }

    public String getCorrespondingNodePath(String workspaceName)
                                                                throws ItemNotFoundException,
                                                                    NoSuchWorkspaceException,
                                                                    AccessDeniedException,
                                                                    RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();
    }

    public NodeDefinition getDefinition() throws RepositoryException
    {
        if( m_definition == null ) sanitize();

        return m_definition;
    }

    public int getIndex() throws RepositoryException
    {
        // Not supported, so always constant
        return 1;
    }

    public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();
    }

    public NodeType[] getMixinNodeTypes() throws RepositoryException
    {
        ArrayList<NodeType> mixinTypes = new ArrayList<NodeType>();
        
        //
        //  If there are no mixin types, then let's just return an empty array.
        //
        try
        {
            Property p = getProperty( JCR_MIXIN_TYPES );
        
            for( Value v : p.getValues() )
            {
                NodeType nt = m_session.getWorkspace().getNodeTypeManager().getNodeType( v.getString() );
            
                mixinTypes.add( nt );
            }
        }
        catch( RepositoryException e ) {}
        
        return mixinTypes.toArray( new NodeType[0] );
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
    
    public Node getNode(String relPath) throws PathNotFoundException, RepositoryException
    {
        return getNode( m_path.resolve(relPath) );
    }

    public NodeIterator getNodes() throws RepositoryException
    {
        List<Node> ls = new ArrayList<Node>();

        List<Path> children = m_session.listNodes( m_path );
        
        for( Path p : children )
        {
            NodeImpl nd = getNode( p );
            ls.add( nd );
        }

        NodeIterator it = new NodeIteratorImpl(ls);

        return it;
    }

    /**
     *  Replaces a string with an other string.
     *
     *  @param orig Original string.  Null is safe.
     *  @param src  The string to find.
     *  @param dest The string to replace <I>src</I> with.
     */
    private final static String replaceString( String orig, String src, String dest )
    {
        if ( orig == null ) return null;
        if ( src == null || dest == null ) throw new NullPointerException();
        if ( src.length() == 0 ) return orig;

        StringBuilder res = new StringBuilder();
        int start, end = 0, last = 0;

        while ( (start = orig.indexOf(src,end)) != -1 )
        {
            res.append( orig.substring( last, start ) );
            res.append( dest );
            end  = start+src.length();
            last = start+src.length();
        }

        res.append( orig.substring( end ) );

        return res.toString();
    }

    public NodeIterator getNodes(String namePattern) throws RepositoryException
    {
        Pattern p = parseJCRPattern(namePattern);

        ArrayList<Node> matchedpaths = new ArrayList<Node>();

        List<Path> children = m_session.listNodes( m_path );
        
        for( Path path : children )
        {
            Matcher match = p.matcher( path.getLastComponent() );

            if( match.matches() )
            {
                matchedpaths.add( getNode(path) );
            }
        }

        return new NodeIteratorImpl(matchedpaths);
    }

    /**
     *  Turns a JCR pattern into a java.util.regex.Pattern
     *
     *  @param namePattern
     *  @return
     */
    private Pattern parseJCRPattern(String namePattern)
    {
        namePattern = replaceString( namePattern, "*", ".*" );
        namePattern = "^("+namePattern +")$";
        Pattern p = Pattern.compile( namePattern );
        return p;
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

    public NodeType getPrimaryNodeType() throws RepositoryException
    {
        return m_primaryType;
    }

    public PropertyIterator getProperties() throws RepositoryException
    {
        List<PropertyImpl> ls = new ArrayList<PropertyImpl>();
        
        ls.addAll( m_session.m_provider.getProperties(getInternalPath()) );
        
        return new PropertyIteratorImpl( ls );
    }

    public PropertyIterator getProperties(String namePattern) throws RepositoryException
    {
        Pattern p = parseJCRPattern(namePattern);

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

    public Property getChildProperty( String name )
        throws RepositoryException
    {
        ItemImpl ii = m_session.m_provider.getItem( m_path.resolve(name) );
        
        if( ii.isNode() ) throw new ItemNotFoundException("Found a Node, not a Property");
        
        return (Property) ii;
    }

    public Property getProperty(String relPath) throws PathNotFoundException, RepositoryException
    {
        Path abspath = m_path.resolve(relPath);

        Item item = m_session.getItem(abspath.toString());

        if( item != null && !item.isNode() )
        {
            return (Property) item;
        }

        throw new PathNotFoundException( abspath.toString() );
    }

    void autoCreateProperties() throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        log.finer( "Autocreating properties for "+m_path );

        autoCreateProperties( getPrimaryNodeType() );

        for( NodeType nt : getMixinNodeTypes() )
        {
            autoCreateProperties( nt );
        }
    }

    private void autoCreateProperties(NodeType nt) throws RepositoryException, ValueFormatException, VersionException, LockException, ConstraintViolationException
    {
        ValueFactoryImpl vfi = ValueFactoryImpl.getInstance();
        
        for( PropertyDefinition pd : nt.getPropertyDefinitions() )
        {
            if( pd.isAutoCreated() && !hasProperty(pd.getName()) )
            {
                log.finer("Autocreating property "+pd.getName());

                String path = m_path + "/" + pd.getName();
                PropertyImpl pi = new PropertyImpl(m_session,new Path(path),pd);

                // FIXME: Add default value generation

                if( JCR_UUID.equals(pi.getName()) )
                {
                    pi.loadValue( vfi.createValue( UUID.randomUUID().toString() ) );
                }
                
                if( "jcr:created".equals(pi.getName() ))
                {
                    pi.loadValue( vfi.createValue( Calendar.getInstance() ) );
                }

                addChildProperty( pi );
            }
        }
    }

    public PropertyIterator getReferences() throws RepositoryException
    {
        Collection<PropertyImpl> references = m_session.getReferences( getUUID() );

        return new PropertyIteratorImpl(references);
    }

    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException
    {
        if( isNodeType("mix:referenceable") )
        {
            Property uuid = getProperty(JCR_UUID);

            return uuid.getValue().getString();
        }

        throw new UnsupportedRepositoryOperationException("No UUID defined for this node");
    }

    public VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException
    {
        if( isNodeType("mix:versionable") )
        {
            // Locate version history

            Path versionPath = new Path( "/jcr:system/jcr:versionStorage", m_path );

            VersionHistory vh = VersionHistoryImpl.getInstance( m_session, versionPath );

            return vh;
        }

        throw new UnsupportedRepositoryOperationException("This node does not have a version history.");
    }

    public boolean hasNode(String relPath) throws RepositoryException
    {
        Path absPath = m_path.resolve(relPath);
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

    public boolean hasProperty(String relPath) throws RepositoryException
    {
        Path abspath = m_path.resolve(relPath);
        
        return m_session.hasProperty( abspath );
    }

    public boolean holdsLock() throws RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();
    }

    public boolean isCheckedOut() throws RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();
    }

    public boolean isLocked() throws RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();
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

    public Lock lock(boolean isDeep, boolean isSessionScoped)
                                                             throws UnsupportedRepositoryOperationException,
                                                                 LockException,
                                                                 AccessDeniedException,
                                                                 InvalidItemStateException,
                                                                 RepositoryException
    {
        // TODO Auto-generated method stub
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

    public void removeMixin(String mixinName)
                                             throws NoSuchNodeTypeException,
                                                 VersionException,
                                                 ConstraintViolationException,
                                                 LockException,
                                                 RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();

    }

    public void restore(String versionName, boolean removeExisting)
                                                                   throws VersionException,
                                                                       ItemExistsException,
                                                                       UnsupportedRepositoryOperationException,
                                                                       LockException,
                                                                       InvalidItemStateException,
                                                                       RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();

    }

    public void restore(Version version, boolean removeExisting)
                                                                throws VersionException,
                                                                    ItemExistsException,
                                                                    UnsupportedRepositoryOperationException,
                                                                    LockException,
                                                                    RepositoryException
    {
        // TODO Auto-generated method stub
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
        // TODO Auto-generated method stub

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
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();

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
    private PropertyImpl prepareProperty( String name, Object value ) throws PathNotFoundException, RepositoryException
    {
        PropertyImpl prop = null;

        //
        //  Because we rely quite a lot on the primary property, we need to go and
        //  handle it separately.
        //
        if( name.equals("jcr:primaryType") )
        {
            if( hasProperty("jcr:primaryType") )
            {
                throw new ConstraintViolationException("The object has already been assigned a primary type!");
            }

            //  We know where this belongs to.
            GenericNodeType gnt = (GenericNodeType)getNodeTypeManager().getNodeType("nt:base");

            PropertyDefinition primaryDef = gnt.findPropertyDefinition("jcr:primaryType",false);

            prop = new PropertyImpl( m_session,
                                     m_path.resolve(name),
                                     primaryDef );

            addChildProperty( prop );
            markModified(false);
            return prop;
        }

        try
        {
            prop = (PropertyImpl) getProperty(name);
        }
        catch( PathNotFoundException e ){}

        if( prop == null )
        {
            Path propertypath = m_path.resolve(name);

            Path p = propertypath.getParentPath();

            NodeImpl parentNode = (NodeImpl) m_session.getItem(p);

            boolean ismultiple = value instanceof Object[];

            GenericNodeType parentType = (GenericNodeType)parentNode.getPrimaryNodeType();
            
            PropertyDefinition pd = parentType.findPropertyDefinition(name,ismultiple);
            
            prop = new PropertyImpl( m_session, propertypath, pd );
            prop.markModified(false);
        }
        else
        {
            markModified(true);
        }
        
        if( value == null )
        {
            removeProperty(prop);
        }
        else
        {
            addChildProperty( prop );
        }
        return prop;
    }

    /**
     *  Checks only the properties of this Node.
     *  
     *  @param propertyName The name of the property to check.
     *  @return True, if the property exists.
     */
    /*
    private boolean hasChildProperty(String propertyName)
    {
        for( PropertyImpl pi : m_properties )
        {
            if( pi.m_name.equals(propertyName) ) return true;
        }
        
        return false;
    }
    */

    /**
     *  Removes a given property from the node.
     *  @param prop
     */
    protected void removeProperty(PropertyImpl prop)
    {
        prop.m_state = ItemState.REMOVED;
        // m_properties.remove(prop);
        markModified(true);
    }

    public Property setProperty(String name, Value value)
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

    public Property setProperty(String name, Value value, int type)
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

    public Property setProperty(String name, Value[] values)
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

    private PropertyImpl internalSetProperty(String name, Value[] values, int type) throws PathNotFoundException, RepositoryException
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

        return p;
    }

    private PropertyImpl internalSetProperty(String name, String value, int type) throws PathNotFoundException, RepositoryException
    {
        PropertyImpl prop = prepareProperty(name,value);
        
        prop.loadValue( ValueFactoryImpl.getInstance().createValue(value,type) );
        
        return prop;
    }

    public Property setProperty(String name, String value)
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

    public Property setProperty(String name, String value, int type)
                                                                    throws ValueFormatException,
                                                                        VersionException,
                                                                        LockException,
                                                                        ConstraintViolationException,
                                                                        RepositoryException
    {
        if( value == null )
        {
            Property p = getProperty(name);
            p.remove();
            return p;
        }
        
        try
        {
            Value val = ValueFactoryImpl.getInstance().createValue( value, type );
            return setProperty( name, val );
        }
        catch( ValueFormatException e )
        {
            // This is kind of stupid to start throwing the same exception again.
            throw new ConstraintViolationException(e.getMessage());
        }
    }

    public Property setProperty(String name, InputStream value)
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

    public Property setProperty(String name, boolean value)
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

    public Property setProperty(String name, double value)
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

    public Property setProperty(String name, long value)
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

    public Property setProperty(String name, Calendar value)
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

    public Property setProperty(String name, Node value)
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

    public void unlock()
                        throws UnsupportedRepositoryOperationException,
                            LockException,
                            AccessDeniedException,
                            InvalidItemStateException,
                            RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();

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

    /**
     *  Just simply puts a node in the repository, but does not affect anything
     *  else, except Nodes own state.
     *
     *  @throws RepositoryException
     */
    /*
    protected void saveItemOnly() throws RepositoryException
    {
        if( isModified() )
        {
            WorkspaceImpl ws = (WorkspaceImpl)m_session.getWorkspace();

            switch( m_state )
            {
                case REMOVED:
                    ws.removeItem(this);
                    break;
                default:
                    ws.saveItem(this);
                    m_state = ItemState.EXISTS;
                    break;
            }
            m_modified = false;
            m_new      = false;
        }
    }
*/
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

    public int compareTo(Object o)
    {
        if( o instanceof NodeImpl )
        {
            NodeImpl nd = (NodeImpl)o;

            try
            {
                return getPath().compareTo(nd.getPath());
            }
            catch( RepositoryException e )
            {} // FIXME: This should never occur
        }

        throw new ClassCastException("Attempt to compare NodeImpl with "+o.getClass().getName());
    }

    public void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        if( m_state == ItemState.REMOVED )
            //throw new ConstraintViolationException(getPath()+" has already been removed");
            return; // Die nicely
            
        if( getPath().equals("/") || getPath().equals("/jcr:system") ) return; // Refuse to remove

        NodeType parentType = getParent().getPrimaryNodeType();
        
        if( !parentType.canRemoveItem(getName()) &&
            ((NodeImpl)getParent()).getState() != ItemState.REMOVED )
        {
            throw new ConstraintViolationException("Attempted to delete a mandatory child node:"+getInternalPath());
        }

        m_session.remove( this );
        markModified(true);
        m_state = ItemState.REMOVED;

        for( NodeIterator ndi = getNodes(); ndi.hasNext(); )
        {
            Node nd = ndi.nextNode();
          
            nd.remove();
        }

        log.fine("Removed "+getPath());
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
                PropertyImpl primarytype = (PropertyImpl) getProperty( "jcr:primaryType" );
            }
            catch( Exception e )
            {
                if( m_path.isRoot() )
                {
                    setProperty( "jcr:primaryType", "nt:unstructured", PropertyType.NAME );
                }
                else
                {
                    setProperty( "jcr:primaryType",
                                 assignChildType( m_path.getLastComponent() ).toString(),
                                 PropertyType.NAME );
                }
            }

            if( getParent() != null )
            {
                GenericNodeType mytype = (GenericNodeType)getPrimaryNodeType();

                m_definition = ((GenericNodeType)getParent().getPrimaryNodeType()).findNodeDefinition( mytype.getName() );
            }
            else
            {
                // FIXME: Not correct
                m_definition = new NodeDefinitionImpl( getPrimaryNodeType(), "nt:unstructured" );
            }

            if( m_definition == null )
            {
                throw new RepositoryException("Cannot assign a node definition for "+m_path);
            }

        }

        autoCreateProperties();

        GenericNodeType mytype = (GenericNodeType)getPrimaryNodeType();

        for( PropertyIterator i = getProperties(); i.hasNext(); )
        {
            PropertyImpl pi = (PropertyImpl)i.next();
            if( pi.getDefinition() == null )
            {
                PropertyDefinition pd = mytype.findPropertyDefinition( pi.getName(), false ); // FIXME: Really?

                pi.m_definition = pd;
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
        WorkspaceImpl ws = (WorkspaceImpl) m_session.getWorkspace();
        
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
        
        if( m_state != ItemState.NEW && !ws.nodeExists(m_path))
        {
            throw new InvalidItemStateException("Looks like this Node has been removed by another session.");
        }
        
        //
        //  Check mandatory properties
        //
        checkMandatoryProperties( getPrimaryNodeType() );

        for( NodeType nt : getMixinNodeTypes() )
        {
            checkMandatoryProperties( nt );
        }
    }

    /**
     *  Checks the mandatory properties for this Nodetype and throws a ConstraintViolationException
     *  if it's not existing.
     *  
     * @param nt
     * @throws RepositoryException 
     */
    private void checkMandatoryProperties(NodeType nt) throws RepositoryException
    {
        for( PropertyDefinition pd : nt.getPropertyDefinitions() )
        {
            if( pd.isMandatory() && !hasProperty(pd.getName()) )
            {
                throw new ConstraintViolationException("Node is missing property "+pd.getName());
            }
        }
    }
}
