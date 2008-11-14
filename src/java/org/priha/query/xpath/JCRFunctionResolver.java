package org.priha.query.xpath;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionResolver;

import org.priha.core.JCRConstants;

public class JCRFunctionResolver implements XPathFunctionResolver
{
    private static final QName F_CONTAINS = new QName(JCRConstants.NS_JCP,"contains");
    
    public XPathFunction resolveFunction( QName functionName, int arity )
    {
        if( F_CONTAINS.equals(functionName) )
        {
            return new JCRContainsFunction();
        }
        
        return null;
    }

}
