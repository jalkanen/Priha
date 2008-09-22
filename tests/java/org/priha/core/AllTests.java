package org.priha.core;

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
        //
        // It is not necessary to run these, a they take quite a while.
        // suite.addTest( PerformanceTest.suite() );
        return suite;
    }

}
