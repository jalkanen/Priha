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
package org.priha.providers;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

import javax.jcr.*;

import org.priha.core.ItemType;
import org.priha.core.RepositoryImpl;
import org.priha.core.WorkspaceImpl;
import org.priha.nodetype.QNodeDefinition;
import org.priha.path.Path;
import org.priha.util.ConfigurationException;
import org.priha.util.QName;

/**
 *  A few ground rules:
 *  <ul>
 *  <li>A RepositoryProvider shall not cache the Session object</li>
 *  <li>A RepositoryProvider shall be thread-safe</li>
 *  <li>There shall always be a default workspace called "default"</li>
 *  </ul>
 *  <p>
 *  Priha RepositoryManager ensures that methods which modify the repository
 *  (addNode(), close(), copy(), move(), open(), putPropertyValue(), remove(),
 *   start(), stop(), storeFinished(), storeStarted()) are single-threaded.
 *  However, the rest of the methods which are supposed to read from the
 *  repository, are protected by a read lock, and therefore they can be
 *  accessed at the same time from multiple threads.  If you do any modifications
 *  anywhere, make sure these are thread-safe.
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
    
    //
    //  REPOSITORY MANAGEMENT
    //
    /**
     *  Opens a repository.  Called whenever a session login() is performed.
     *  
     * @param rep The Repository which owns this Provider.
     * @param credentials The Credentials object passed to the Session.open() call.  May be null,
     *                    if there were no credentials.
     * @param workspaceName The workspace which will be accessed.
     * @throws NoSuchWorkspaceException if no such workspace exists. 
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
     *  
     *  @see org.priha.core.ProviderManager#filterProperties(RepositoryImpl, String)
     */
    public void start( RepositoryImpl repository, 
                       Properties     properties ) throws ConfigurationException;
    
    /**
     *  Stops a given repository.  This may be called without a preceding call
     *  to close().  All allocated resources can now be deallocated.
     *  <p>
     *  This method will only be called when the Repository shuts down.
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
     *  Lists all workspaces which are available in this Repository.  This method is
     *  called after start() but before open().
     *  
     *  @return The workspace names.
     * @throws RepositoryException 
     */
    public Collection<String> listWorkspaces() throws RepositoryException;


    //
    //  GETTING NODES AND VALUES
    //
    /**
     *  Returns a list of properties for a Node.
     *  
     *  @param ws The Workspace in which the properties should be located.
     *  @param path The path of the Node.
     *  @return A List of the names of the properties under this Node.
     *  @throws PathNotFoundException If the path given does not exist.
     *  @throws RepositoryException If something goes wrong.
     */
    public abstract List<QName> listProperties( WorkspaceImpl ws, Path path ) 
        throws PathNotFoundException, RepositoryException;
    
    /**
     *  Returns the value of a property.
     *  
     *  @param ws The workspace in which the property value should be located.
     *  @param path The path to the Property
     *  @return Either a ValueImpl or ValueImpl[], depending on whether this is a multi-valued thing
     *  @throws RepositoryException If something goes wrong.
     *  @throws PathNotFoundException If there is nothing at the end of this Path, i.e. the object could not be found.
     */
    public abstract ValueContainer getPropertyValue( WorkspaceImpl ws, Path path ) 
        throws PathNotFoundException, RepositoryException;
    
    /**
     *  Returns true, if the Item exists and is of given type.
     *  
     *  @param ws The workspace in which the existence of the Node is checked.
     *  @param path The path to the Node.
     *  @param type Type to check for
     *  @return True, if the item exists.  False otherwise (like when it's actually of a different type)
     *  @throws RepositoryException 
     */
    public boolean itemExists( WorkspaceImpl ws, Path path, ItemType type ) throws RepositoryException;
    
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
     *  If an item by this UUID exists, returns a Path.
     *  
     * @param ws
     * @param uuid
     * @return
     * @throws ItemNotFoundException If the repository does not contain an UUID by this name.
     */
    public Path findByUUID( WorkspaceImpl ws, String uuid ) throws ItemNotFoundException, RepositoryException;

    /**
     *  Finds all the Property paths which are of type REFERENCE and whose content
     *  is equal to the UUID given.
     *  
     *  @param ws
     *  @param uuid
     *  @return A list of paths to properties which reference the node by the given UUID.
     *  @throws RepositoryException 
     */
    public List<Path> findReferences(WorkspaceImpl ws, String uuid) throws RepositoryException;

    //
    //  METHODS WHICH CHANGE THE REPOSITORY CONTENT
    //
    
    /**
     *  Adds a new Node to the repository to the given Path.  The properties of the
     *  Node will be stored separately using successive putPropertyValue() calls.
     *  This includes also system things like the jcr:primaryType, so this method
     *  really exists just to ensure that the Node can be added to the repository.
     * 
     *  @param ws The workspace.
     *  @param path Path to the node in this workspace.
     *  @paran definition The definition of the Node which will be added. The provider
     *                    may use this to optimize for particular types.
     *  @throws RepositoryException If the Node cannot be added.
     */
    public void addNode( StoreTransaction tx, Path path, QNodeDefinition definition ) throws RepositoryException;
    
    /**
     *  Sets or adds a new Property to the repository.  Note that
     *  a Property may be multi-valued.  It is up to the provider to
     *  decide how it serializes the data.
     * 
     *  @param ws The workspace
     *  @param property The Property content to store.
     *  @throws RepositoryException If the property cannot be stored.
     */
    
    public void putPropertyValue( StoreTransaction tx, Path path, ValueContainer property ) throws RepositoryException;
    

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
    public void remove( StoreTransaction tx, Path path )  throws RepositoryException;
    

    /**
     *  This method is called whenever Priha starts a transaction which will save the
     *  contents of the repository.  You could, for example, use this to start a transaction.
     *  
     *  @param ws The workspace
     *  @return An arbitrary StoreTransaction object. May be null.
     */
    public StoreTransaction storeStarted(WorkspaceImpl ws) throws RepositoryException;
    
    /**
     *  This method is called when the repository-changing operation is complete.  For example,
     *  you could close the transaction at this stage.
     * @param tx The same StoreTransaction object which was returned from storeStarted().
     */
    public void storeFinished(StoreTransaction tx) throws RepositoryException;
    
    /**
     *  If the store has been cancelled and changes need to be rolled back.  A RepositoryProvider
     *  should use this opportunity to make sure it is in a consistent state.
     *  
     *  @param tx The transaction from storeStarted().
     */
    public void storeCancelled(StoreTransaction tx) throws RepositoryException;

    public void reorderNodes(StoreTransaction tx, Path path, List<Path> childOrder) throws RepositoryException;
}
