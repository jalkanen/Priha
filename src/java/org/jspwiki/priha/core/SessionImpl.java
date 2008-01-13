package org.jspwiki.priha.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.version.VersionException;

import org.jspwiki.priha.core.values.ValueFactoryImpl;
import org.jspwiki.priha.nodetype.GenericNodeType;
import org.jspwiki.priha.util.InvalidPathException;
import org.jspwiki.priha.util.Path;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 *  The SessionImpl class implements a JCR Session.  It is non thread safe,
 *  so each Thread must have its own Session.
 *  
 *  @author jalkanen
 */
public class SessionImpl implements Session
{
    private static final String JCR_SYSTEM = "jcr:system";
    private RepositoryImpl m_repository;
    private WorkspaceImpl  m_workspace;
    private NodeManager    m_nodeManager;

    private SimpleCredentials m_credentials;

    private Logger         log = Logger.getLogger( getClass().getName() );
    
    /** Keeps a list of nodes which have been updated and must be flushed at next
     *  save().
     */
    private TreeSet<NodeImpl> m_updateList = new TreeSet<NodeImpl>();

    public SessionImpl( RepositoryImpl rep, Credentials creds, String name )
        throws RepositoryException
    {
        m_repository = rep;
        m_workspace  = new WorkspaceImpl( this, name, rep.getProviderManager() );
        m_nodeManager = new NodeManager( this );

        if( !hasNode("/"+JCR_SYSTEM) )
        {
            repopulate();
        }
        
        if( creds instanceof SimpleCredentials )
        {
            m_credentials = (SimpleCredentials)creds;
        }
    }

    /**
     * Adds a node to the internal checklists
     *
     * @param node
     * @throws RepositoryException
     */
    void addNode( NodeImpl node )
        throws RepositoryException
    {
        try
        {
            NodeImpl parentNode = (NodeImpl)getItem( node.getInternalPath().getParentPath() );
            
            m_nodeManager.addNode(parentNode,node);
        }
        catch (InvalidPathException e)
        {
            throw new ItemNotFoundException(e.getMessage());
        }
    }

    void markDirty( NodeImpl node )
    {
        synchronized(m_updateList)
        {
            m_updateList.add(node);
        }
    }

    boolean hasNode( String absPath )
    {
        if( !m_nodeManager.hasNode(absPath) )
        {
            try
            {
                return m_repository.getProviderManager().hasNode( m_workspace, new Path(absPath) );
            }
            catch (InvalidPathException e)
            {
                return false;
            }
        }
        
        return true;
    }

    boolean hasProperty( Path absPath ) throws RepositoryException
    {
        Path parent = absPath.getParentPath();
        
        NodeImpl nd = m_nodeManager.findNode( parent );
        
        if( nd == null )
            nd = m_workspace.loadNode( parent );
        
        if( nd != null )
        {
            try
            {
                nd.getChildProperty( absPath.getLastComponent() );
                return true;
            }
            catch( PathNotFoundException e )
            {
            }
        }
        return false;
    }
    
    public void addLockToken(String lt)
    {
        // TODO Auto-generated method stub
        // throw new UnsupportedRepositoryOperationException();
    }

