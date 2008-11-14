package org.priha.query.xpath;

import java.util.List;

import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

public class JCRContainsFunction implements XPathFunction
{

    public Object evaluate(List args) throws XPathFunctionException
    {
        System.out.println("jcr:contains called: "+(args!=null ? args.size() : "null" ));
        // TODO Auto-generated method stub
        return null;
    }

}
