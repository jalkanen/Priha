package org.priha;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllTests extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite("Priha unit tests");
        
        suite.addTest( org.priha.nodetype.AllTests.suite() );
        suite.addTest( org.priha.core.AllTests.suite() );
        suite.addTest( org.priha.util.AllTests.suite() );
        suite.addTest( org.priha.xml.AllTests.suite() );
        
        return suite;
    }
}
