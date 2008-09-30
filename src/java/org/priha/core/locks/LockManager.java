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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import javax.jcr.Session;
import javax.jcr.Workspace;

import org.priha.util.InvalidPathException;
import org.priha.util.Path;

/**
 *  A LockManager exists per static workspace.  This means that it
 *  manages *all* the locks for a given workspace, regardless of the
 *  Session which accesses it.
 */
public class LockManager
{
    private static HashMap<String,LockManager> m_lockManagers = new HashMap<String,LockManager>();
    
    private HashMap<Path,LockImpl> m_locks = new HashMap<Path,LockImpl>();
    //private String m_workspace;
    private Logger log = Logger.getLogger( LockManager.class.getName() );
    
    private LockManager(Workspace ws)
    {
        //m_workspace = ws.getName();
    }
    
    public synchronized void addLock( LockImpl lock )
    {
        m_locks.put( lock.getPath(), lock );
    }
    
    /**
     *  Checks if the object at this path holds a lock.
     *  
     *  @param path
     *  @return
     */
    public synchronized LockImpl getLock( Path path )
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
    public synchronized LockImpl findLock( Path path )
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

    /**
     *  This method must be used to access a LockManager.
     *  
     *  @param ws
     *  @return
     */
    public static synchronized LockManager getInstance(Workspace ws)
    {
        LockManager lm = m_lockManagers.get(ws.getName());
        
        if( lm == null )
        {
            lm = new LockManager( ws );
            m_lockManagers.put( ws.getName(), lm );
        }
        
        return lm;
    }

    public synchronized void expireSessionLocks( Session session )
    {
        for( Iterator<Map.Entry<Path,LockImpl>> i = m_locks.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry<Path,LockImpl> e = i.next();
            
            LockImpl li = e.getValue();

            if( li.expire(session) )
            {
                i.remove();
            }
        }
    }

    public synchronized void removeLock(LockImpl lock)
    {
        m_locks.remove(lock.getPath());
    }

    /**
     *  Checks if any of the children of this Node hold a lock
     *  to which the session does not hold a key to.
     *  
     *  @param internalPath
     *  @return
     */
    public synchronized boolean hasChildLock(Path internalPath)
    {
        for( Path p : m_locks.keySet() )
        {
            //TODO Slightly unoptimal
            if( internalPath.isParentOf(p) ) 
            {
                LockImpl li = m_locks.get(p);
                
                if( li.getLockToken() == null ) return true;
            }
        }
        
        return false;
    }
}
