package org.jspwiki.priha.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

import org.jspwiki.priha.nodetype.NodeTypeManagerImpl;
import org.jspwiki.priha.query.PrihaQueryManager;
import org.jspwiki.priha.util.InvalidPathException;
import org.jspwiki.priha.util.Path;
import org.xml.sax.ContentHandler;

public class WorkspaceImpl
    implements Workspace
{
    private SessionImpl         m_session;
    private String              m_name;
    private ProviderManager     m_providerManager;
    private NodeTypeManagerImpl m_nodeTypeManager;

    public WorkspaceImpl( SessionImpl session, String name, ProviderManager mgr )
        throws RepositoryException
    {
        m_session  = session;
        m_name     = name;
        m_providerManager = mgr;
        m_nodeTypeManager = NodeTypeManagerImpl.getInstance(this);
    }

    /**
     *  Creates a new property implementation without a property definition.
     *  This is meant for providers to create an "empty" property definition,
     *  which the WorkspaceImpl will then update later on.
     *
     *  @param path
     *  @return
     *  @throws RepositoryException
     */
    public PropertyImpl createPropertyImpl( Path path )
        throws RepositoryException
    {
        // String name = path.getLastComponent();

        //NodeImpl nd = (NodeImpl) m_session.getItem(p);

        //PropertyDefinition pd = ((GenericNodeType)nd.getPrimaryNodeType()).findPropertyDefinition(name);

        PropertyImpl pi = new PropertyImpl( m_session, path, null );

        return pi;
    }

    /**
     *  Writes the state of a node directly to the repository.
     *
     *  @param item
     *  @throws RepositoryException
     */
    /*
    void saveItem( ItemImpl item ) throws RepositoryException
    {
        if( item instanceof NodeImpl )
            m_providerManager.addNode( this, (NodeImpl)item );
        else
            m_providerManager.putProperty( this, (PropertyImpl) item ); 
    }
*/
    /**
     * Loads the state of a node from the repository.
     *
     * @param path
     * @return A brand new NodeImpl.
     *
     * @throws RepositoryException
     */
    NodeImpl loadNode( Path path ) throws RepositoryException
    {
        return m_providerManager.loadNode(this, path);
    }

    public void clone(String srcWorkspace, String srcAbsPath, String destAbsPath, boolean removeExisting) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException("Workspace.clone()");

    }

    public void copy(String srcAbsPath, String destAbsPath) throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException("Workspace.copy()");

    }

    public void copy(String srcWorkspace, String srcAbsPath, String destAbsPath) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException("Workspace.copy2()");

    }

    public String[] getAccessibleWorkspaceNames() throws RepositoryException
    {
        Collection<String> list = m_providerManager.listWorkspaces();

        return list.toArray(new String[0]);
    }

    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, AccessDeniedException, RepositoryException
    {
        throw new UnsupportedRepositoryOperationException("Workspace.getImportContentHandler()");
        // TODO Auto-generated method stub
    }

    public String getName()
    {
        return m_name;
    }

    public NamespaceRegistry getNamespaceRegistry() throws RepositoryException
    {
        return ((RepositoryImpl)m_session.getRepository()).getGlobalNamespaceRegistry();
    }

    public NodeTypeManager getNodeTypeManager() throws RepositoryException
    {
        if( m_nodeTypeManager == null )
            m_nodeTypeManager = NodeTypeManagerImpl.getInstance(this);

        return m_nodeTypeManager;
    }

    public ObservationManager getObservationManager() throws UnsupportedRepositoryOperationException, RepositoryException
    {
        throw new UnsupportedRepositoryOperationException("Workspace.getObservationManager()");
        // TODO Auto-generated method stub
    }

    public QueryManager getQueryManager() throws RepositoryException
    {
        return new PrihaQueryManager();
    }

    public Session getSession()
    {
        return m_session;
    }

    public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, InvalidSerializedDataException, LockException, AccessDeniedException, RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException("Workspace.importXML()");

    }

    public void move(String srcAbsPath, String destAbsPath) throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException("Workspace.move()");

    }

    public void restore(Version[] versions, boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException("Workspace.restore()");

    }

    /**
     *  Goes to the repository and lists all available Nodes with their paths.
     *  @return
     */
    List<Path> listNodePaths()
    {
        return listNodePaths( Path.ROOT );
    }

    private List<Path> listNodePaths(Path path)
    {
        List<Path> ls = m_providerManager.listNodes( this, path );
        List<Path> result = new ArrayList<Path>();
        
        result.addAll( ls );
        
        for( Path p : ls )
        {
            result.addAll( listNodePaths(p) );
        }
        
        return result;
    }
    
    public void logout()
    {
        m_providerManager.close(this);
    }

    public void removeItem(ItemImpl impl) throws RepositoryException
    {
        m_providerManager.remove( this, impl.getInternalPath() );
    }

    /**
     *  Checks directly from the repository if an item exists.
     *  
     *  @param path
     *  @return
     * @throws InvalidPathException 
     */
    public boolean nodeExists(Path path) throws InvalidPathException
    {
        return m_providerManager.hasNode(this, path);
    }

}
