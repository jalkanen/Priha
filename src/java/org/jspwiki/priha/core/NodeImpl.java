package org.jspwiki.priha.core;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.*;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.jspwiki.priha.nodetype.NodeDefinitionImpl;
import org.jspwiki.priha.util.*;

public class NodeImpl extends ItemImpl implements Node, Comparable
{
    private ArrayList<Property> m_properties = new ArrayList<Property>();
    
    private ArrayList<NodeImpl> m_children = new ArrayList<NodeImpl>();
    
    private ArrayList m_mixinTypes = new ArrayList();

    private NodeDefinition m_definition;
    
    private enum NodeState { EXISTS, REMOVED };
    
    private NodeState m_state = NodeState.EXISTS;
    
    public NodeImpl( SessionImpl session, String path )
    {
        super( session, path );
    }
    
    public NodeImpl( SessionImpl session, Path path )
    {
        super( session, path );
    }
    
    void markModified()
    {
        m_modified = true;
        m_session.markDirty(this);
    }
    
    public void addMixin(String mixinName)
                                          throws NoSuchNodeTypeException,
                                              VersionException,
                                              ConstraintViolationException,
                                              LockException,
                                              RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException("Node.addMixin()");
    }

    public Node addNode(String relPath)
                                       throws ItemExistsException,
                                           PathNotFoundException,
                                           VersionException,
                                           ConstraintViolationException,
                                           LockException,
                                           RepositoryException
    {
        Path absPath = m_path.resolve(relPath);
        
        if( absPath.getLastComponent().indexOf('[') != -1 )
        {
            throw new RepositoryException("Cannot add an indexed entry");
        }
        
        NodeImpl ni = null;
        try
        {
            Path parentPath = absPath.getParentPath();
        
            Item item = m_session.getItem(parentPath.toString());

            if( !item.isNode() )
            {
                throw new ConstraintViolationException("Trying to add a node to a Property");
            }
        
            ni = new NodeImpl(m_session,absPath.toString());

            ni.setProperty( "jcr:primaryType", "nt:base" );

            ni.markModified();
            m_session.addNode( ni );
        }
        catch( InvalidPathException e) 
        {
            throw new PathNotFoundException( e.getMessage() );
        }
        return ni;
    }

    public Node addNode(String relPath, String primaryNodeTypeName)
                                                                   throws ItemExistsException,
                                                                       PathNotFoundException,
                                                                       NoSuchNodeTypeException,
                                                                       LockException,
                                                                       VersionException,
                                                                       ConstraintViolationException,
                                                                       RepositoryException
    {
        if( primaryNodeTypeName == null ) throw new NoSuchNodeTypeException("Node type cannot be null");

        Node nd = addNode(relPath);
        nd.setProperty( "jcr:primaryType", primaryNodeTypeName );
        
        return nd;
    }

    /**
     *  Adds a child node to the child list.  Assumes that child node is
     *  already set up.
     *  @param node
     *  @return the new length of the child node array
     */
    int addChildNode( NodeImpl childNode )
    {
        m_children.add( childNode );
        childNode.m_parent = this;
        
        return m_children.size();
    }
    
    void removeChildNode( NodeImpl childNode )
    {
        m_children.remove(childNode);
        childNode.m_parent = null;
    }
    
    public boolean canAddMixin(String mixinName) throws NoSuchNodeTypeException, RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException("Node.canAddMixin()");
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
        if( m_definition == null )
        {
            m_definition = new NodeDefinitionImpl(m_session.getWorkspace().getNodeTypeManager().getNodeType("nt:base"));
        }
            
        return m_definition;
    }

    public int getIndex() throws RepositoryException
    {        
        int idx = 1;
        
        for( Node nd : m_parent.m_children )
        {
            if( nd.getName().equals(getName()) )
            {
                if( nd == this )
                {
                    return idx;
                }
                
                idx++;
            }
        }
        
        throw new RepositoryException("Unable to even find myself!");
    }

    public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();
    }

    public NodeType[] getMixinNodeTypes() throws RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();
    }

    public Node getNode(String relPath) throws PathNotFoundException, RepositoryException
    {
        Path p = m_path.resolve(relPath);

        Item i = m_session.getItem(p.toString());
        
        if( i.isNode() )
        {
            return (Node) i;
        }
        
        throw new PathNotFoundException("Path refers to a property: "+p.toString());
    }

    public NodeIterator getNodes() throws RepositoryException
    {
        List<Node> ls = new ArrayList<Node>();
        ls.addAll(m_children);
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
        
        for( NodeImpl node : m_children )
        {
            Matcher match = p.matcher( node.getName() );
            
            if( match.matches() )
            {
                matchedpaths.add( node );
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
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();
    }

    public NodeType getPrimaryNodeType() throws RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();
    }

    public PropertyIterator getProperties() throws RepositoryException
    {
        List<Property> ls = new ArrayList<Property>();
        ls.addAll( m_properties );
        return new PropertyIteratorImpl( ls );
    }

    public PropertyIterator getProperties(String namePattern) throws RepositoryException
    {
        Pattern p = parseJCRPattern(namePattern);        
        
        ArrayList<Property> matchedpaths = new ArrayList<Property>();
        
        for( Property prop : m_properties )
        {
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
        for( Property prop : m_properties )
        {
            if( name.equals(prop.getName()) )
                return prop;
        }
        
        throw new PathNotFoundException( m_path.resolve(name).toString() );
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

    public PropertyIterator getReferences() throws RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();
    }

    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();
    }

    public VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException("Node.getVersionHistory()");
    }

    public boolean hasNode(String relPath) throws RepositoryException
    {
        Path absPath = m_path.resolve(relPath);
        return m_session.hasNode(absPath.toString());
    }

    public boolean hasNodes() throws RepositoryException
    {
        return m_children.size() > 0;
    }

    public boolean hasProperties() throws RepositoryException
    {
        return m_properties.size() > 0;
    }

    public boolean hasProperty(String relPath) throws RepositoryException
    {
        try
        {
            return getProperty(relPath) != null; // FIXME: Slow
        }
        catch( PathNotFoundException e ) {}
        
        return false;
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
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();
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
    private Property prepareProperty( String name, Object value ) throws PathNotFoundException, RepositoryException
    {
        Property prop = null;
    
        try
        {
            prop = getProperty(name);
        }
        catch( PathNotFoundException e ){}
        
        if( prop == null )
        {
            Path propertypath = m_path.resolve(name);
            prop = new PropertyImpl( m_session, propertypath.toString() );
            m_properties.add(prop);
            markModified();
        }
        
        if( value == null )
        {
            removeProperty(prop);
        }
        
        return prop;
    }
    
    /**
     *  Removes a given property from the node.
     *  @param prop
     */
    protected void removeProperty(Property prop)
    {
        m_properties.remove(prop);
    }

    public Property setProperty(String name, Value value)
                                                         throws ValueFormatException,
                                                             VersionException,
                                                             LockException,
                                                             ConstraintViolationException,
                                                             RepositoryException
    {
        Property p = prepareProperty( name, value );
        
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
        Property p = prepareProperty( name, value );
        
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
        Property p = prepareProperty( name, values );
        
        p.setValue(values);
        
        return p;    
    }

    public Property setProperty(String name, Value[] values, int type)
                                                                      throws ValueFormatException,
                                                                          VersionException,
                                                                          LockException,
                                                                          ConstraintViolationException,
                                                                          RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();
    }

    public Property setProperty(String name, String[] values)
                                                             throws ValueFormatException,
                                                                 VersionException,
                                                                 LockException,
                                                                 ConstraintViolationException,
                                                                 RepositoryException
    {
        Property p = prepareProperty( name, values );
        
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
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();
    }
    public Property setProperty(String name, String value)
                                                          throws ValueFormatException,
                                                              VersionException,
                                                              LockException,
                                                              ConstraintViolationException,
                                                              RepositoryException
    {
        Property prop = prepareProperty(name,value);
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
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();
    }

    public Property setProperty(String name, InputStream value)
                                                               throws ValueFormatException,
                                                                   VersionException,
                                                                   LockException,
                                                                   ConstraintViolationException,
                                                                   RepositoryException
    {
        Property p = prepareProperty( name, value );
        
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
        Property p = prepareProperty( name, value );
        
        p.setValue(value);
        
        return p;        // TODO Auto-generated method stub
    }

    public Property setProperty(String name, double value)
                                                          throws ValueFormatException,
                                                              VersionException,
                                                              LockException,
                                                              ConstraintViolationException,
                                                              RepositoryException
    {
        Property p = prepareProperty( name, value );
        
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
        Property p = prepareProperty( name, value );
        
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
        Property p = prepareProperty( name, value );
        
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
        Property p = prepareProperty( name, value );
        
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
    
    private void saveNodeOnly() throws RepositoryException
    {
        WorkspaceImpl ws = (WorkspaceImpl)m_session.getWorkspace();
        
        ws.saveNode(this);
        m_modified = false;
        m_new      = false;
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
        List<String> modifications = new ArrayList<String>();
        
        WorkspaceImpl ws = (WorkspaceImpl)m_session.getWorkspace();

        if( isModified() ) 
        {
            saveNodeOnly();
            modifications.add( getPath() );
        }
        
        for( NodeImpl node : m_children )
        {
            if( node.isModified() ) 
            {
                node.saveNodeOnly();
                modifications.add(getPath());
            }
        }

        m_session.nodesSaved(modifications);
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
        m_session.getNodeManager().remove( this );
        markModified();
    }

    public void addChildProperty(PropertyImpl property)
    {
        m_properties.add( property );
        property.m_parent = this;
    }

}
