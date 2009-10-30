package org.priha.providers;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.priha.AbstractTest;

public class AllTests extends AbstractTest
{

    public static Test suite()
    {
        TestSuite suite = new TestSuite("Provider tests");
        
        suite.addTest( FileProviderTest.suite() );
        suite.addTest( JdbcProviderTest.suite() );
        
        return suite;
    }

}
