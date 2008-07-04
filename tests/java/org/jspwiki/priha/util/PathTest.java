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

    public static Test suite()
    {
        return new TestSuite( PathTest.class );
    }
}
