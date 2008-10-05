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
package org.priha.providers;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

import javax.jcr.*;
import javax.xml.namespace.QName;

import org.priha.core.PropertyImpl;
import org.priha.core.RepositoryImpl;
import org.priha.core.WorkspaceImpl;
import org.priha.util.ConfigurationException;
import org.priha.util.Path;

/**
 *  A few ground rules:
 *  <ul>
 *  <li>A RepositoryProvider shall not cache the Session object</li>
 *  <li>A RepositoryProvider shall be thread-safe</li>
 *  <li>There shall always be a default workspace called "default"</li>
 *  </ul>
 *
 *  <p>
 *  The RepositoryProvider lifecycle is as follows.
 *  <ol>
 *  <li>When Priha first starts up and parses its configuration, it locates each
 *      RepositoryProvider from the configuration, and calls its <code>start()</code>
 *      method to notify that it exists.</li>
 *  <li>Once the user then grabs a Session object, the RepositoryProvider is
 *      notified via the <code>open()</code> method.</li>
 *  <li>The user now uses all of the other methods from the repo.</li>
 *  <li>Once the user chooses to logout, then the <code>close()</code> method
 *      is called.</li>
 *  <li>Then, finally, when Priha closes down, the <code>stop()</code> method
 *      will be called.  Priha installs its own shutdown hook for this case.</li>
 *  </ol>
 */
public interface RepositoryProvider
{
    /**
     *  Opens a repository.  Called whenever a session login() is performed.
     *  
     * @param rep The Repository which owns this Provider.
     * @param credentials The Credentials object passed to the Session.open() call.  May be null,
     *                    if there were no credentials.
     * @param workspaceName The workspace which will be accessed.
     * @throws NoSuchWorkspaceException, if no such workspace exists. 
     */
    public void open( RepositoryImpl  rep, 
                      Credentials     credentials, 
                      String          workspaceName ) 
        throws RepositoryException,
               NoSuchWorkspaceException;
    
    /**
     *  Starts access to a repository.  This is called only once per
     *  RepositoryProvider lifecycle.
     *  
     *  @param repository The Repository which owns this provider.
     *  @param properties A set of filtered properties for this provider.
     *  @throws ConfigurationException If the repository cannot be started due to a faulty configuration.
     * @throws  
     *  
     *  @see org.priha.core.ProviderManager#filterProperties(RepositoryImpl, String)
     */
    public void start( RepositoryImpl repository, 
                       Properties     properties ) throws ConfigurationException;
    
    /**
     *  Stops a given repository.  This may be called without a preceding call
     *  to stop().  All allocated resources can now be deallocated.
     *  
     *  @param rep The Repository object.
     */
    public void stop( RepositoryImpl rep );
    
    /**
     *  The repository will no longer be used by a session, so any session-specific
     *  things can now be deallocated.
     *  
     *  @param ws The Workspace attached to the Session.
     */
    public void close( WorkspaceImpl ws );
    
    /**
     *  Returns a list of properties for a Node.
     *  
     *  @param ws The Workspace in which the properties should be located.
     *  @param path The path of the Node.
     *  @return A List of the names of the properties under this Node.
     *  @throws RepositoryException If something goes wrong.
     */
    public abstract List<QName> listProperties( WorkspaceImpl ws, Path path ) throws RepositoryException;
    
    /**
     *  Returns the value of a property.
     *  
     *  @param ws The workspace in which the property value should be located.
     *  @param path The path to the Property
     *  @return Either a ValueImpl or ValueImpl[], depending on whether this is a multi-valued thing
     *  @throws RepositoryException If something goes wrong.
     *  @throws PathNotFoundException If there is nothing at the end of this Path, i.e. the object could not be found.
     */
    public abstract Object getPropertyValue( WorkspaceImpl ws, Path path ) throws PathNotFoundException, RepositoryException;
    
    /**
     *  Returns true, if the Node exists.
     *  
     *  @param ws The workspace in which the existence of the Node is checked.
     *  @param path The path to the Node.
     *  @return True, if the node exists.  False otherwise (like when it's actually a Property)
     */
    public boolean nodeExists( WorkspaceImpl ws, Path path );
    
    /**
     *  Adds a new Node to the repository to the given Path.  The properties of the
     *  Node will be stored separately using successive putPropertyValue() calls.
     *  This includes also system things like the jcr:primaryType, so this method
     *  really exists just to ensure that the Node can be added to the repository.
     * 
     *  @param ws The workspace.
     *  @param path Path to the node in this workspace.
     *  @throws RepositoryException If the Node cannot be added.
     */
    public void addNode( WorkspaceImpl ws, Path path ) throws RepositoryException;
    
    /**
     *  Sets or adds a new Property to the repository.
     * 
     *  @param ws The workspace
     *  @param property The Property content to store.
     *  @throws RepositoryException If the property cannot be stored.
     */
    
    public void putPropertyValue( WorkspaceImpl ws, PropertyImpl property ) throws RepositoryException;
    
    /**
     * Copies content from one path to another path.
     * 
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
     *  @param ws The Workspace.
     *  @param parentpath The path to the Node whose children should be listed.
     *  @return A List of Path objects with the <i>full</i> paths to the children.
     *  @throws RepositoryException If the children cannot be found. 
     */
    public List<Path> listNodes(WorkspaceImpl ws, Path parentpath) throws RepositoryException;
    
    /**
     *  Lists all workspaces which are available in this Repository.
     *  
     *  @return The workspace names.
     */
    public Collection<String> listWorkspaces();

    /**
     *  Removes a node or a property from the repository.  If the removed
     *  entity is a Node, all of its children and properties MUST also be removed
     *  from the repository.
     *  <p>
     *  In addition, it MUST NOT be an error if remove() is called on a path
     *  which is already removed.  In such a case, remove() shall fail silently.
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
    
    /**
     *  This method is called whenever Priha starts a transaction which will save the
     *  contents of the repository.  You could, for example, use this to start a transaction.
     *  
     *  @param ws The workspace
     */
    public void storeStarted(WorkspaceImpl ws);
    
    /**
     *  This method is called when the repository-changing operation is complete.  For example,
     *  you could close the transaction at this stage.
     *  
     *  @param ws Workspace.
     */
    public void storeFinished(WorkspaceImpl ws);
}
