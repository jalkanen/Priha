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
package org.priha.core;

import java.util.Collection;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;

import org.priha.providers.StoreTransaction;
import org.priha.util.InvalidPathException;
import org.priha.util.Path;
import org.priha.util.QName;

public interface ItemStore
{
    void open(Credentials credentials, String workspaceName) throws NoSuchWorkspaceException, RepositoryException;
    
    void stop();
    
    void close(WorkspaceImpl ws);

    void copy(WorkspaceImpl ws, Path srcpath, Path destpath) throws RepositoryException;

    NodeImpl findByUUID(WorkspaceImpl ws, String uuid) throws RepositoryException;

    ItemImpl getItem(WorkspaceImpl ws, Path path) throws InvalidPathException, RepositoryException;

    List<? extends Path> listNodes(WorkspaceImpl m_workspace, Path parentpath) throws RepositoryException;

    Collection<? extends String> listWorkspaces() throws RepositoryException;

    boolean nodeExists(WorkspaceImpl m_workspace, Path path) throws RepositoryException;


    void remove(StoreTransaction tx, Path path) throws RepositoryException;

    void addNode(StoreTransaction tx, NodeImpl ni) throws RepositoryException;

    void putProperty(StoreTransaction tx, PropertyImpl pi) throws RepositoryException;

    /**
     *  Locate all properties of type REFERENCE with the content of the particular UUID.
     *  
     *  @param m_workspace
     *  @param uuid
     *  @return
     * @throws RepositoryException 
     */
    Collection<? extends PropertyImpl> getReferences(WorkspaceImpl w, String uuid) throws RepositoryException;

    List<QName> listProperties(WorkspaceImpl ws, Path path) throws RepositoryException;
    
    public StoreTransaction storeStarted(WorkspaceImpl ws) throws RepositoryException;
    
    public void storeFinished(StoreTransaction tx) throws RepositoryException;

    public void storeCancelled(StoreTransaction tx) throws RepositoryException;

    void reorderNodes(StoreTransaction tx, Path internalPath, List<Path> childOrder) throws RepositoryException;
}
