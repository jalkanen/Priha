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
package org.priha.query;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.priha.core.SessionImpl;
import org.priha.query.aqt.DefaultQueryNodeFactory;
import org.priha.query.aqt.QueryRootNode;
import org.priha.query.aqt.xpath.XPathQueryBuilder;


/**
 *  Manages our Provider modules and provides the QueryManager interface.
 *  
 *  @author Janne Jalkanen
 */
public class PrihaQueryManager implements QueryManager
{
    private static final String[] SUPPORTEDLANGUAGES = { Query.XPATH };
    
    private static final String   DEFAULT_QUERYPROVIDER = "org.priha.BasicQueryProvider";
    
    private QueryProvider m_queryProvider;

    private SessionImpl m_session;
    
    public PrihaQueryManager( SessionImpl session )
    {
        m_session = session;
    }
    
    /**
     *  Get the QueryProvider which will be used to resolve the actual query.
     *  
     *  @return A QueryProvider instance.
     */
    protected QueryProvider getQueryProvider()
    {
        return new SimpleQueryProvider();
    }
    
    public Query createQuery(String statement, String language) throws InvalidQueryException, RepositoryException
    {
        if( language.equals( Query.XPATH ) )
        {
            QueryRootNode root = XPathQueryBuilder.createQuery(statement, 
                                                               m_session, 
                                                               new DefaultQueryNodeFactory(null) );
            
            return new XPathQueryImpl( m_session, statement );
        }
        
        throw new InvalidQueryException("Query language "+language+" is not supported.");
    }

    public Query getQuery(Node node) throws InvalidQueryException, RepositoryException
    {
        throw new InvalidQueryException("Node not found");
    }

    public String[] getSupportedQueryLanguages() throws RepositoryException
    {
        return SUPPORTEDLANGUAGES;
    }

}
