package org.jspwiki.priha.providers;

import java.util.List;

import javax.jcr.*;

import org.jspwiki.priha.core.NodeImpl;
import org.jspwiki.priha.core.PropertyImpl;
import org.jspwiki.priha.core.RepositoryImpl;
import org.jspwiki.priha.core.WorkspaceImpl;
import org.jspwiki.priha.util.PropertyList;

/**
 *  A few ground rules:
 *  <ul>
 *  <li>A RepositoryProvider shall not cache the Session object</li>
 *  </ul>
 *  
 *  @author jalkanen
 *
 */
public abstract class RepositoryProvider
{
    /**
     *  Opens a repository.  Called only once when the Repository is
     *  created.
     *  
     * @param rep
     * @param credentials
     * @param workspaceName
     * @return
     * @throws NoSuchWorkspaceException, if no such workspace exists 
     */
    public void open( RepositoryImpl  rep, 
                      Credentials     credentials, 
                      String          workspaceName ) 
        throws RepositoryException,
               NoSuchWorkspaceException
    {
    }
    
    /**
     *  Starts access to a repository.  This is called only when the
     *  repository starts.
     *
     */
    public void start( RepositoryImpl rep )
    {
    }
    
    public void stop( RepositoryImpl rep )
    {
    }
    
    public void close( WorkspaceImpl ws )
    {
    }
    
    /**
     *  Returns a list of properties for a Node.
     *  
     *  @param ws
     *  @param path
     *  @return
     *  @throws RepositoryException
     */
    public abstract PropertyList getProperties( WorkspaceImpl ws, String path ) throws RepositoryException;
    
    public abstract PropertyImpl getProperty( WorkspaceImpl ws, String path ) throws RepositoryException;
    
    public boolean nodeExists( WorkspaceImpl ws, String path )
    {
        return false;
    }
    
    public abstract void putNode( WorkspaceImpl ws, NodeImpl node ) throws RepositoryException;
    
    public void copy( WorkspaceImpl ws, String srcpath, String destpath ) throws RepositoryException
    {
        
    }
    
    public void move( WorkspaceImpl ws, String srcpath, String destpath ) throws RepositoryException
    {
        
    }

    public abstract List<String> listNodePaths(WorkspaceImpl ws);
    
    public abstract List<String> listWorkspaces();

    public abstract void remove( WorkspaceImpl ws, String path );
}
