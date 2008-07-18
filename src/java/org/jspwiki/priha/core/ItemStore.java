package org.jspwiki.priha.core;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

import javax.jcr.Credentials;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;

import org.jspwiki.priha.util.ConfigurationException;
import org.jspwiki.priha.util.InvalidPathException;
import org.jspwiki.priha.util.Path;

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

    void move(WorkspaceImpl m_workspace, Path srcpath, Path destpath) throws RepositoryException;

    boolean nodeExists(WorkspaceImpl m_workspace, Path path) throws RepositoryException;


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
