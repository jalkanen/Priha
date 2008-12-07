package org.priha.query;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

import org.priha.core.SessionImpl;
import org.priha.query.aqt.QueryRootNode;

public abstract class QueryImpl implements Query
{
    private QueryRootNode m_root;
    private SessionImpl m_session;
    
    public QueryImpl(SessionImpl session, QueryRootNode root)
    {
        m_session = session;
        m_root = root;
    }

    public QueryResult execute() throws RepositoryException
    {
        QueryProvider qp = m_session.getWorkspace().getQueryManager().getQueryProvider();
        
        return qp.query(m_session, m_root);
    }

    public String getStoredQueryPath() throws ItemNotFoundException, RepositoryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Node storeAsNode(String arg0)
                                        throws ItemExistsException,
                                            PathNotFoundException,
                                            VersionException,
                                            ConstraintViolationException,
                                            LockException,
                                            UnsupportedRepositoryOperationException,
                                            RepositoryException
    {
        // TODO Auto-generated method stub
        return null;
    }

}
