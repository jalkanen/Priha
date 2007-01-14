package org.jspwiki.priha.core;

import javax.jcr.*;

import org.jspwiki.priha.RepositoryManager;


import junit.framework.TestCase;

public class RepositoryTest extends TestCase
{
    Repository m_repository;
    
    protected void setUp() throws Exception
    {
        m_repository = RepositoryManager.getRepository();
    }
    
    public void testLogin() throws Exception
    {
        Session s = m_repository.login();
        
        Node nd = s.getRootNode();
        
        assertEquals( 0, nd.getDepth() );
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
            else
            {
                fail("Extraneous node found");
            }
        }
        
        assertTrue( "test node not found", testFound );
    }
    
}
