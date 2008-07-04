package org.jspwiki.priha.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.Comparator;
import java.util.Iterator;
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
 */
public class SessionImpl implements Session
{
    private static final String JCR_SYSTEM = "jcr:system";
    private RepositoryImpl m_repository;
    private WorkspaceImpl  m_workspace;

    private SimpleCredentials m_credentials;

    private Logger         log = Logger.getLogger( getClass().getName() );
    
    private SessionProvider m_provider;

    public SessionImpl( RepositoryImpl rep, Credentials creds, String name )
        throws RepositoryException
    {
        m_repository = rep;
        m_workspace  = new WorkspaceImpl( this, name, rep.getProviderManager() );
        m_provider   = new SessionProvider( this, rep.getProviderManager() );
        
        if( !hasNode("/"+JCR_SYSTEM) )
        {
            repopulate();
        }
        
        if( creds instanceof SimpleCredentials )
        {
            m_credentials = (SimpleCredentials)creds;
        }
    }

    public List<Path> listNodes( Path parentpath )
    {
        return m_provider.listNodes(parentpath);
    }
    
    protected void markDirty( ItemImpl ii )
    {
        
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
            m_provider.addNode( node );
        }
        catch (InvalidPathException e)
        {
            throw new ItemNotFoundException(e.getMessage());
        }
    }

    boolean hasNode( String absPath )
    {
        return m_provider.nodeExists( new Path(absPath) );
    }

    boolean hasProperty( Path absPath ) throws RepositoryException
    {
        return m_provider.getProperty(absPath) != null;
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

    public ItemImpl getItem( Path absPath ) throws PathNotFoundException, RepositoryException
    {
        try
        {
            ItemImpl ii = m_provider.getProperty(absPath);

            if( ii != null )
            {
                return ii;
            }

            if( absPath.depth() > 0 )
            {
                NodeImpl nd = (NodeImpl)m_provider.getNode( absPath.getParentPath() );

                if( nd != null && nd.hasProperty( absPath.getLastComponent() ) )
                {
                    return (ItemImpl)nd.getChildProperty( absPath.getLastComponent() );
                }
            }
            
            //
            //  Load from repository
            //
            try
            {
                NodeImpl nd = m_workspace.loadNode( absPath );
            
                if( nd != null )
                {
                    return nd;   
                }
            }
            catch( RepositoryException e ) {}
            
            try
            {
                NodeImpl nd = m_workspace.loadNode( absPath.getParentPath() );
            
                if( nd != null && nd.hasProperty( absPath.getLastComponent() ) )
                {
                    return (ItemImpl)nd.getChildProperty( absPath.getLastComponent() );
                }
            }
            catch( RepositoryException e ) {}
        }
        catch( InvalidPathException e )
        {
        }

        throw new PathNotFoundException( absPath.toString() );        
    }
    
    public Item getItem(String absPath) throws PathNotFoundException, RepositoryException
    {
        return getItem( new Path(absPath) );
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
        throw new UnsupportedRepositoryOperationException("No UUID");
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
        return m_provider.hasPendingChanges();
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

        m_provider.clear();

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
                
            m_provider.addNode( ni );
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
        saveNodes( new Path("/") );
    }

    public void setNamespacePrefix(String newPrefix, String existingUri) throws NamespaceException, RepositoryException
    {
        // TODO Auto-generated method stub

        throw new UnsupportedRepositoryOperationException("Session.setNamespacePrefix()");
    }

    /**
     *  Saves all modified nodes that start with the given path prefix. This
     *  can be used to save a node and all its children.
     *  
     *  @param pathprefix
     *  @throws RepositoryException
     */
    protected void saveNodes( Path pathprefix ) throws RepositoryException
    {
        m_provider.save( pathprefix );
    }

    public boolean itemExists(Path absPath)
        throws RepositoryException
    {
        return hasNode(absPath.toString()) || hasProperty(absPath);
    }

}
