package org.jspwiki.priha.providers;

import java.util.List;

import javax.jcr.*;

import org.jspwiki.priha.core.NodeImpl;
import org.jspwiki.priha.core.WorkspaceImpl;

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
    public void open( Repository  rep, 
                      Credentials credentials, 
                      String      workspaceName ) 
        throws RepositoryException,
               NoSuchWorkspaceException
    {
    }
    
    /**
     *  Starts access to a repository.  This is called only when the
     *  repository starts.
     *
     */
    public void start( Repository rep )
    {
    }
    
    public void stop( Repository rep )
    {
    }
    
    public void close( Workspace ws )
    {
    }
    
    /**
     *  This must return a Node with all the metadata filled in, except
     *  for jcr:content (which is optional).
     *  @param ws
     *  @param path
     *  @return
     */
    public abstract NodeImpl getNode( Workspace ws, String path ) throws RepositoryException;
    
    public boolean nodeExists( Workspace ws, String path )
    {
        return false;
    }
    
    public abstract void putNode( Workspace ws, NodeImpl node ) throws RepositoryException;
    
    public void copy( Workspace ws, String srcpath, String destpath ) throws RepositoryException
    {
        
    }
    
    public void move( Workspace ws, String srcpath, String destpath ) throws RepositoryException
    {
        
    }

    public abstract List<String> listNodePaths(Workspace ws);
    
    public abstract List<String> listWorkspaces();
}
