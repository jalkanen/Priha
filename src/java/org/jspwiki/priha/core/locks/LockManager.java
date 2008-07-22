package org.jspwiki.priha.core.locks;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.jcr.Workspace;

import org.jspwiki.priha.core.SessionImpl;
import org.jspwiki.priha.util.InvalidPathException;
import org.jspwiki.priha.util.Path;

public class LockManager
{
    private static HashMap<String,LockManager> m_lockManagers = new HashMap<String,LockManager>();
    
    private HashMap<Path,LockImpl> m_locks = new HashMap<Path,LockImpl>();
    private String m_workspace;
    private Logger log = Logger.getLogger( LockManager.class.getName() );
    public LockManager(Workspace ws)
    {
        m_workspace = ws.getName();
    }
    
    public void addLock( LockImpl lock )
    {
        m_locks.put( lock.getPath(), lock );
    }
    
    /**
     *  Checks if the object at this path holds a lock.
     *  
     *  @param path
     *  @return
     */
    public LockImpl getLock( Path path )
    {
        return m_locks.get(path);
    }
    
    /**
     *  Checks if the object at this path or any object above it
     *  holds a lock.
     *  
     *  @param path
     *  @return
     */
    public LockImpl findLock( Path path )
    {
        boolean deepRequired = false;
        while( !path.isRoot() )
        {
            LockImpl li = m_locks.get( path );
        
            if( li != null )
            {
                if( deepRequired && li.isDeep() )
                    return li;
                else if( !deepRequired )
                    return li;
            }
            
            try
            {
                path = path.getParentPath();
                deepRequired = true;
            }
            catch (InvalidPathException e)
            {
                log.warning("Internal error: "+e.getMessage());
            }
        }
        
        return null;
    }

    public static LockManager getInstance(Workspace ws)
    {
        LockManager lm = m_lockManagers.get(ws.getName());
        
        if( lm == null )
        {
            lm = new LockManager( ws );
            m_lockManagers.put( ws.getName(), lm );
        }
        
        return lm;
    }

    public void expireSessionLocks()
    {
        m_locks.clear();
    }

    public void removeLock(LockImpl lock)
    {
        m_locks.remove(lock.getPath());
    }
}
