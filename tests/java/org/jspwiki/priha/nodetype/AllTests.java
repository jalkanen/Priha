package org.jspwiki.priha.nodetype;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllTests extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite("NodeType package tests");

        suite.addTest( NodeTypeManagerImplTest.suite() );

        return suite;
    }
}
