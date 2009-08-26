package org.priha.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.priha.TestUtil;
import org.priha.core.namespace.NamespaceMapper;
import org.priha.core.namespace.NamespaceRegistryImpl;

public class PathTest extends TestCase
{
    private NamespaceMapper m_nsa = new NamespaceRegistryImpl();
    
    public void testRoot()
    {
        assertEquals("/", Path.ROOT.toString());
    }
    
    public void testPath1()
        throws Exception
    {
        Path p = new Path(m_nsa,"/");
        
        assertEquals( "/",p.toString() );
    }

    public void testPath2() throws Exception
    {
        Path p = new Path(m_nsa,"/foo/bar/gobble/bloo");
        
        assertEquals( "/foo/bar/gobble/bloo",p.toString() );
    }
    public void testPath3() throws Exception
    {
        Path p = new Path(m_nsa,"/foo[2]/bar[3]/jcr:node");
    
        assertEquals( "/foo[2]/bar[3]/{http://www.jcp.org/jcr/1.0}node",p.toString() );
    }
    
    public void testQPath() throws Exception
    {
        Path p = PathFactory.getPath( "/{http://www.jcp.org/jcr/1.0}foo[2]/{http://www.jcp.org/jcr/1.0}bar/node");
    
        assertEquals( "/{http://www.jcp.org/jcr/1.0}foo[2]/{http://www.jcp.org/jcr/1.0}bar/node",p.toString() );
    }

    public void testQPath2() throws Exception
    {
        Path p = new Path(m_nsa,"/{http://www.jcp.org/jcr/1.0}foo[2]/{http://www.jcp.org/jcr/1.0}bar/jcr:node");
    
        assertEquals( "/{http://www.jcp.org/jcr/1.0}foo[2]/{http://www.jcp.org/jcr/1.0}bar/{http://www.jcp.org/jcr/1.0}node",p.toString() );
    }
    
    public void testIsRoot1() throws Exception
    {
        Path p = new Path(m_nsa,"/");
        
        assertTrue(p.isRoot());
    }
    

    public void testIsRoot2() throws Exception
    {
        Path p = new Path(m_nsa,"/test/root");
        
        assertFalse(p.isRoot());
    }

    public void testIsParentOf1() throws Exception
    {
        Path p1 = new Path(m_nsa,"/test/root");
        Path p2 = new Path(m_nsa,"/test/root/foo/bar");
        
        assertTrue( "p1->p2", p1.isParentOf(p2) );
        assertFalse( "p2->p1", p2.isParentOf(p1) );
        
        assertFalse( "p1=p1", p1.isParentOf(p1));
    }

    public void testIsParentOf2() throws Exception
    {
        Path p1 = new Path(m_nsa,"/");
        Path p2 = new Path(m_nsa,"/test/root/foo/bar");
        
        assertTrue( "p1->p2", p1.isParentOf(p2) );
        assertFalse( "p2->p1", p2.isParentOf(p1) );
    }

    private static final int SPEED_ITERS = 100000;
    private static final int RUN_ITERS = 10;
    
    public void testSpeed() throws Exception
    {
        Path[] paths = new Path[SPEED_ITERS];
        Path refpath = new Path( m_nsa, "/foo/bar/test/xxx/");
        
        // Path creation
        long start = System.currentTimeMillis();
        for( int i = 0; i < SPEED_ITERS; i++ )
        {
            Path p = new Path(m_nsa, "/foo/bar/test/gobble" );
            paths[i] = p;
        }
        
        long end = System.currentTimeMillis();

        TestUtil.printSpeed( "Path creation", SPEED_ITERS, start, end );
        
        // Resolve
        start = System.currentTimeMillis();
        for( int i = 0; i < SPEED_ITERS; i++ )
        {
            Path p = paths[i].resolve(m_nsa,"test");
            
            assertNotNull(p);
            //paths[i] = p;
        }
        end = System.currentTimeMillis();
        
        TestUtil.printSpeed( "Basic resolve", SPEED_ITERS, start, end );
        
        // getParentPath
        start = System.currentTimeMillis();
        for( int x = 0; x < RUN_ITERS; x++ )
        {
            for( int i = 0; i < SPEED_ITERS; i++ )
            {
                Path p = paths[i].getParentPath();
            
                assertNotNull(p);
                //paths[i] = p;
            }
        }
        end = System.currentTimeMillis();
        
        TestUtil.printSpeed( "getParentPath()", SPEED_ITERS*RUN_ITERS, start, end );        

        // equals()
        start = System.currentTimeMillis();
        for( int x = 0; x < RUN_ITERS; x++ )
        {
            for( int i = 0; i < SPEED_ITERS-1; i++ )
            {
                boolean eq = paths[i].equals(paths[i+1]);
            
                assertTrue(eq);
                
                eq = paths[i].equals(refpath);
                
                assertFalse(eq);
                
                eq = paths[i].equals( paths[i] );
                
                assertTrue(eq);
            }
        }
        end = System.currentTimeMillis();
        
        TestUtil.printSpeed( "equals()", SPEED_ITERS*RUN_ITERS, start, end );        

        // isParentOf()
        start = System.currentTimeMillis();
        for( int x = 0; x < RUN_ITERS; x++ )
        {
            for( int i = 0; i < SPEED_ITERS-1; i++ )
            {
                boolean eq = paths[i].isParentOf( paths[i+1] );
                assertFalse(eq);
            
                eq = paths[i].getParentPath().isParentOf( paths[i+1] );
                assertTrue(eq);
            }
        }
        end = System.currentTimeMillis();
        
        TestUtil.printSpeed( "isParentOf()", SPEED_ITERS*RUN_ITERS, start, end );        

    }
    
    public static Test suite()
    {
        return new TestSuite( PathTest.class );
    }
}