    public void checkPermission(String absPath, String actions) throws AccessControlException, RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException("Session.checkPermission()");
    }

    public void exportDocumentView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException("Session.exportDocumentView()");
    }

    public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException
    {
        // TODO Auto-generated method stub

        throw new UnsupportedRepositoryOperationException("Session.exportDocumentView2()");
    }

    public void exportSystemView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException
    {
        // TODO Auto-generated method stub

        throw new UnsupportedRepositoryOperationException("Session.exportSystemView()");
    }

    public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException
    {
        // TODO Auto-generated method stub

        throw new UnsupportedRepositoryOperationException("Session.exportSystemView2()");
    }

    public Object getAttribute(String name)
    {
        Object res = null;

        if( m_credentials != null )
        {
            res = m_credentials.getAttribute(name);
        }
        return res;
    }

    public String[] getAttributeNames()
    {
        if( m_credentials != null )
        {
            return m_credentials.getAttributeNames();
        }

        return new String[0];
    }

    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, RepositoryException
    {
        throw new UnsupportedRepositoryOperationException("Session.getImportContentHandler()");
        // TODO Auto-generated method stub
    }

    public Item getItem(String absPath) throws PathNotFoundException, RepositoryException
    {
        Path p = new Path(absPath);

        try
        {
            //
            //  Check internal cache
            //
            NodeImpl nd = m_nodeManager.findNode( p );

            if( nd != null )
            {
                return nd;
            }

            if( p.depth() > 0 )
            {
                nd = m_nodeManager.findNode( p.getParentPath() );

                if( nd != null && nd.hasProperty( p.getLastComponent() ) )
                {
                    return nd.getChildProperty( p.getLastComponent() );
                }
            }
            
            //
            //  Load from repository
            //
            try
            {
                nd = m_workspace.loadNode( p );
            
                if( nd != null )
                {
                    return nd;   
                }
            }
            catch( RepositoryException e ) {}
            
            try
            {
                nd = m_workspace.loadNode( p.getParentPath() );
            
                if( nd != null && nd.hasProperty( p.getLastComponent() ) )
                {
                    return nd.getChildProperty( p.getLastComponent() );
                }
            }
            catch( RepositoryException e ) {}
        }
        catch( InvalidPathException e )
        {
        }

        throw new PathNotFoundException( absPath );
    }

    public String[] getLockTokens()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getNamespacePrefix(String uri) throws NamespaceException, RepositoryException
    {
        return m_workspace.getNamespaceRegistry().getPrefix(uri);
    }

    public String[] getNamespacePrefixes() throws RepositoryException
    {
        return m_workspace.getNamespaceRegistry().getPrefixes();
    }

    public String getNamespaceURI(String prefix) throws NamespaceException, RepositoryException
    {
        return m_workspace.getNamespaceRegistry().getURI(prefix);
    }

    public Node getNodeByUUID(String uuid) throws ItemNotFoundException, RepositoryException
    {
        return m_nodeManager.getNodeByUUID( uuid );
    }

    public Repository getRepository()
    {
        return m_repository;
    }

    public Node getRootNode() throws RepositoryException
    {
        return (Node) getItem("/");
    }

    public String getUserID()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ValueFactory getValueFactory() throws UnsupportedRepositoryOperationException, RepositoryException
    {
        return ValueFactoryImpl.getInstance();
    }

    public Workspace getWorkspace()
    {
        return m_workspace;
    }

    public boolean hasPendingChanges() throws RepositoryException
    {
        return !m_updateList.isEmpty();
    }

    public Session impersonate(Credentials credentials) throws LoginException, RepositoryException
    {
        throw new UnsupportedRepositoryOperationException("Session.impersonate()");
        // TODO Auto-generated method stub
    }

    public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException, RepositoryException
    {
        throw new UnsupportedRepositoryOperationException("Session.importXML()");
        // TODO Auto-generated method stub
    }

    public boolean isLive()
    {
        return m_workspace != null;
    }

    public boolean itemExists(String absPath) throws RepositoryException
    {
        return itemExists( new Path(absPath) );
    }

    public void logout()
    {
        if( isLive() )
        {
            m_workspace.logout();
            m_workspace = null;
        }
    }

    public void move(String srcAbsPath, String destAbsPath) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException("Session.move()");

    }

    public void refresh(boolean keepChanges) throws RepositoryException
    {
        if( keepChanges ) throw new UnsupportedRepositoryOperationException("Session.refresh(true)");

        m_updateList.clear();

        m_nodeManager.reset();

        repopulate();
    }

    /**
     *  This method makes sure that certain default stuff is available.
     *  @throws RepositoryException
     */
    private void repopulate() throws RepositoryException
    {
        if( !hasNode("/") )
        {
            NodeImpl ni = null;
            
            log.info("Repository empty; setting up root node...");

            GenericNodeType rootType = (GenericNodeType)getWorkspace().getNodeTypeManager().getNodeType("nt:unstructured");
            
            NodeDefinition nd = rootType.findNodeDefinition("*");
            
            ni = new NodeImpl( this, "/", rootType, nd );

            ni.sanitize();
                
            m_nodeManager.addNode( null, ni );
            ni.markModified();
                
            save();
        }
        
        if( !hasNode("/"+JCR_SYSTEM) )
        {
            getRootNode().addNode(JCR_SYSTEM, "nt:unstructured");

            // FIXME: Should probably set up all sorts of things.
            save();
        }
    }

    public void removeLockToken(String lt)
    {
        // TODO Auto-generated method stub

    }

    public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException
    {
        synchronized( m_updateList )
        {
            for( NodeImpl ni : m_updateList )
            {
                ni.saveNodeAndChildren();
            }

            m_updateList.clear();
            m_nodeManager.reset();
        }
    }

    public void setNamespacePrefix(String newPrefix, String existingUri) throws NamespaceException, RepositoryException
    {
        // TODO Auto-generated method stub

        throw new UnsupportedRepositoryOperationException("Session.setNamespacePrefix()");
    }

    protected void nodesSaved(List<String> modifications) throws VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        synchronized( m_updateList )
        {
            for( String path : modifications )
            {
                for( NodeImpl nd : m_updateList )
                {
                    if( nd.getPath().equals(path) )
                    {
                        m_updateList.remove(nd);
                        break;
                    }
                }
            }
        }
    }

    public NodeManager getNodeManager()
    {
        return m_nodeManager;
    }

    public boolean itemExists(Path absPath)
        throws RepositoryException
    {
        return hasNode(absPath.toString()) || hasProperty(absPath);
    }

    public ItemImpl getItem(Path path) throws PathNotFoundException, RepositoryException
    {
        return (ItemImpl) getItem( path.toString() );
    }

}
