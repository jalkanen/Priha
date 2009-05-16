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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.xml.parsers.ParserConfigurationException;

import org.priha.core.locks.LockManager;
import org.priha.core.namespace.NamespaceRegistryImpl;
import org.priha.nodetype.QNodeTypeManager;
import org.priha.query.PrihaQueryManager;
import org.priha.util.InvalidPathException;
import org.priha.util.Path;
import org.priha.util.PathFactory;
import org.priha.util.PropertyIteratorImpl;
import org.priha.xml.XMLImport;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class WorkspaceImpl
    implements Workspace
{
    private SessionImpl           m_session;
    private String                m_name;
    private ProviderManager       m_providerManager;
    private QNodeTypeManager.Impl m_nodeTypeManager;
    
    private Logger log = Logger.getLogger(WorkspaceImpl.class.getName());
    
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

    public void clone(String srcWorkspace, String srcAbsPath, String destAbsPath, boolean removeExisting) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException("Workspace.clone()");

    }

    public void copy(String srcAbsPath, String destAbsPath) throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException
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
    
    public void copy(String srcWorkspace, String srcAbsPath, String destAbsPath) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException
    {
        SessionImpl srcSession = getSession().getRepository().login( srcWorkspace ); 
        
        // Superuser does not care about locking, so we'll check it separately.
        
        checkLock(destAbsPath);
        
        boolean isSuper = m_session.setSuper( true );
        try
        {
            copy( srcSession, srcAbsPath, destAbsPath );
            m_session.save();
        }
        finally
        {
            m_session.setSuper( isSuper );
            srcSession.logout();
        }
    }
    
    public void copy(SessionImpl srcSession, String srcAbsPath, String destAbsPath) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException
    {
        if( m_session.hasNode( destAbsPath ) ) throw new ItemExistsException("Destination node already exists!");
        
        NodeImpl srcnode = srcSession.getRootNode().getNode(srcAbsPath);
        
        // System.out.println("Moving "+srcAbsPath+" to "+destAbsPath);

        String newDestPath = destAbsPath;

        LockManager lm = LockManager.getInstance( srcSession.getWorkspace() );
        if( lm.hasChildLock( srcnode.getInternalPath() ) )
            throw new LockException( "Lock on source path prevents copy" );

        Node destnode = m_session.getRootNode().addNode( newDestPath, 
                                                         srcnode.getPrimaryNodeType().getName() );
            
        for( NodeIterator ni = srcnode.getNodes(); ni.hasNext(); )
        {
            Node child = ni.nextNode();

            String relPath = srcnode.getName();

            copy( srcSession, child.getPath(), destAbsPath + "/" + relPath );
        }
        
        for( PropertyIteratorImpl pi = srcnode.getProperties(); pi.hasNext(); )
        {
            PropertyImpl p = pi.nextProperty();

            // newDestPath = destAbsPath + "/" + nd.getName() + "/" +
            // p.getName();
            // System.out.println(" property "+p.getPath()+" ==>
            // "+newDestPath
            // );
            
            if( p.getQName().equals( JCRConstants.Q_JCR_PRIMARYTYPE )) continue;
                
            if( p.getQName().equals( JCRConstants.Q_JCR_UUID) )
            {
                destnode.setProperty( p.getName(), UUID.randomUUID().toString() );
            }
            else
            {
                if( p.getDefinition().isMultiple() )
                {
                    destnode.setProperty( p.getName(), p.getValues() );
                }
                else
                {
                    destnode.setProperty( p.getName(), p.getValue() );
                }
            }
        }
    }

    public String[] getAccessibleWorkspaceNames() throws RepositoryException
    {
        Collection<String> list = m_providerManager.listWorkspaces();

        return list.toArray(new String[0]);
    }

    // FIXME: SuperUserSession leaks.
    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, AccessDeniedException, RepositoryException
    {
        SessionImpl suSession = m_session.getRepository().superUserLogin( m_name );
        
        XMLImport importer = new XMLImport( suSession,true, PathFactory.getPath(m_session,parentAbsPath), uuidBehavior );
        
        return importer;
    }

    public String getName()
    {
        return m_name;
    }

    public NamespaceRegistryImpl getNamespaceRegistry() throws RepositoryException
    {
        return RepositoryImpl.getGlobalNamespaceRegistry();
    }

    public QNodeTypeManager.Impl getNodeTypeManager() throws RepositoryException
    {
        return m_nodeTypeManager;
    }

    public ObservationManager getObservationManager() throws UnsupportedRepositoryOperationException, RepositoryException
    {
        throw new UnsupportedRepositoryOperationException("Workspace.getObservationManager()");
        // TODO Auto-generated method stub
    }

    public PrihaQueryManager getQueryManager() throws RepositoryException
    {
        return new PrihaQueryManager(getSession());
    }

    public SessionImpl getSession()
    {
        return m_session;
    }

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
            log.log( Level.WARNING, "Could not get SAX parser", e );
            throw new RepositoryException("Could not get SAX parser, please check logs.");
        }
        catch (SAXException e)
        {
            log.log( Level.WARNING, "Importing failed", e );
            
            if( e.getException() != null && e.getException() instanceof ItemExistsException )
                throw (ItemExistsException) e.getException();
            
            throw new InvalidSerializedDataException("Importing failed: "+e.getMessage());
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
    // TODO: This should really call the relevant methods in the provider which
    //       would make moves a lot faster.
    public void move(String srcAbsPath, String destAbsPath) throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException
    {
        // Superuser does not care about locking, so we'll check it separately.
        
        checkLock(destAbsPath);

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

    public void restore(Version[] versions, boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException("Workspace.restore()");

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
    public boolean nodeExists(Path path) throws RepositoryException
    {
        return m_providerManager.nodeExists(this, path);
    }

}
