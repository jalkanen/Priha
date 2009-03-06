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
