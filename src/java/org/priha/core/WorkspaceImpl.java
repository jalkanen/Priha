/*
    Priha - A JSR-170 implementation library.

    Copyright (C) 2007-2009 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Logger;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.observation.ObservationManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.xml.parsers.ParserConfigurationException;

import org.priha.core.locks.LockManager;
import org.priha.core.namespace.NamespaceRegistryImpl;
import org.priha.nodetype.QNodeTypeManager;
import org.priha.path.InvalidPathException;
import org.priha.path.Path;
import org.priha.path.PathFactory;
import org.priha.query.PrihaQueryManager;
import org.priha.util.LazyPropertyIteratorImpl;
import org.priha.xml.XMLImport;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 *  Implements a JCR Workspace.  This class mostly functions as a facade for
 *  ProviderManager, which takes care of the actual repository management.
 */
public class WorkspaceImpl
    implements Workspace
{
    private SessionImpl           m_session;
    private String                m_name;
    private ProviderManager       m_providerManager;
    private QNodeTypeManager.Impl m_nodeTypeManager;
    
    private Logger log = Logger.getLogger(WorkspaceImpl.class.getName());
    
    /**
     *  Create a new Workspace instance.
     *  
     *  @param session The SessionImpl to which this Workspace is tied to
     *  @param name The name of the Workspace
     *  @param mgr The ProviderManager instance which owns the repository.
     *  @throws RepositoryException If a NodeTypeManager cannot be instantiated.
     */
    public WorkspaceImpl( SessionImpl session, String name, ProviderManager mgr )
        throws RepositoryException
    {
        m_session  = session;
        m_name     = name;
        m_providerManager = mgr;
        m_nodeTypeManager = QNodeTypeManager.getManager(this);
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
     *  If removeExisting is true, removes Nodes with conflicting UUIDs.  If it's false,
     *  this exists with a ItemExistsException.
     *  
     *  Does not save the Session.
     *  
     *  @param srcNode
     *  @param removeExisting
     *  @throws RepositoryException
     */
    private void checkUUIDs( NodeImpl srcNode, boolean removeExisting ) throws RepositoryException
    {
        try
        {
            String uuid = srcNode.getUUID();
            
            Node n = getSession().getNodeByUUID( uuid );
            
            if( removeExisting ) 
            {
                 System.out.println("Removing node due to UUID conflict "+n);
                n.remove();
            }
            else
            {
                throw new ItemExistsException("There already exists a Node by UUID "+uuid+" in the destination workspace! "+n.getPath());
            }
        }
        catch( UnsupportedRepositoryOperationException e )
        {
            // Fine, this node just does not have an UUID.
        }
        catch( ItemNotFoundException e )
        {
            // Fine, expected as well.
        }
        
        for( NodeIterator ni = srcNode.getNodes(); ni.hasNext(); )
        {
            checkUUIDs( (NodeImpl)ni.nextNode(), removeExisting );
        }
    }
    
    /**
     *  {@inheritDoc}
     */
    public void clone(String srcWorkspace, String srcAbsPath, String destAbsPath, boolean removeExisting) 
        throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException
    {
        checkLock(destAbsPath);
        getSession().checkWritePermission();
        
        SessionImpl srcSession = getSession().getRepository().login(srcWorkspace);
        
        boolean isSuper = m_session.setSuper( true );
        
        try
        {
            //
            //  Do the check here, since once we're in super mode, addNode() will happily
            //  allow adding to an indexed path.
            //
            if( destAbsPath.endsWith("]") )
                throw new ConstraintViolationException("Cannot clone to a path which ends with an index.");
            
            if( removeExisting && getSession().itemExists( destAbsPath ) )
            {
//                getSession().getItem( destAbsPath ).remove();
                //getSession().save();
            }

            checkUUIDs( (NodeImpl)srcSession.getItem( srcAbsPath ), removeExisting );
            //getSession().save();
            
            copy( srcSession, srcAbsPath, destAbsPath, true );
            
            m_session.save();
        }
        finally
        {
            m_session.setSuper( isSuper );
            srcSession.logout();
        }
    }

    /**
     *  {@inheritDoc}
     */
    public void copy(String srcAbsPath, String destAbsPath) 
        throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, 
               ItemExistsException, LockException, RepositoryException
    {
        copy( getName(), srcAbsPath, destAbsPath );
    }

    /**
     *  Throws a LockException if the given absolute path or any of its child nodes
     *  is locked.
     */
    private void checkLock( String abspath ) throws LockException, RepositoryException
    {
        LockManager lm = LockManager.getInstance( this );
        
        if( lm.findLock( PathFactory.getPath( m_session, abspath ) ) != null )
            throw new LockException("Destination path "+abspath+" is locked and cannot be modified.");
        
    }
    
    /**
     *  {@inheritDoc}
     */
    public void copy(String srcWorkspace, String srcAbsPath, String destAbsPath) 
        throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, 
               AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException
    {
        if( destAbsPath.endsWith("]") )
            throw new ConstraintViolationException("Destination path must not end with an index.");
        
        SessionImpl srcSession = getSession().getRepository().login( srcWorkspace ); 
        
        // Superuser does not care about locking, so we'll check it separately.
        
        checkLock(destAbsPath);
        getSession().checkWritePermission();

        boolean isSuper = m_session.setSuper( true );
        try
        {
            copy( srcSession, srcAbsPath, destAbsPath, false );
            m_session.save();
        }
        finally
        {
            m_session.setSuper( isSuper );
            srcSession.logout();
        }
    }
    
    /**
     *  Checks if the node or any of its parents are checked in.
     *  
     *  @param n
     *  @return
     *  @throws RepositoryException
     */
    protected boolean isCheckedIn( NodeImpl n ) throws RepositoryException
    {
        if( !n.isCheckedOut() ) return true;
        
        if( n.getInternalPath().isRoot() ) return false;
        
        return isCheckedIn( n.getParent() );
    }
    
    /**
     *  Performs the actual copy, but does not save.
     *  
     *  @param srcSession
     *  @param srcAbsPath
     *  @param destAbsPath
     *  @throws NoSuchWorkspaceException
     *  @throws ConstraintViolationException
     *  @throws VersionException
     *  @throws AccessDeniedException
     *  @throws PathNotFoundException
     *  @throws ItemExistsException
     *  @throws LockException
     *  @throws RepositoryException
     */
    protected void copy(SessionImpl srcSession, String srcAbsPath, String destAbsPath, boolean preserveUUIDs ) 
        throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, 
               PathNotFoundException, ItemExistsException, LockException, RepositoryException
    {
        Path path = PathFactory.getPath( getSession(), destAbsPath );

        if( isCheckedIn( (NodeImpl)m_session.getItem(path.getParentPath()) ) )
            throw new VersionException("Versioned node checked in");

        if( m_session.hasNode( destAbsPath ) ) 
        {
            NodeImpl nx = getSession().getRootNode().getNode(destAbsPath);
            
            if( !nx.getDefinition().allowsSameNameSiblings() )
                throw new ItemExistsException("Destination node "+destAbsPath+" already exists, and does not allow same name siblings!");
        }
        
        NodeImpl srcnode = srcSession.getRootNode().getNode(srcAbsPath);
        
        // System.out.println("Moving "+srcAbsPath+" to "+destAbsPath);

        String newDestPath = destAbsPath;

        LockManager lm = LockManager.getInstance( srcSession.getWorkspace() );
        if( lm.hasChildLock( srcnode.getInternalPath() ) )
            throw new LockException( "Lock on source path prevents copy" );
        
        NodeImpl destnode = m_session.getRootNode().addNode( newDestPath, 
                                                             srcnode.getPrimaryNodeType().getName() );
        
        if( isCheckedIn( destnode ) )
        {
            throw new VersionException("Destination node "+destnode.getPath()+" (or one of its parent nodes) is checked in, and therefore unmodifiable.");
        }
        
        for( NodeIterator ni = srcnode.getNodes(); ni.hasNext(); )
        {
            Node child = ni.nextNode();

            String relPath = child.getName();

            copy( srcSession, child.getPath(), destAbsPath + "/" + relPath, preserveUUIDs );
        }
        
        for( LazyPropertyIteratorImpl pi = srcnode.getProperties(); pi.hasNext(); )
        {
            PropertyImpl p = pi.nextProperty();

            // newDestPath = destAbsPath + "/" + nd.getName() + "/" +
            // p.getName();
            // System.out.println(" property "+p.getPath()+" ==>
            // "+newDestPath
            // );
            
            //
            // The primary type is already set above upon the creation of the Node.
            //
            
            if( p.getQName().equals( JCRConstants.Q_JCR_PRIMARYTYPE )) continue;
                
            //
            // Section 7.1.7; the new node must get a new UUID when copying;
            // but we allow UUIDs to be preserved for e.g. clone.
            //
            
            if( p.getQName().equals( JCRConstants.Q_JCR_UUID) && !preserveUUIDs )
            {
                destnode.setProperty( p.getName(), UUID.randomUUID().toString() );
            }
            else
            {
                //
                //  Copy the property
                //
                
                if( p.getDefinition().isMultiple() )
                {
                    PropertyImpl pix = destnode.internalSetProperty( p.getQName(), p.getValues(), p.getType() );
                    pix.enterState( ItemState.NEW );
                }
                else
                {
                    destnode.setProperty( p.getName(), p.getValue() );
                }
            }
        }
    }

    /**
     *  {@inheritDoc}
     */
    public String[] getAccessibleWorkspaceNames() throws RepositoryException
    {
        Collection<String> list = m_providerManager.listWorkspaces();

        return list.toArray(new String[0]);
    }

    /**
     *  {@inheritDoc}
     */
    // FIXME: SuperUserSession leaks.
    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, AccessDeniedException, RepositoryException
    {
        SessionImpl suSession = m_session.getRepository().superUserLogin( m_name );
        
        XMLImport importer = new XMLImport( suSession,true, PathFactory.getPath(m_session,parentAbsPath), uuidBehavior );
        
        return importer;
    }

    /**
     *  {@inheritDoc}
     */
    public String getName()
    {
        return m_name;
    }

    /**
     *  {@inheritDoc}
     */
    public NamespaceRegistryImpl getNamespaceRegistry() throws RepositoryException
    {
        return RepositoryImpl.getGlobalNamespaceRegistry();
    }

    /**
     *  {@inheritDoc}
     */
    public QNodeTypeManager.Impl getNodeTypeManager() throws RepositoryException
    {
        return m_nodeTypeManager;
    }

    /**
     *  Unsupported at the moment.
     */
    public ObservationManager getObservationManager() throws UnsupportedRepositoryOperationException, RepositoryException
    {
        throw new UnsupportedRepositoryOperationException("Workspace.getObservationManager()");
        // TODO Auto-generated method stub
    }

    /**
     *  {@inheritDoc}
     */
    public PrihaQueryManager getQueryManager() throws RepositoryException
    {
        return new PrihaQueryManager(getSession());
    }

    /**
     *  {@inheritDoc}
     */
    public SessionImpl getSession()
    {
        return m_session;
    }

    /**
     *  {@inheritDoc}
     */
    public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, InvalidSerializedDataException, LockException, AccessDeniedException, RepositoryException
    {
        SessionImpl suSession = m_session.getRepository().superUserLogin( m_name );
        
        XMLImport importer = new XMLImport( suSession, true, PathFactory.getPath(m_session,parentAbsPath), uuidBehavior );
        
        try
        {
            importer.doImport( in );
        }
        catch (ParserConfigurationException e)
        {
            throw new RepositoryException("Could not get SAX parser!",e);
        }
        catch (SAXException e)
        {            
            if( e.getException() != null 
                && (e.getException() instanceof RepositoryException) )
            {
                throw (RepositoryException) e.getException();
            }
            
            throw new InvalidSerializedDataException("Importing failed",e);
        }
        finally
        {
            suSession.logout();
        }
        
    }

    /**
     *  Implemented simply by starting a new Session, which then performs the copy, 
     *  and then calling save() on it. 
     */
    public void move(String srcAbsPath, String destAbsPath) throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException
    {
        // Superuser does not care about locking, so we'll check it separately.

        checkLock(destAbsPath);
        getSession().checkWritePermission();

        if( destAbsPath.endsWith("]") )
            throw new ConstraintViolationException("Cannot move to a path with an index in the name");
        
        SessionImpl s = m_session.getRepository().superUserLogin( getName() );
        
        try
        {
            s.move( srcAbsPath, destAbsPath );

            s.save();
        }
        finally
        {
            s.logout();
        }
    }

    /**
     *  Unsupported at the moment.
     */
    public void restore(Version[] versions, boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException("Workspace.restore()");

    }
    
    /**
     *  Performs a logout; to be called by SessionImpl only.
     */
    public void logout()
    {
        m_providerManager.close(this);
    }

    /**
     *  Checks directly from the repository if an item exists.
     *  
     *  @param path
     *  @return
     * @throws InvalidPathException 
     */
    public boolean nodeExists(Path path) throws RepositoryException
    {
        return m_providerManager.nodeExists(this, path);
    }

}
