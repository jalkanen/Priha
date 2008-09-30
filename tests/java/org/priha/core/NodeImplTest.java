package org.priha.core;

import javax.jcr.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.priha.RepositoryManager;
import org.priha.core.RepositoryImpl;

public class NodeImplTest extends TestCase
{

    private RepositoryImpl m_repository;
    private Session m_session;
    private Session m_session2;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        
        m_repository = RepositoryManager.getRepository();
        m_session = m_repository.login();
        Node nd = m_session.getRootNode().addNode("gobble");
        nd.addMixin("mix:referenceable");
        m_session.save();
        
        m_session2 = m_repository.login();
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        
        Node nd = m_session.getRootNode();
        
        removeAll( nd.getNodes() );
        m_session.save();
        
        m_session.logout();
        m_session2.logout();
    }

    public static Test suite()
    {
        return new TestSuite( NodeImplTest.class );
    }
    
    private void removeAll( NodeIterator ni ) throws RepositoryException
    {
        while( ni.hasNext() )
        {
            Node nd = ni.nextNode();
            removeAll( nd.getNodes() );
        
            nd.remove();
        }        
    }
    
    public void testUUID() throws Exception
    {
        Node nd = (Node)m_session.getItem("/gobble");
        
        assertNotNull( "no uuid", nd.getUUID() );
        
        Node nd2 = m_session.getNodeByUUID( nd.getUUID() );
        
        assertEquals( "wrong uuid", nd.getPath(), nd2.getPath() );
    }

    public void testReferences() throws Exception
    {
        Node gobble = (Node)m_session.getItem("/gobble");

        Node nd = m_session.getRootNode().addNode( "zorp" );
        
        nd.addMixin("mix:referenceable");
        
        nd.setProperty("ownerOf", gobble);
        
        m_session.save();
        
        Node gobble2 = (Node)m_session.getItem("/gobble");
        
        PropertyIterator pi = gobble2.getReferences();
        
        assertEquals( "wrong # of refs", 1, pi.getSize() );
        
        Property p = pi.nextProperty();
        
        assertEquals( "wrong ref", "ownerOf", p.getName() );
        assertEquals( "wrong val", gobble2.getUUID(), p.getValue().getString() );
    }

    public void testSave() throws Exception
    {
        Node root = m_session.getRootNode();
        
        root.addNode("foo");
        
        root.save();
        
        Node test = m_session2.getRootNode().getNode("/foo");
        
        assertNotNull( test );
        
    }
    
    public void testMixinLoadSave() throws Exception
    {
        Node root = m_session.getRootNode();
        
        Node n = root.addNode("foo");
        n.addMixin("mix:referenceable");
        root.save();
        
        Node n2 = m_session2.getRootNode().getNode("foo");
        
        Property p = n2.getProperty("jcr:mixinTypes");
        
        Value[] v = p.getValues();
        
        boolean found = false;
        
        for( Value vv : v )
        {
            if( vv.getString().equals("mix:referenceable") ) { found = true; };
        }
        
        assertTrue("mix:referenceable not found",found);
        
        assertNotNull( n2.getUUID() );
    }
    
    public void testModified() throws Exception
    {
        Node root = m_session.getRootNode();
        
        Node n = root.addNode("foo");
        
        m_session.save();
        
        assertFalse( "Not modified", n.isModified() );
        
        n.setProperty("gii","blaa");
        
        assertTrue( "Property add didn't reflect up to the changed node", n.isModified() );
    }
}
