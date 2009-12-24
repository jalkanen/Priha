/*
    Priha - A JSR-170 implementation library.

    Copyright (C) 2007-2009 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    Licensed under the Apache License, Version 2.0 (the "License"); 
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at 
    
      http://www.apache.org/licenses/LICENSE-2.0 
      
    Unless required by applicable law or agreed to in writing, software 
    distributed under the License is distributed on an "AS IS" BASIS, 
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
    See the License for the specific language governing permissions and 
    limitations under the License. 
 */
package org.priha.util;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.priha.TestUtil;
import org.priha.core.namespace.NamespaceMapper;
import org.priha.core.namespace.NamespaceRegistryImpl;
import org.priha.path.Path;
import org.priha.path.PathFactory;

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

        // compareTo()
        
        start = System.currentTimeMillis();
        for( int x = 0; x < RUN_ITERS; x++ )
        {
            for( int i = 0; i < SPEED_ITERS-1; i++ )
            {
                int res = paths[i].compareTo( paths[i+1] );
                assertTrue( res == 0 );
            }
        }
        end = System.currentTimeMillis();
        
        TestUtil.printSpeed( "compareTo()", SPEED_ITERS*RUN_ITERS, start, end );        
        
    }
    
    public void testCompareTo() throws NamespaceException, RepositoryException
    {
        Path root = new Path(m_nsa,"/");
        Path a = new Path(m_nsa,"/a");
        Path ab = new Path(m_nsa,"/a/b");
        Path ac = new Path(m_nsa,"/a/c");
        Path b = new Path(m_nsa,"/b");
        Path x = new Path(m_nsa,"/priha:test");
        Path y = new Path(m_nsa,"/priha:test/foo");
        
        assertTrue( "root==root", root.compareTo( root ) == 0 );
        assertTrue( "root < a", root.compareTo(a) < 0 );
        assertTrue( "root < ab", root.compareTo(ab) < 0 );
        assertTrue( "a < b", a.compareTo(b) < 0 );
        assertTrue( "b > a", b.compareTo(a) > 0 );

        assertTrue( "a < ab", a.compareTo(ab) < 0 );
        assertTrue( "ab > a", ab.compareTo(a) > 0 );

        assertTrue( "a == a", a.compareTo(a) == 0 );
        assertTrue( "ab == ab", ab.compareTo(ab) == 0 );

        assertTrue( "ab < b", ab.compareTo(b) < 0 );
        assertTrue( "b > ab", b.compareTo(ab) > 0 );
        
        assertTrue( "ab < ac", ab.compareTo(ac) < 0 );
        assertTrue( "ac > ab", ac.compareTo(ab) > 0 );

        assertTrue( "x < y", x.compareTo(y) < 0 );
    }
    
    public void testValueOf() throws Exception
    {
        Path.Component p = Path.Component.valueOf( "foo" );
        
        assertEquals("foo", "foo", p.toString());
        
        p = Path.Component.valueOf( "{http://priha.org}bar");
        
        assertEquals("bar", "{http://priha.org}bar", p.toString());        
        assertEquals("bar index", 1, p.getIndex());

        p = Path.Component.valueOf( "{http://priha.org}gobble[5]");
        
        assertEquals("gobble", "{http://priha.org}gobble[5]", p.toString());
        assertEquals("gobble index", 5, p.getIndex());
    }
    
    public static Test suite()
    {
        return new TestSuite( PathTest.class );
    }
}
