package org.jspwiki.priha.core;

import javax.jcr.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jspwiki.priha.RepositoryManager;

public class RepositoryTest extends TestCase
{
    Repository m_repository;
    
    protected void setUp() throws Exception
    {
        m_repository = RepositoryManager.getRepository();

        Session s = m_repository.login();
        
        s.refresh(false);
        deleteTree( s.getRootNode() );
        
        s.save();
    }
    
    protected void tearDown() throws Exception
    {
        Session s = m_repository.login();
        
        s.refresh(false);
        deleteTree( s.getRootNode() );
        
        s.save();
    }
    
    public static void deleteTree( Node start ) throws RepositoryException
    {
        for( NodeIterator i = start.getNodes(); i.hasNext(); )
        {
            deleteTree( i.nextNode() );
        }
        
        if( start.getDepth() > 0 )
        {
            start.remove();
        }
    }
    
    public void testLogin() throws Exception
    {
        Session s = m_repository.login();
        
        Node nd = s.getRootNode();
        
        assertEquals( 0, nd.getDepth() );
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

    public static Test suite()
    {
        return new TestSuite( RepositoryTest.class );
    }
    
}
