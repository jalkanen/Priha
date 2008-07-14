package org.jspwiki.priha.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class PathTest extends TestCase
{
    public void testPath1()
        throws Exception
    {
        Path p = new Path("/");
        
        assertEquals( "/",p.toString() );
    }

    public void testPath2() throws Exception
    {
        Path p = new Path("/foo/bar/gobble/bloo");
        
        assertEquals( "/foo/bar/gobble/bloo",p.toString() );
    }
    public void testPath3() throws Exception
    {
        Path p = new Path("/foo[2]/bar[3]/jcr:node");
    
        assertEquals( "/foo[2]/bar[3]/jcr:node",p.toString() );
    }
    
    public void testIsRoot1() throws Exception
    {
        Path p = new Path("/");
        
        assertTrue(p.isRoot());
    }
    

    public void testIsRoot2() throws Exception
    {
        Path p = new Path("/test/root");
        
        assertFalse(p.isRoot());
    }

    public void testIsParentOf1() throws Exception
    {
        Path p1 = new Path("/test/root");
        Path p2 = new Path("/test/root/foo/bar");
        
        assertTrue( "p1->p2", p1.isParentOf(p2) );
        assertFalse( "p2->p1", p2.isParentOf(p1) );
        
        assertFalse( "p1=p1", p1.isParentOf(p1));
    }

    public void testIsParentOf2() throws Exception
    {
        Path p1 = new Path("/");
        Path p2 = new Path("/test/root/foo/bar");
        
        assertTrue( "p1->p2", p1.isParentOf(p2) );
        assertFalse( "p2->p1", p2.isParentOf(p1) );
    }

    private static final int SPEED_ITERS = 10000;
    
    public void testSpeed() throws Exception
    {
        Path[] paths = new Path[SPEED_ITERS];
        long start = System.currentTimeMillis();
        for( int i = 0; i < SPEED_ITERS; i++ )
        {
            Path p = new Path( "/foo/bar/test/gobble" );
            paths[i] = p;
        }
        
        long end = System.currentTimeMillis();

        printSpeed( "Path creation", SPEED_ITERS, start, end );
        
        start = System.currentTimeMillis();
        for( int i = 0; i < SPEED_ITERS; i++ )
        {
            Path p = paths[i].resolve("test");
            
            paths[i] = p;
        }
        end = System.currentTimeMillis();
        
        printSpeed( "Basic resolve", SPEED_ITERS, start, end );
        
        start = System.currentTimeMillis();
        for( int i = 0; i < SPEED_ITERS; i++ )
        {
            Path p = paths[i].getParentPath();
            
            paths[i] = p;
        }
        end = System.currentTimeMillis();
        
        printSpeed( "getParentPath()", SPEED_ITERS, start, end );        
        
    }
    
    private void printSpeed( String msg, int iters, long start, long end )
    {
        long time = end - start;
        float itersSec = (iters*100)/((float)time/1000) / 100;
        
        System.out.println( msg + ":" + iters + " iterations in "+time+" ms ("+itersSec+" iterations/second)");
    }
    
    public static Test suite()
    {
        return new TestSuite( PathTest.class );
    }
}
