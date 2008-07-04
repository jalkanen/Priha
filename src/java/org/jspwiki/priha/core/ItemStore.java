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
    
    void close(WorkspaceImpl m_workspace);

    void copy(WorkspaceImpl m_workspace, Path srcpath, Path destpath) throws RepositoryException;

    NodeImpl findByUUID(WorkspaceImpl m_workspace, String uuid) throws RepositoryException;

    ItemImpl getItem(WorkspaceImpl m_workspace, Path path) throws InvalidPathException, RepositoryException;

    List<Path> listNodes(WorkspaceImpl m_workspace, Path parentpath);

    Collection<String> listWorkspaces();

    void move(WorkspaceImpl m_workspace, Path srcpath, Path destpath) throws RepositoryException;

    boolean nodeExists(WorkspaceImpl m_workspace, Path path);


    void remove(WorkspaceImpl m_workspace, Path path) throws RepositoryException;

    void addNode(WorkspaceImpl m_workspace, NodeImpl ni) throws RepositoryException;

    void putProperty(WorkspaceImpl m_workspace, PropertyImpl pi) throws RepositoryException;
}
