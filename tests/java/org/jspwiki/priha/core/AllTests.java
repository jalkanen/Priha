package org.jspwiki.priha.core;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests
{

    public static Test suite()
    {
        TestSuite suite = new TestSuite("Core tests");
        
        suite.addTest( NamespaceRegistryImplTest.suite() );
        suite.addTest( RepositoryTest.suite() );
        suite.addTest( WorkspaceImplTest.suite() );
        suite.addTest( NodeImplTest.suite() );
        suite.addTest( SessionImplTest.suite() );
        // suite.addTest( PerformanceTest.suite() );
        return suite;
    }

}
