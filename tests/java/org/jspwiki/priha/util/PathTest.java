package org.jspwiki.priha.util;

import junit.framework.TestCase;

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
}
