package org.priha.providers;

import org.priha.core.WorkspaceImpl;

/**
 *  Provides a base implementation for a StoreTransaction.
 */
public class BaseStoreTransaction implements StoreTransaction
{
    protected WorkspaceImpl m_workspace;
    
    public BaseStoreTransaction(WorkspaceImpl ws)
    {
        m_workspace = ws;
    }
    
    public WorkspaceImpl getWorkspace()
    {
        return m_workspace;
    }

}
