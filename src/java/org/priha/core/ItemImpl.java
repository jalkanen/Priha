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

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.priha.path.InvalidPathException;
import org.priha.path.Path;
import org.priha.path.PathFactory;
import org.priha.path.PathRef;
import org.priha.util.*;

/**
 *  Provides a basic implementation for Items.  An Item stores a reference
 *  to its Path, the Session, and keeps a record of its ItemState.  Therefore
 *  it's a fairly lightweight object.
 */
public abstract class ItemImpl implements Item
{

    protected PathRef           m_path;
    protected final SessionImpl m_session;
    protected boolean           m_isNew;
    
    /**
     *  Create an Item for a particular session and path.
     *  
     *  @param session Session which owns this Item
     *  @param path Path at which the Item is created.
     */
    public ItemImpl(SessionImpl session, Path path)
    {
        m_session = session;
        m_path = session.getPathManager().getPathRef(path);
    }

    /**
     *  Returns the path reference object.
     *  
     *  @return The internal path reference.
     */
    public PathRef getPathReference()
    {
        if( m_path == null ) throw new RuntimeException("Path reference must not be null!");
        return m_path;
    }
    
    /**
     *  Return the current state of this Item.
     *  
     *  @return An {@link ItemState} representing the state.
     */
    public ItemState getState()
    {
        //
        //  Check if the state exists in the change list.
        //
        ItemState state = ItemState.UNDEFINED;
        try
        {
            state = m_session.m_provider.getState( m_path );
        }
        catch( PathNotFoundException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        // If this object exists, but is not in the change list, then it's just
        // a simply existing object
        return state != null ? state : ItemState.EXISTS;
    }
    
    /**
     *  Changes the {@link ItemState} of the Item, and places it in the appropriate
     *  queues.
     *  
     *  @param state New state.
     *  @throws RepositoryException If the state cannot be entered for some reason.
     */
    @SuppressWarnings("fallthrough")
    public void enterState( ItemState state ) throws RepositoryException
    {
        ItemState oldState = getState();
        SessionProvider sp = m_session.m_provider;
        
        switch( state )
        {
            case EXISTS:
                break;
                    
            case NEW:
                if( oldState == ItemState.NEW ) 
                    break;
                
                m_isNew = true;
                /* FALLTHROUGH OK */
            case REMOVED:
                // FIXME: Slightly hacky, this.
                if( state == ItemState.REMOVED && oldState == ItemState.REMOVED ) 
                    break;
                
            case MOVED:
                //
                //  Additions and removals also affect the parent.
                //
                sp.m_changedItems.add( state, this );
                
                Path path = getInternalPath();
                if( !path.isRoot() )
                {
                    //
                    //  We try to avoid calling getParent(), since it causes hits to the database.
                    //
                    Path parent = getInternalPath().getParentPath();
                    
                    if( sp.getState(parent) == null )
                    {
                        getParent().enterState(ItemState.UPDATED);
                    }
                }
                break;
                    
            case UPDATED:
                if( oldState == ItemState.EXISTS || oldState == ItemState.UNDEFINED )
                {
                    // If we're already modified, no use repeating that information
                    sp.m_changedItems.add( state, this );
                }
                break;
                    
            case UNDEFINED:
            default:
                throw new InvalidItemStateException("State cannot be set to UNDEFINED - that is the starting state of any Item only.");
        }
    }
    
    /**
     *  {@inheritDoc}
     */
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

    /**
     *  {@inheritDoc}
     */
    public Item getAncestor(int depth) throws ItemNotFoundException, AccessDeniedException, RepositoryException
    {
        try
        {
            Path ancestorPath = getInternalPath().getAncestorPath(depth);
            
            return m_session.getItem(ancestorPath);
        }
        catch( InvalidPathException e )
        {
            throw new ItemNotFoundException(e.getMessage());
        }
    }

    /**
     *  {@inheritDoc}
     */
    public int getDepth() throws RepositoryException
    {
        return getInternalPath().depth();
    }

    /**
     *  {@inheritDoc}
     */
    public String getName() throws RepositoryException
    {
        return m_session.fromQName( getInternalPath().getLastComponent() );
    }

    /**
     *  Returns the QName of this item.
     *  
     *  @return A QName.
     *  @throws NamespaceException If the namespace cannot be parsed.
     *  @throws RepositoryException If something else goes wrong.
     */
    public QName getQName() throws NamespaceException, RepositoryException
    {
        QName qname = getInternalPath().getLastComponent();
        
        return qname;
    }
    
    /**
     *  {@inheritDoc}
     */
    public NodeImpl getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException
    {
        try
        {
            Path parentPath = getInternalPath().getParentPath();
        
            NodeImpl parent = (NodeImpl)m_session.getItem(parentPath);
                            
            return parent;
        }
        catch( InvalidPathException e )
        {
            throw new ItemNotFoundException(e.getMessage());
        }
    }

    /**
     *  Get the internal path representation for this Item.
     *  
     *  @return The internal Path.
     */
    public Path getInternalPath()
    {
        try
        {
            return m_session.getPath( m_path );
        }
        catch( PathNotFoundException e )
        {
            throw new RuntimeException("Invalid path received: "+m_path,e);
        }
    }
    
    /**
     *  {@inheritDoc}
     */
    public String getPath() throws RepositoryException
    {
        return PathFactory.getMappedPath( m_session, getInternalPath() );
    }

    /**
     *  {@inheritDoc}
     */
    public SessionImpl getSession() throws RepositoryException
    {
        return m_session;
    }

    /**
     *  {@inheritDoc}
     */
    public boolean isModified()
    {
        //
        //  An Item is not modified, if it's, well, not modified or it's been recently
        //  added (then it's NEW).
        //
        ItemState state = getState();
        return state != ItemState.EXISTS && state != ItemState.NEW;
    }

    /**
     *  {@inheritDoc}
     */
    public boolean isNew()
    {
        return m_isNew || getState() == ItemState.UNDEFINED;
    }

    /**
     *  {@inheritDoc}
     */
    public boolean isNode()
    {
        return false;
    }

    /**
     *  {@inheritDoc}
     */
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

   /**
    *  {@inheritDoc}
    */
    public void refresh(boolean keepChanges) throws InvalidItemStateException, RepositoryException
    {
        if( !m_session.itemExists(getInternalPath()) ) 
            throw new InvalidItemStateException("You cannot refresh an Item which has been deleted!");
        
        if( getState() == ItemState.REMOVED || getState() == ItemState.MOVED )
            throw new InvalidItemStateException("Node has been removed");
        
        m_session.refresh( keepChanges, getInternalPath() );
    }

    /**
     *  {@inheritDoc}
     */
    public abstract void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException;

    /**
     *  {@inheritDoc}
     */
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


    /**
     *  Returns a human-readable description of the Item.
     *  
     *  @return Something readable.
     */
    @Override
    public String toString()
    {
        try
        {
            return "Node["+m_session.getWorkspace().getName()+":"+getPath()+"]";
        }
        catch( RepositoryException e )
        {
            return "Node["+m_session.getWorkspace().getName()+":"+getInternalPath()+"]";            
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return getInternalPath().hashCode()-17;
    }
    
    /**
     *  Performs mandatory housekeeping right before saving.
     *  @throws RepositoryException If something goes wrong
     */
    protected void preSave() throws RepositoryException
    {
        if( getState() == ItemState.UNDEFINED )
        {
            throw new IllegalStateException("Node "+getInternalPath()+" must not be in UNDEFINED state at this point (this is a bug in Priha, please report!)");
        }
    }
    
    /**
     *  Performs mandatory housekeeping after item state has been persisted to disk.
     *
     */
    protected void postSave()
    {
        m_isNew = false;
    }
    
    private transient long           m_creationTime = System.currentTimeMillis();

    /**
     *  Returns the creation time of this Item instance.
     *  
     *  @return The creation time.
     */
    public long getCreationTime()
    {
        return m_creationTime;
    }
}
