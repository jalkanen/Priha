package org.jspwiki.priha.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

import org.jspwiki.priha.nodetype.GenericNodeType;
import org.jspwiki.priha.nodetype.NodeDefinitionImpl;
import org.jspwiki.priha.nodetype.NodeTypeManagerImpl;
import org.jspwiki.priha.providers.RepositoryProvider;
import org.jspwiki.priha.util.InvalidPathException;
import org.jspwiki.priha.util.Path;
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

    /**
     *  Creates a new property implementation without a property definition.
     *  This is meant for providers to create an "empty" property definition,
     *  which the WorkspaceImpl will then update later on.
     *
     *  @param path
     *  @return
     *  @throws RepositoryException
     */
    public PropertyImpl createPropertyImpl( String path )
        throws RepositoryException
    {
        Path p = new Path(path);

        String name = p.getLastComponent();

        p = p.getParentPath();

        //NodeImpl nd = (NodeImpl) m_session.getItem(p);

        //PropertyDefinition pd = ((GenericNodeType)nd.getPrimaryNodeType()).findPropertyDefinition(name);

        PropertyImpl pi = new PropertyImpl( m_session, path, null );

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

    /**
     * Loads the state of a node from the repository.
     *
     * @param path
     * @return A brand new NodeImpl.
     *
     * @throws RepositoryException
     */
    NodeImpl loadNode( String path ) throws RepositoryException
    {
        PropertyList properties = m_provider.getProperties( this, path );

        PropertyImpl primaryType = properties.find( "jcr:primaryType" );

        if( primaryType == null )
            throw new RepositoryException("Repository did not return a primary type for path "+path);

        GenericNodeType type = (GenericNodeType) m_nodeTypeManager.getNodeType( primaryType.getString() );

        NodeDefinition nd = m_nodeTypeManager.findNodeDefinition( primaryType.getString() );

        NodeImpl ni = new NodeImpl( m_session, path, type, nd );

        for( PropertyImpl p : properties )
        {
            String name = p.getName();
            PropertyDefinition pd = ((GenericNodeType)ni.getPrimaryNodeType()).findPropertyDefinition(name);

            p.setDefinition( pd );

            ni.addChildProperty( p );
        }

        return ni;
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
