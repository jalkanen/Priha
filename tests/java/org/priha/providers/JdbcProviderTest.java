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
package org.priha.providers;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.priha.RepositoryManager;
import org.priha.TestUtil;
import org.priha.core.RepositoryImpl;
import org.priha.core.SessionImpl;

public class JdbcProviderTest extends TestCase
{
    SessionImpl m_session;
    
    public void setUp() throws Exception
    {
        super.setUp();
        RepositoryImpl rep = RepositoryManager.getRepository("jdbcnocache.properties");
        m_session = rep.login(new SimpleCredentials("foo",new char[0]));
    }
    
    public void tearDown() throws Exception
    {
        Repository rep = m_session.getRepository();
        m_session.logout();
      
        TestUtil.emptyRepo(rep);
        
        super.tearDown();
    }
    
    private static final String UTF8_NAME = "\u00e5\u304f"; // HIRAGANA KU
    
    public void testUTF8_1() throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException
    {
        Node nd = m_session.getRootNode().addNode(UTF8_NAME, "priha:referenceable");
        nd.setProperty(UTF8_NAME, UTF8_NAME);
        
        m_session.save();
        
        Node nd2 = (Node)m_session.getItem("/"+UTF8_NAME);
        Property p2 = nd2.getProperty(UTF8_NAME);
        
        assertEquals(UTF8_NAME,nd2.getName());
        assertEquals(UTF8_NAME,p2.getName());
        assertEquals(UTF8_NAME,p2.getString());
    }
    
    /**
     *  JCR names are case-sensitive, so let's check that all these work.
     */
    @SuppressWarnings("unused")
    public void testCase1() throws Exception
    {
        Node nd = m_session.getRootNode().addNode("Test One");
        nd.setProperty("Property One","KILL");
        
        m_session.save();
        
        try
        {
            Node nd2 = (Node)m_session.getItem("/test one");
            fail("Found with wrong case");
        }
        catch( PathNotFoundException e ) {}
        
        
        try
        {
            Property p2 = (Property)m_session.getItem("/Test One/property one");
            fail("Found property with wrong case");
        }
        catch( PathNotFoundException e ) {}   
    }    
    
    @SuppressWarnings("unused")
    public void testCase2() throws Exception
    {
        Node nd = m_session.getRootNode().addNode("A");
        nd.setProperty("B", "foo");
        m_session.save();
        
        try
        {
            Node nd2 = (Node)m_session.getItem("/a");
            fail("Found with wrong case");
        }
        catch( PathNotFoundException e ) {}
        
        
        try
        {
            Property p2 = (Property)m_session.getItem("/A/b");
            fail("Found property with wrong case");
        }
        catch( PathNotFoundException e ) {}   
        
    }
    
    public void testCase3() throws Exception
    {
        Node nd = m_session.getRootNode().addNode("A");
        nd.setProperty("x","foo-a");
        
        Node nd2 = m_session.getRootNode().addNode("a");
        nd2.setProperty("x","foo-b");
        
        m_session.save();

        assertFalse("same",nd.isSame(nd2));
        
        Node nda = (Node) m_session.getItem("/A");
        assertEquals("foo-a",nda.getProperty("x").getString());

        Node ndb = (Node) m_session.getItem("/a");
        assertEquals("foo-b",ndb.getProperty("x").getString());
    }
    
    public void testPropertyCase1() throws Exception
    {
        Node nd = m_session.getRootNode().addNode("A");
        nd.setProperty("x","foo-a");
        nd.setProperty("X","foo-b");
        
        m_session.save();
        
        Node nda = (Node) m_session.getItem("/A");
        assertEquals("foo-a",nda.getProperty("x").getString());
        assertEquals("foo-b",nda.getProperty("X").getString());
    }   
    public static Test suite()
    {
        return new TestSuite( JdbcProviderTest.class );
    }

}
