package org.priha.query;

import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;

import org.priha.core.SessionImpl;
import org.priha.query.aqt.DefaultQueryNodeFactory;
import org.priha.query.aqt.xpath.XPathQueryBuilder;

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
        System.out.println("Statement = "+statement);
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
