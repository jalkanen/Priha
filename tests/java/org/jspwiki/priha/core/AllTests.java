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
        
        return suite;
    }

}
