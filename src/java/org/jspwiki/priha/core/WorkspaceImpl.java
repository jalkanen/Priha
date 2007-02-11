package org.jspwiki.priha.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

import org.jspwiki.priha.nodetype.NodeTypeManagerImpl;
import org.jspwiki.priha.providers.RepositoryProvider;
import org.jspwiki.priha.util.PropertyList;
import org.xml.sax.ContentHandler;

public class WorkspaceImpl
    implements Workspace
{
    private SessionImpl         m_session;
    private String              m_name;
    private RepositoryProvider  m_provider;
    private NodeTypeManagerImpl m_nodeTypeManager;
    
    public WorkspaceImpl( SessionImpl session, String name, RepositoryProvider provider )
        throws RepositoryException
    {
        m_session  = session;
        m_name     = name;
        m_provider = provider;
        m_nodeTypeManager = NodeTypeManagerImpl.getInstance(this);
    }
        
    public PropertyImpl createPropertyImpl( String path )
    {
        PropertyImpl pi = new PropertyImpl( m_session, path );
        
        return pi;
    }
    
    /**
     *  Writes the state of a node directly to the repository.
     *  
     *  @param node
     *  @throws RepositoryException
     */
    void saveNode( NodeImpl node ) throws RepositoryException
    {
        m_provider.putNode( this, node );
    }
    
    NodeImpl loadNode( String path ) throws RepositoryException
    {
        NodeImpl nd = new NodeImpl( m_session, path );
        
        PropertyList properties = m_provider.getProperties( this, path );
        
        for( PropertyImpl p : properties )
        {
            nd.addChildProperty( p );
        }
        
        return nd;
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
        List<String> list = m_provider.listWorkspaces();
        
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
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException("Workspace.getQueryManager()");
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
    List<String> listNodePaths()
    {
        return m_provider.listNodePaths( this );
    }

    public void logout()
    {
        m_provider.close(this);
    }

    public void removeNode(NodeImpl impl) throws RepositoryException
    {
        m_provider.remove( this, impl.getPath() );
    }

}
