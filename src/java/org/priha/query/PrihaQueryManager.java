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


/**
 *  Manages our Provider modules and provides the QueryManager interface.
 *  
 *  @author Janne Jalkanen
 */
public class PrihaQueryManager implements QueryManager
{
    private static final String[] SUPPORTEDLANGUAGES = {  };
    
    private static final String   DEFAULT_QUERYPROVIDER = "org.priha.BasicQueryProvider";
    
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
