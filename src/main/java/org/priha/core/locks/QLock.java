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
package org.priha.core.locks;

import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

import org.priha.core.NodeImpl;
import org.priha.core.SessionImpl;
import org.priha.path.Path;

public class QLock
{
    private String      m_workspace;
    private Path        m_lockPath;
    private String      m_lockToken;
    private boolean     m_isDeep;
    private boolean     m_isSessionScoped;
    private String      m_owner;
    private boolean     m_live = true;
    
    public QLock( NodeImpl ni, boolean isDeep, boolean isSessionScoped ) throws RepositoryException
    {
        m_lockPath        = ni.getInternalPath();
        m_lockToken       = UUID.randomUUID().toString();
        m_isDeep          = isDeep;
        m_isSessionScoped = isSessionScoped;
        m_workspace       = ni.getSession().getWorkspace().getName();
        m_owner           = ni.getSession().getUserID();
    }
    
    public QLock( QLock orig )
    {
        m_lockPath        = orig.m_lockPath;
        m_workspace       = orig.m_workspace;
        m_lockToken       = orig.m_lockToken;
        m_isSessionScoped = orig.m_isSessionScoped;
        m_isDeep          = orig.m_isDeep;
        m_live            = orig.m_live;
        m_owner           = orig.m_owner;
    }
    
    public String getWorkspace()
    {
        return m_workspace;
    }
    
    public Path getPath()
    {
        return m_lockPath;
    }
    
    public String getToken()
    {
        return m_lockToken;
    }
    
    public String getLockOwner()
    {
        return m_owner;
    }
    

    public boolean isDeep()
    {
        return m_isDeep;
    }

    public boolean isLive() throws RepositoryException
    {
        return m_live;
    }

    public boolean isSessionScoped()
    {
        return m_isSessionScoped;
    }

    public void refresh() throws LockException, RepositoryException
    {
        if( !isLive() ) throw new LockException("Lock is not live");
        
        // No timer implemented, so nothing happens.
    }

    public Impl getLockInstance(SessionImpl session)
    {
        return new Impl(session);
    }
    
    /**
     *  Invalidates the lock in such a way that isLive() returns
     *  false and it can be collected.
     */
    public void invalidate()
    {
        m_live = false;
        m_isSessionScoped = true;
    }
    
    protected void move( Path destPath )
    {
        m_lockPath = destPath;
    }
    
    public String getLockToken(SessionImpl s)
    {
        String[] tokens = s.getLockTokens();
        
        for( String tok : tokens )
        {
            if( m_lockToken.equals(tok) )
                return m_lockToken;
        }
            
        return null;
   
    }
    
    /**
     *  This method checks if this QLock should be expired from the given Session.
     *  
     *  @param s
     *  @return True, if this QLock is expired
     */
    protected boolean expire(SessionImpl s)
    {
        String token = getLockToken(s);

        if( token != null && m_isSessionScoped )
        {
            try
            {
                NodeImpl nd = (NodeImpl)s.getItem( m_lockPath );
                nd.unlock();
            }
            catch( RepositoryException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
                
            return true;
        }
        
        return false;
    }

    /**
     *  The implementation which is per Session.
     *  
     *  @author jalkanen
     *
     */
    public class Impl implements Lock
    {
        private SessionImpl m_session;

        public Impl( SessionImpl session )
        {
            m_session = session;
        }
        
        public QLock getQLock()
        {
            return QLock.this;
        }
        
        public String getLockOwner()
        {
            return QLock.this.getLockOwner();
        }

        public String getLockToken()
        {
            return QLock.this.getLockToken( m_session );
        }

        public Node getNode()
        {
            try
            {
                return (Node)m_session.getItem(m_lockPath);
            }
            catch (PathNotFoundException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (RepositoryException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            return null;
        }

        public boolean isDeep()
        {
            return QLock.this.isDeep();
        }

        public boolean isLive() throws RepositoryException
        {
            return QLock.this.isLive();
        }

        public boolean isSessionScoped()
        {
            return QLock.this.isSessionScoped();
        }

        public void refresh() throws LockException, RepositoryException
        {
            QLock.this.refresh();
        }

    }
}
