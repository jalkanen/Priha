package org.priha.query;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite("Query tests");
    
        suite.addTest( XPathTest.suite() );
    
        return suite;
    }
}
