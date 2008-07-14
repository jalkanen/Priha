package org.jspwiki.priha.providers;

import java.util.Collection;
import java.util.List;

import javax.jcr.*;

import org.jspwiki.priha.core.*;
import org.jspwiki.priha.core.values.ValueImpl;
import org.jspwiki.priha.util.Path;
import org.jspwiki.priha.util.PropertyList;

/**
 *  A few ground rules:
 *  <ul>
 *  <li>A RepositoryProvider shall not cache the Session object</li>
 *  <li>A RepositoryProvider shall be thread-safe</li>
 *  </ul>
 *  
 *  @author jalkanen
 *
 */
public interface RepositoryProvider
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
               NoSuchWorkspaceException;
    
    /**
     *  Starts access to a repository.  This is called only when the
     *  repository starts.
     *
     */
    public void start( RepositoryImpl rep );
    
    /**
     *  Stops a given repository.  This is called whenever the repository
     *  can go safely to sleep.
     *  
     *  @param rep
     */
    public void stop( RepositoryImpl rep );
    
    /**
     *  The repository will no longer be used.  This may be called without a preceding call
     *  to stop().
     *  
     *  @param ws
     */
    public void close( WorkspaceImpl ws );
    
    /**
     *  Returns a list of properties for a Node.
     *  
     *  @param ws
     *  @param path
     *  @return
     *  @throws RepositoryException
     */
    public abstract List<String> listProperties( WorkspaceImpl ws, Path path ) throws RepositoryException;
    
    /**
     *  Returns the value of a property.
     *  
     * @param ws
     * @param path
     * @return either a ValueImpl or ValueImpl[], depending on whether this is a multi-valued thing
     * @throws RepositoryException
     */
    public abstract Object getPropertyValue( WorkspaceImpl ws, Path path ) throws RepositoryException;
    
    /**
     *  Returns true, if a property exists.
     *  
     *  @param ws
     *  @param path
     *  @return
     */
    public boolean nodeExists( WorkspaceImpl ws, Path path );
    
    /**
     * Adds a new Node to the repository to the given Path.
     * 
     * @param ws
     * @param node
     * @throws RepositoryException
     */
    public void addNode( WorkspaceImpl ws, Path path ) throws RepositoryException;
    
    /**
     * Sets or adds a new Property to the repository.
     * 
     */
    
    public void putPropertyValue( WorkspaceImpl ws, PropertyImpl property ) throws RepositoryException;
    
    /**
     * Copies content from one path to another path.
     * @param ws
     * @param srcpath
     * @param destpath
     * @throws RepositoryException
     */
    public void copy( WorkspaceImpl ws, Path srcpath, Path destpath ) throws RepositoryException;
    
    /**
     *  Moves the content at the end of one Path to the destpath.
     *  
     * @param ws
     * @param srcpath
     * @param destpath
     * @throws RepositoryException
     */
    public void move( WorkspaceImpl ws, Path srcpath, Path destpath ) throws RepositoryException;

    /**
     *  Lists all the Nodes from the repository which belong to this parent.
     *  
     *  @param ws
     *  @param parentpath
     *  @return
     */
    public List<Path> listNodes(WorkspaceImpl ws, Path parentpath);
    
    /**
     *  Lists all workspaces which are available in this Repository.
     *  
     *  @return
     */
    public Collection<String> listWorkspaces();

    /**
     *  Removes a node or a property from the repository.
     *  
     *  @param ws
     *  @param path
     */
    public void remove( WorkspaceImpl ws, Path path )  throws RepositoryException;
    
    /**
     *  If an item by this UUID exists, returns a Path.
     *  
     * @param ws
     * @param uuid
     * @return
     * @throws ItemNotFoundException If the repository does not contain an UUID by this name.
     */
    public Path findByUUID( WorkspaceImpl ws, String uuid ) throws RepositoryException;

    /**
     *  Finds all the Property paths which are of type REFERENCE and whose content
     *  is equal to the UUID given.
     *  
     *  @param ws
     *  @param uuid
     *  @return
     *  @throws RepositoryException 
     */
    public List<Path> findReferences(WorkspaceImpl ws, String uuid) throws RepositoryException;
}
