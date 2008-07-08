package org.jspwiki.priha.core;

import java.io.File;
import java.util.Random;
import java.util.logging.Logger;

import javax.jcr.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jspwiki.priha.RepositoryManager;
import org.jspwiki.priha.TestUtil;

public class RepositoryTest extends TestCase
{
    Logger log = Logger.getLogger(RepositoryTest.class.getName());
    Repository m_repository;
    
    protected void setUp() throws Exception
    {
        m_repository = RepositoryManager.getRepository();

        TestUtil.emptyRepo(m_repository);
    }
    
    protected void tearDown() throws Exception
    {
        TestUtil.emptyRepo(m_repository);
    }
    
    public void testLogin() throws Exception
    {
        Session s = m_repository.login();
        
        Node nd = s.getRootNode();
        
        assertEquals( 0, nd.getDepth() );
    }

    public void testEmptyRepo() throws Exception
    {
        Session s = m_repository.login();
        
        s.getRootNode().addNode("testemptyrepo");
        
        s.save();
        
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
        log.info("testRemoveProperty");
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
    
    public void testMillionSaves() throws Exception
    {
        Session s = m_repository.login();

        Node nd = s.getRootNode();
        
        long start = System.currentTimeMillis();

        for( int i = 0; i < 1; i++ )
        {
            String name = "x-"+getUniqueID();
            
            nd.addNode( name );
            nd.setProperty( "test", getUniqueID() );
        }
        
        s.save();
        
        long stop = System.currentTimeMillis();
        System.out.println("Saves took "+(stop-start)+"ms");
        
        start = System.currentTimeMillis();
        
        nd = s.getRootNode();
        
        for( NodeIterator i = nd.getNodes(); i.hasNext(); )
        {
            Node n = i.nextNode();
            
            //  Skip nodes which weren't created in this test.
            if( n.getName().startsWith("x-") )
            {
                Property p = n.getProperty("test");
                assertEquals( p.getName(), 6, p.getString().length() );
            }
        }
        
        stop = System.currentTimeMillis();
        System.out.println("Reads took "+(stop-start)+"ms");
    }

    /**
     *  Returns a random string of six uppercase characters.
     *
     *  @return A random string
     */
    private static String getUniqueID()
    {
        StringBuffer sb = new StringBuffer();
        Random rand = new Random();

        for( int i = 0; i < 6; i++ )
        {
            char x = (char)('A'+rand.nextInt(26));

            sb.append(x);
        }

        return sb.toString();
    }
    public static Test suite()
    {
        return new TestSuite( RepositoryTest.class );
    }
    
}
