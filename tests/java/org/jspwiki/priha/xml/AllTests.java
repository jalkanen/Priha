package org.jspwiki.priha.xml;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite("XML tests");
    
        suite.addTest( XMLUtilsTest.suite() );
    
        return suite;
    }
}
