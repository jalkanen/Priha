package org.jspwiki.priha.core;

import java.util.List;

import javax.jcr.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jspwiki.priha.RepositoryManager;
import org.jspwiki.priha.TestUtil;
import org.jspwiki.priha.util.Path;

public class WorkspaceImplTest extends TestCase
{
    Repository m_repository;
    
    protected void setUp() throws Exception
    {
        m_repository = RepositoryManager.getRepository();

        TestUtil.emptyRepo( m_repository );
    }
    
    protected void tearDown() throws Exception
    {
        TestUtil.emptyRepo(m_repository);
    }

    public void testEmpty()
    {
        
    }
    
    public static Test suite()
    {
        return new TestSuite( WorkspaceImplTest.class );
    }
}
