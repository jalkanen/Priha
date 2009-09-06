package org.priha.util;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite("Util tests");
    
        suite.addTest( PathTest.suite() );
        suite.addTest( PathUtilTest.suite() );
        suite.addTest( FastPropertyStoreTest.suite() );
        return suite;
    }
}
