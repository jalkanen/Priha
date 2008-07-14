package org.jspwiki.priha.core;

import java.util.Collection;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;

import org.jspwiki.priha.util.InvalidPathException;
import org.jspwiki.priha.util.Path;

public interface ItemStore
{
    void open(RepositoryImpl repository, Credentials credentials, String workspaceName) throws NoSuchWorkspaceException, RepositoryException;
    
    void start(RepositoryImpl repository);

    void stop(RepositoryImpl repository);
    
    void close(WorkspaceImpl ws);

    void copy(WorkspaceImpl ws, Path srcpath, Path destpath) throws RepositoryException;

    NodeImpl findByUUID(WorkspaceImpl ws, String uuid) throws RepositoryException;

    ItemImpl getItem(WorkspaceImpl ws, Path path) throws InvalidPathException, RepositoryException;

    List<? extends Path> listNodes(WorkspaceImpl m_workspace, Path parentpath);

    Collection<? extends String> listWorkspaces();

    void move(WorkspaceImpl m_workspace, Path srcpath, Path destpath) throws RepositoryException;

    boolean nodeExists(WorkspaceImpl m_workspace, Path path);


    void remove(WorkspaceImpl m_workspace, Path path) throws RepositoryException;

    void addNode(WorkspaceImpl m_workspace, NodeImpl ni) throws RepositoryException;

    void putProperty(WorkspaceImpl m_workspace, PropertyImpl pi) throws RepositoryException;

    /**
     *  Locate all properties of type REFERENCE with the content of the particular UUID.
     *  
     *  @param m_workspace
     *  @param uuid
     *  @return
     * @throws RepositoryException 
     */
    Collection<? extends PropertyImpl> getReferences(WorkspaceImpl w, String uuid) throws RepositoryException;

    List<String> listProperties(WorkspaceImpl ws, Path path) throws RepositoryException;
}
