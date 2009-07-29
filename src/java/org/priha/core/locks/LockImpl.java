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
package org.priha.core.locks;

import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

import org.priha.core.NodeImpl;
import org.priha.core.SessionImpl;
import org.priha.util.Path;

public class LockImpl implements Lock
{
    private String      m_workspace;
    private Path        m_lockPath;
    private SessionImpl m_session;
    private String      m_lockToken;
    private boolean     m_isDeep;
    private boolean     m_isSessionScoped;
    
    public LockImpl( SessionImpl session, Path path, boolean isDeep, boolean isSessionScoped )
    {
        m_lockPath        = path;
        m_session         = session;
        m_workspace       = session.getWorkspace().getName();
        m_lockToken       = UUID.randomUUID().toString();
        m_isDeep          = isDeep;
        m_isSessionScoped = isSessionScoped;
    }
    
    public LockImpl( LockImpl orig, SessionImpl session )
    {
        m_lockPath        = orig.m_lockPath;
        m_workspace       = orig.m_workspace;
        m_lockToken       = orig.m_lockToken;
        m_isSessionScoped = orig.m_isSessionScoped;
        
        m_session         = session;
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
        return m_session.getUserID();
    }
    
    public String getLockToken()
    {
        if(m_session == null) return null;
        
        String[] tokens = m_session.getLockTokens();
        
        for( String tok : tokens )
        {
            if( m_lockToken.equals(tok) )
                return m_lockToken;
        }
        
        return null;
    }

    public Node getNode()
    {
        try
        {
            if( m_session != null )
            {
                return (Node)m_session.getItem(m_lockPath);
            }
        }
        catch (RepositoryException e)
        {
            // TODO Auto-generated catch block
            // This should never happen, actually.
            e.printStackTrace();
        }
        return null;
    }

    public boolean isDeep()
    {
        return m_isDeep;
    }

    public boolean isLive() throws RepositoryException
    {
        return m_session != null || (m_session == null && !m_isSessionScoped);
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

    /**
     *  This method checks if this LockImpl should be expired from the given Session.
     *  
     *  @param s
     *  @return True, if this LockImpl is expired
     */
    protected boolean expire(SessionImpl s)
    {
        if( m_session == s )
        {
            m_session = null;
            if( m_isSessionScoped )
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
        }
        
        return false;
    }
    
    /**
     *  Invalidates the lock in such a way that isLive() returns
     *  false and it can be collected.
     */
    public void invalidate()
    {
        m_session = null;
        m_isSessionScoped = true;
    }
}
