package org.jspwiki.priha.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.*;
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
import org.jspwiki.priha.xml.StreamContentHandler;
import org.jspwiki.priha.xml.XMLExport;
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
    
    protected SessionProvider m_provider;

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
    
    protected void markDirty( ItemImpl ii ) throws RepositoryException
    {
        if( ii instanceof NodeImpl )
            m_provider.addNode( (NodeImpl) ii );
        else
            m_provider.putProperty( (NodeImpl) ii.getParent(), (PropertyImpl)ii );
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
        return hasNode( new Path(absPath) );
    }
    
    boolean hasNode( Path absPath )
    {
        return m_provider.nodeExists( absPath );
    }

    boolean hasProperty( Path absPath ) throws RepositoryException
    {
        try
        {
            ItemImpl ii = m_provider.getItem(absPath);
            
            if( !ii.isNode() ) return true;
        }
        catch( PathNotFoundException e) {}
        
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
        XMLExport export = new XMLExport( this );
        
        export.export( absPath, contentHandler, skipBinary, noRecurse );
    }

    public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException
    {
        XMLExport export = new XMLExport( this );

        ContentHandler handler = new StreamContentHandler( out );
        
        try
        {
            export.export( absPath, handler, skipBinary, noRecurse );
        }
        catch (SAXException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
        ItemImpl ii = m_provider.getItem( absPath );
        
        return ii;
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
        return m_provider.findByUUID(uuid);
    }

    public Repository getRepository()
    {
        return m_repository;
    }

    public Node getRootNode() throws RepositoryException
    {
        return (Node) getItem(Path.ROOT);
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

    void refresh( boolean keepChanges, Path path ) throws RepositoryException
    {
        m_provider.refresh( keepChanges, path );
    }
    
    public void refresh(boolean keepChanges) throws RepositoryException
    {
        refresh( keepChanges, Path.ROOT );
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
            
            ni = new NodeImpl( this, "/", rootType, nd, true );

            ni.sanitize();
                
            m_provider.addNode( ni );
            ni.markModified(false);
                
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
        saveNodes( Path.ROOT );
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

    public boolean itemExists( Path absPath )
        throws RepositoryException
    {
        return hasNode(absPath) || hasProperty(absPath);
    }

    public void remove(ItemImpl itemImpl) throws RepositoryException
    {
        m_provider.remove( itemImpl );
    }

    public Collection<PropertyImpl> getReferences(String uuid) throws RepositoryException
    {
        return m_provider.getReferences( uuid );
    }

}
