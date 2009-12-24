package org.priha.providers;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.priha.AbstractTest;
import org.priha.TestUtil;
import org.priha.core.SessionImpl;

public class FileProviderTest extends AbstractTest
{
    SessionImpl m_session;
    
    public void setUp() throws Exception
    {
        super.setUp();
        m_session = getNewSession();
    }
    
    public void tearDown() throws Exception
    {
        TestUtil.emptyRepo(m_session.getRepository());
        m_session.logout();
        
        super.tearDown();
    }
    
    private static final String UTF8_NAME = "\u00e5\u304f"; // HIRAGANA KU
    
    public void testUTF8_1() throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException
    {
        Node nd = m_session.getRootNode().addNode(UTF8_NAME);
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
        return new TestSuite( FileProviderTest.class );
    }

}
