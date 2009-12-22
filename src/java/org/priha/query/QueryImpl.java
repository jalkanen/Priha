package org.priha.query;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

import org.priha.core.SessionImpl;
import org.priha.query.aqt.QueryRootNode;

/**
 *  The root class for Priha query implementations.
 *  
 *  @author Janne Jalkanen
 */
public abstract class QueryImpl implements Query
{
    private QueryRootNode m_root;
    private SessionImpl   m_session;
    private String        m_storedQueryPath = null;
    
    /**
     *  Construct a QueryImpl for a given Session and a given abstract
     *  query tree.  Subclasses are expected to create the AQT.
     *  
     *  @param session The Session against which this Query is created
     *  @param root The AQT Root node
     */
    public QueryImpl(SessionImpl session, QueryRootNode root)
    {
        m_session = session;
        m_root = root;
    }

    public QueryResult execute() throws RepositoryException
    {
        QueryProvider qp = m_session.getWorkspace().getQueryManager().getQueryProvider();
        
        System.out.println( "Executing "+getStatement() );
        
        return qp.query(m_session, m_root);
    }

    public String getStoredQueryPath() throws ItemNotFoundException, RepositoryException
    {
        if( m_storedQueryPath == null ) throw new ItemNotFoundException("This is not a stored query");

        return m_storedQueryPath;
    }

    public Node storeAsNode(String absPath)
                                        throws ItemExistsException,
                                            PathNotFoundException,
                                            VersionException,
                                            ConstraintViolationException,
                                            LockException,
                                            UnsupportedRepositoryOperationException,
                                            RepositoryException
    {
        Node queryNode = m_session.getRootNode().addNode( absPath, "nt:query" );
        
        queryNode.setProperty( "jcr:statement", getStatement() );
        queryNode.setProperty( "jcr:language", getLanguage() );
        
        m_storedQueryPath = queryNode.getPath();
        
        return queryNode;
    }

}
