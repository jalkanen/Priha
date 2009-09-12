package org.priha.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.jcr.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.priha.RepositoryManager;
import org.priha.TestUtil;

public class RepositoryTest extends TestCase
{
    Logger log = Logger.getLogger(RepositoryTest.class.getName());
    Repository m_repository;
    
    Session m_session;
    
    protected void setUp() throws Exception
    {
        m_repository = RepositoryManager.getRepository();

        TestUtil.emptyRepo(m_repository);
        
        m_session = m_repository.login(new SimpleCredentials("foo",new char[0]));
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
            if( p.startsWith("jcr:") || p.startsWith( "priha:") ) continue; // This is okay
        
            fail( "This was not removed: "+p );
        }
    }
    
    public void testRemove() throws Exception
    {
        Session s = m_repository.login(new SimpleCredentials("foo",new char[0]));
        
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
        Session s = m_repository.login(new SimpleCredentials("foo",new char[0]));
        
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
        Session s = m_repository.login(new SimpleCredentials("foo",new char[0]));
        
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
        Session s = m_repository.login(new SimpleCredentials("foo",new char[0]));
        
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
        Session s = m_repository.login(new SimpleCredentials("foo",new char[0]));
        
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
        
        String content = TestUtil.getUniqueID(32);
        
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
        
        Session s = r.login(new SimpleCredentials("foo",new char[0]));
        Session s2 = r.login(new SimpleCredentials("foo",new char[0]),"testworkspace");
        
        assertFalse( "Must end up in different workspaces", s.getWorkspace().getName().equals(s2.getWorkspace().getName()) );
        
        Node nd = s2.getRootNode().addNode("largefiles");
        nd = nd.addNode("test");
        nd.setProperty("reallybig", 42);
        
        nd = s.getRootNode().addNode("small");
        nd.setProperty("reallysmall", "foobar");
        
        s.save();
        s2.save();
        
        File f = new File("/tmp/priha-multi/fileprovider/workspaces/default/small/reallysmall.info");
        
        assertTrue("small", f.exists() );
        
        File f2 = new File("/tmp/priha-multi/fileprovider2/workspaces/testworkspace/largefiles/test/reallybig.info");
        
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
    
    
    public void testSameNameSiblings() throws Exception
    {
        Node nd = m_session.getRootNode().addNode("test", "nt:unstructured");

        Node nd21 = nd.addNode( "samename", "nt:unstructured" );
        nd21.setProperty( "order", 1 );
        Node nd22 = nd.addNode( "samename", "nt:unstructured" );
        nd22.setProperty( "order", 2 );
        Node nd23 = nd.addNode( "samename", "nt:unstructured" );
        nd23.setProperty( "order", 3 );
        
        m_session.save();
        
        nd = (Node) m_session.getItem("/test/samename[1]");
        assertEquals( "one", 1, nd.getProperty( "order" ).getLong() );

        nd = (Node) m_session.getItem("/test/samename[2]");
        assertEquals( "two", 2, nd.getProperty( "order" ).getLong() );

        nd = (Node) m_session.getItem("/test/samename[3]");
        assertEquals( "three", 3, nd.getProperty( "order" ).getLong() );
    }

    public void testSameNameSiblingsRemoval() throws Exception
    {
        Node nd = m_session.getRootNode().addNode("test", "nt:unstructured");

        Node nd21 = nd.addNode( "samename", "nt:unstructured" );
        nd21.setProperty( "order", 1 );
        Node nd22 = nd.addNode( "samename", "nt:unstructured" );
        nd22.setProperty( "order", 2 );
        Node nd23 = nd.addNode( "samename", "nt:unstructured" );
        nd23.setProperty( "order", 3 );
        
        m_session.save();
        
        nd22.remove();
        m_session.save();
        
        nd = (Node) m_session.getItem("/test/samename[1]");
        assertEquals( "one", 1, nd.getProperty( "order" ).getLong() );

        nd = (Node) m_session.getItem("/test/samename[2]");
        assertEquals( "samename[3] not moved to samename[2]", 3, nd.getProperty( "order" ).getLong() );
    }

    public static Test suite()
    {
        return new TestSuite( RepositoryTest.class );
    }
    
}
