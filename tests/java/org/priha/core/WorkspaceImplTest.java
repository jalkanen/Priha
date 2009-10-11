package org.priha.core;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.priha.AbstractTest;
import org.priha.TestUtil;

public class WorkspaceImplTest extends AbstractTest
{
    protected void setUp() throws Exception
    {
        super.setUp();
        TestUtil.emptyRepo( m_repository );
    }
    
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testEmpty()
    {
        
    }
    
    public static Test suite()
    {
        return new TestSuite( WorkspaceImplTest.class );
    }
}
