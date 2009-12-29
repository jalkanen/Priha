package org.priha.version;

import javax.jcr.*;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

import org.priha.core.NodeImpl;
import org.priha.core.PropertyImpl;
import org.priha.core.SessionImpl;
import org.priha.core.locks.QLock.Impl;
import org.priha.nodetype.QNodeDefinition;
import org.priha.nodetype.QNodeType;
import org.priha.path.Path;
import org.priha.util.QName;

/**
 *  Provides a common base class for both Version and VersionHistory and disable
 *  a number of write-methods.
 */
public class AbstractVersion extends NodeImpl
{

    public AbstractVersion(SessionImpl session, Path path, QNodeType primaryType, QNodeDefinition def, boolean initDefaults) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        super(session,path,primaryType,def,initDefaults);
    }

    // TODO: I can't find the JCR spec which states this should occur.
    @Override
    public void addMixin(String mixinName)
                                          throws NoSuchNodeTypeException,
                                              VersionException,
                                              ConstraintViolationException,
                                              LockException,
                                              RepositoryException
    {
        throw new ConstraintViolationException("Versions cannot add mixin types.");
    }

    @Override
    public boolean canAddMixin(String mixinName) throws NoSuchNodeTypeException, RepositoryException
    {
        return false;
    }

    @Override
    public void checkout() throws UnsupportedRepositoryOperationException, LockException, RepositoryException
    {
        throw new UnsupportedRepositoryOperationException("A Version node cannot be checked out.");
    }

    @Override
    public Impl getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException
    {
        throw new LockException("A Version node cannot be locked.");
    }

    @Override
    public Lock lock(boolean isDeep, boolean isSessionScoped)
                                                             throws UnsupportedRepositoryOperationException,
                                                                 LockException,
                                                                 AccessDeniedException,
                                                                 InvalidItemStateException,
                                                                 RepositoryException
    {
        throw new LockException("A Version node cannot be locked.");
    }

    @Override
    public NodeIterator merge(String srcWorkspace, boolean bestEffort)
                                                                      throws NoSuchWorkspaceException,
                                                                          AccessDeniedException,
                                                                          MergeException,
                                                                          LockException,
                                                                          InvalidItemStateException,
                                                                          RepositoryException
    {
        throw new ConstraintViolationException("A Version node cannot be merged.");
    }
/*
    @Override
    public void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        if( m_session.isSuper() )
            super.remove();
        
        throw new ConstraintViolationException("A Version node cannot be removed.");
    }
*/
    @Override
    public void removeMixin(String mixinName)
                                             throws NoSuchNodeTypeException,
                                                 VersionException,
                                                 ConstraintViolationException,
                                                 LockException,
                                                 RepositoryException
    {
        if( m_session.isSuper() )
        {
            super.removeMixin(mixinName);
            return;
        }

        throw new ConstraintViolationException("A Version node cannot remove mixins.");
    }

    @Override
    public void restore(String versionName, boolean removeExisting)
                                                                   throws VersionException,
                                                                       ItemExistsException,
                                                                       UnsupportedRepositoryOperationException,
                                                                       LockException,
                                                                       InvalidItemStateException,
                                                                       RepositoryException
    {
        throw new UnsupportedRepositoryOperationException("A Version node cannot be restored.");
    }

    @Override
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
        throw new ConstraintViolationException("A Version node does not support restore().");
    }


    @Override
    public void update(String srcWorkspaceName)
                                               throws NoSuchWorkspaceException,
                                                   AccessDeniedException,
                                                   LockException,
                                                   InvalidItemStateException,
                                                   RepositoryException
    {
        throw new ConstraintViolationException("A Version node cannot be updated.");
    }

    @Override
    protected PropertyImpl prepareProperty(QName name, Object value) throws PathNotFoundException, RepositoryException
    {
        if( m_session.isSuper() )
            return super.prepareProperty(name, value);
        
        throw new ConstraintViolationException("A Version node does not support setProperty().");
    }

    @Override
    protected PropertyImpl prepareProperty(String name, Object value) throws PathNotFoundException, RepositoryException
    {
        if( m_session.isSuper() )
            return super.prepareProperty(name, value);

        throw new ConstraintViolationException("A Version node does not support setProperty().");
    }

    @Override
    public VersionImpl getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException
    {
        if( m_session.isSuper() )
            return super.getBaseVersion();

        throw new UnsupportedRepositoryOperationException("Version nodes do not support getBaseVersion()");
    }

    @Override
    public NodeImpl addNode(String relPath, String primaryNodeTypeName)
                                                                       throws ItemExistsException,
                                                                           PathNotFoundException,
                                                                           VersionException,
                                                                           ConstraintViolationException,
                                                                           LockException,
                                                                           RepositoryException
    {
        if(m_session.isSuper())
            return super.addNode(relPath,primaryNodeTypeName);
        
        throw new ConstraintViolationException();
    }

    @Override
    public NodeImpl addNode(String relPath)
                                           throws ItemExistsException,
                                               PathNotFoundException,
                                               NoSuchNodeTypeException,
                                               LockException,
                                               VersionException,
                                               ConstraintViolationException,
                                               RepositoryException
    {
        if(m_session.isSuper())
            return super.addNode(relPath);
        
        throw new ConstraintViolationException();
    }

}
