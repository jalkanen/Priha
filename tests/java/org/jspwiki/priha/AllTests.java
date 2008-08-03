package org.jspwiki.priha;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllTests extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite("Priha unit tests");
        
        suite.addTest( org.jspwiki.priha.nodetype.AllTests.suite() );
        suite.addTest( org.jspwiki.priha.core.AllTests.suite() );
        suite.addTest( org.jspwiki.priha.util.AllTests.suite() );
        suite.addTest( org.jspwiki.priha.xml.AllTests.suite() );
        
        return suite;
    }
}
