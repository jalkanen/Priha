/*
    Priha - A JSR-170 implementation library.

    Copyright (C) 2007-2009 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;

import org.priha.core.SessionImpl;
import org.priha.query.aqt.DefaultQueryNodeFactory;
import org.priha.query.aqt.xpath.XPathQueryBuilder;

/**
 *  Implements an XPath query by using the XPathQueryBuilder to construct
 *  an abstract query tree (AQT).
 *  
 *  @author Janne Jalkanen
 */
public class XPathQueryImpl extends QueryImpl implements Query
{
    private String      m_statement;

    public XPathQueryImpl(SessionImpl session, String statement) 
        throws InvalidQueryException
    {
        super( session, XPathQueryBuilder.createQuery(statement, 
                                                      session, 
                                                      new DefaultQueryNodeFactory(null) ) );
        m_statement = statement;
    }

    public String getLanguage()
    {
        return XPATH;
    }

    public String getStatement()
    {
        return m_statement;
    }

}
