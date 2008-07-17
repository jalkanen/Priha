package org.jspwiki.priha.core;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.jspwiki.priha.util.InvalidPathException;
import org.jspwiki.priha.util.Path;

public abstract class ItemImpl implements Item
{

    protected Path        m_path;
    protected String      m_name;
    protected SessionImpl m_session;
    protected boolean     m_modified = false;
    protected ItemState   m_state    = ItemState.NEW;
    
    public ItemImpl( SessionImpl session, String path )
    {
        this( session, new Path(path) );
    }
    
    public ItemImpl(SessionImpl session, Path path)
    {
        m_session = session;
        m_path = path;
        m_name = m_path.getLastComponent();
    }

    public ItemImpl(ItemImpl original, SessionImpl session)
    {
        this( session, original.getInternalPath() );
        m_modified = original.m_modified;
        m_state    = original.m_state;
    }

    public ItemState getState()
    {
        return m_state;
    }
    
    public void accept(ItemVisitor visitor) throws RepositoryException
    {
        if( isNode() )
        {
            visitor.visit( (Node) this );
        }
        else
        {
            visitor.visit( (Property) this );
        }
    }

    public Item getAncestor(int depth) throws ItemNotFoundException, AccessDeniedException, RepositoryException
    {
        try
        {
            Path ancestorPath = m_path.getAncestorPath(depth);
            
            return m_session.getItem(ancestorPath.toString());
        }
        catch( InvalidPathException e )
        {
            throw new ItemNotFoundException(e.getMessage());
        }
    }

    public int getDepth() throws RepositoryException
    {
        return m_path.depth();
    }

    public String getName() throws RepositoryException
    {
        return m_name;
    }

    public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException
    {
        try
        {
            Path parentPath = m_path.getParentPath();
        
            NodeImpl parent = (NodeImpl)m_session.getItem(parentPath);
                            
            return parent;
        }
        catch( InvalidPathException e )
        {
            throw new ItemNotFoundException(e.getMessage());
        }
    }

    public Path getInternalPath()
    {
        return m_path;
    }
    
    public String getPath() throws RepositoryException
    {
        return m_path.toString();
    }

    public Session getSession() throws RepositoryException
    {
        return m_session;
    }

    public boolean isModified()
    {
        return m_modified;
    }

    public boolean isNew()
    {
        return m_state == ItemState.NEW;
    }

    public boolean isNode()
    {
        return false;
    }

    public boolean isSame(Item otherItem) throws RepositoryException
    {
        if( m_session.getRepository() == otherItem.getSession().getRepository() )
        {
            if( getPath().equals(otherItem.getPath()) )
            {
                if( isNode() == otherItem.isNode() )
                {
                    if( isNode() )
                    {
                        return true;
                    }
                    else if( getParent().getPath().equals(otherItem.getParent().getPath()) )
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void refresh(boolean keepChanges) throws InvalidItemStateException, RepositoryException
    {
        if( !m_session.itemExists(m_path) ) 
            throw new InvalidItemStateException("You cannot refresh an Item which has been deleted!");
        
        m_session.refresh( keepChanges, m_path );
    }

    public abstract void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException;

    public abstract void save()
                      throws AccessDeniedException,
                          ItemExistsException,
                          ConstraintViolationException,
                          InvalidItemStateException,
                          ReferentialIntegrityException,
                          VersionException,
                          LockException,
                          NoSuchNodeTypeException,
                          RepositoryException
    ;

//    protected abstract void saveItemOnly() throws RepositoryException;

    public String toString()
    {
        return "Node["+m_path.toString()+"]";
    }

    protected void markModified()
    {
        try
        {
            m_modified = true;
            m_session.markDirty(this);
            
            if( !getInternalPath().isRoot() ) ((NodeImpl)getParent()).markModified();
        }
        catch( Exception e ) {} // This is fine.  I guess.
    }

    @Override
    public int hashCode()
    {
        return getInternalPath().hashCode()-17;
    }
    
    /**
     *  Performs mandatory housekeeping right before saving.
     *  @throws RepositoryException
     */
    protected void preSave() throws RepositoryException
    {
    }
    
    /**
     *  Performs mandatory housekeeping after item state has been persisted to disk.
     *
     */
    protected void postSave()
    {
        m_state = ItemState.EXISTS;
        m_modified = false;
    }
}
