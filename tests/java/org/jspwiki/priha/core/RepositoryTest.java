package org.jspwiki.priha.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.jcr.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jspwiki.priha.RepositoryManager;
import org.jspwiki.priha.TestUtil;
import org.jspwiki.priha.util.Path;

public class RepositoryTest extends TestCase
{
    Logger log = Logger.getLogger(RepositoryTest.class.getName());
    Repository m_repository;
    
    Session m_session;
    
    protected void setUp() throws Exception
    {
        m_repository = RepositoryManager.getRepository();

        TestUtil.emptyRepo(m_repository);
        
        m_session = m_repository.login();
    }
    
    protected void tearDown() throws Exception
    {
        m_session.logout();
        TestUtil.emptyRepo(m_repository);
    }
    
    public void testLogin() throws Exception
    {
        Node nd = m_session.getRootNode();
        
        assertEquals( 0, nd.getDepth() );
    }

    public void testEmptyRepo() throws Exception
    {
        Node nd = m_session.getRootNode().addNode("testemptyrepo");
        nd = nd.addNode("foo");
        nd.setProperty("Barbapapa","Barbamama");
        
        m_session.save();
        
        TestUtil.emptyRepo(m_repository);
        
        File f = new File("/tmp/priha/fileprovider/workspaces/default/");
        
        String[] paths = f.list();
        
        for( String p : paths )
        {
            if( p.startsWith("jcr:") ) continue; // This is okay
        
            fail( "This was not removed: "+p );
        }
    }
    
    public void testRemove() throws Exception
    {
        Session s = m_repository.login();
        
        s.getRootNode().addNode("test");
        
        s.save();
        
        assertTrue( "was not created", s.getRootNode().hasNode("/test") );
        
        Node nd = s.getRootNode().getNode("/test");
        
        nd.remove();
        
        s.save();

        assertFalse( "was not removed", s.getRootNode().hasNode("/test") );
        
        File f = new File("/tmp/priha/fileprovider/workspaces/default/test");
        assertFalse( "File was not removed", f.exists() );
    }
    
    public void testSave() throws Exception
    {
        Session s = m_repository.login();
        
        Node nd = s.getRootNode();
        
        Node newnode = nd.addNode("test");
        
        newnode.setProperty( "a", "foo" );
        
        s.save();
        
        Item it = s.getItem("/test");
        
        assertTrue( "is not node", it.isNode() );
        
        assertEquals( "wrong property", "foo",((Node)it).getProperty("a").getString());
    }

    public void testSave2() throws Exception
    {
        Session s = m_repository.login();
        
        Node nd = s.getRootNode();
        
        Node newnode = nd.addNode("test");
        
        newnode.setProperty( "a", "foo" );
        
        s.save();
        
        s.logout();
        
        s = m_repository.login();
        
        Item it = s.getItem("/test");
        
        assertTrue( "is not node", it.isNode() );
        
        assertEquals( "wrong property", "foo",((Node)it).getProperty("a").getString());
    }

    public void testRemoveProperty() throws Exception
    {
        Session s = m_repository.login();
        
        Node nd = s.getRootNode();
        
        nd.setProperty("foo", "bar");
        
        s.save();
                
        Item i = s.getItem("/foo");
        
        assertTrue( "wrong type", i instanceof Property );
        
        assertEquals( "wrong content", "bar", ((Property)i).getValue().getString() );
        
        i.remove();
        
        s.save();
        
        Session s2 = m_repository.login();
        
        try
        {
            s2.getItem("/foo");
            fail("Still got it");
        }
        catch( PathNotFoundException e ) 
        { 
            // Expected 
        }
        
        File f = new File("/tmp/priha/fileprovider/workspaces/default/foo.info");
        assertFalse( "File was not removed", f.exists() );
    }
    
    public void testTraversal() throws Exception
    {
        Session s = m_repository.login();
        
        Node nd = s.getRootNode();

        Node newnode = nd.addNode("test");
        newnode.setProperty( "a", "foo" );
        s.save();

        boolean testFound = false;
        
        for( NodeIterator it = nd.getNodes(); it.hasNext(); )
        {
            Node n = it.nextNode();
            
            if( n.getName().equals("test") )
            {
                testFound = true;
            }
            else if( n.getName().equals("jcr:system") )
            {
                // We ignore this
            }
            else
            {
                fail("Extraneous node found");
            }
        }
        
        assertTrue( "test node not found", testFound );
    }
    

    public void testBinaryProperty() throws Exception
    {
        Node nd = m_session.getRootNode();
        
        nd = nd.addNode("binarytest");
        
        String content = TestUtil.getUniqueID();
        
        InputStream in = new ByteArrayInputStream( content.getBytes() );
        
        Property p = nd.setProperty("blob", in );
        
        m_session.save();
        
        Property p2 = (Property) m_session.getItem("/binarytest/blob");
        
        assertEquals( content, p2.getString() );
    }
    
    public void testMultiProviders() throws Exception
    {
        Repository r = RepositoryManager.getRepository("multiprovidertest.properties");
        
        TestUtil.emptyRepo(r);
        
        Session s = r.login();
        Session s2 = r.login("testworkspace");
        
        Node nd = s2.getRootNode().addNode("largefiles");
        nd = nd.addNode("test");
        nd.setProperty("reallybig", 42);
        
        nd = s.getRootNode().addNode("small");
        nd.setProperty("reallysmall", "foobar");
        
        s.save();
        s2.save();
        
        File f = new File("/tmp/priha/fileprovider/workspaces/default/small/reallysmall.info");
        
        assertTrue("small", f.exists() );
        
        File f2 = new File("/tmp/priha/fileprovider2/workspaces/testworkspace/largefiles/test/reallybig.info");
        
        assertTrue("big", f2.exists() );
        
        Property p = (Property)s.getItem("/small/reallysmall");
        assertEquals("small content","foobar",p.getString());
        
        p = (Property)s2.getItem("/largefiles/test/reallybig");
        assertEquals("big content", p.getLong(), 42 );
        
        s.logout();
        s2.logout();
        
        TestUtil.emptyRepo(r);
        
        assertFalse("small remove", f.exists());
        assertFalse("big remove", f2.exists());
    }
    
    public static Test suite()
    {
        return new TestSuite( RepositoryTest.class );
    }
    
}
