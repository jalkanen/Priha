package org.jspwiki.priha.query;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;


/**
 *  Manages our Provider modules and provides the QueryManager interface.
 *  
 *  @author Janne Jalkanen
 */
public class PrihaQueryManager implements QueryManager
{
    private static final String[] SUPPORTEDLANGUAGES = {  };
    
    private static final String   DEFAULT_QUERYPROVIDER = "org.jspwiki.priha.BasicQueryProvider";
    
    private QueryProvider m_queryProvider;
    
    public Query createQuery(String statement, String language) throws InvalidQueryException, RepositoryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Query getQuery(Node node) throws InvalidQueryException, RepositoryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String[] getSupportedQueryLanguages() throws RepositoryException
    {
        return SUPPORTEDLANGUAGES;
    }

}
