package org.jspwiki.priha.core.locks;

import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

import org.jspwiki.priha.core.SessionImpl;
import org.jspwiki.priha.util.Path;

public class LockImpl implements Lock
{
    private String      m_workspace;
    private Path        m_lockPath;
    private SessionImpl m_session;
    private String      m_lockToken;
    private boolean     m_isDeep;
    
    public LockImpl( SessionImpl session, Path path, boolean isDeep )
    {
        m_lockPath = path;
        m_session  = session;
        m_workspace = session.getWorkspace().getName();
        m_lockToken = UUID.randomUUID().toString();
        m_isDeep   = isDeep;
    }
    
    public LockImpl( LockImpl orig, SessionImpl session )
    {
        m_lockPath  = orig.m_lockPath;
        m_workspace = orig.m_workspace;
        m_lockToken = orig.m_lockToken;
        
        m_session   = session;
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
            return (Node)m_session.getItem(m_lockPath);
        }
        catch (RepositoryException e)
        {
            // TODO Auto-generated catch block
            // This should never happen, actually.
            e.printStackTrace();
            return null;
        }
    }

    public boolean isDeep()
    {
        return m_isDeep;
    }

    public boolean isLive() throws RepositoryException
    {
        return true;
    }

    public boolean isSessionScoped()
    {
        return m_session != null;
    }

    public void refresh() throws LockException, RepositoryException
    {
        // No timer implemented, so nothing happens.
    }

}
