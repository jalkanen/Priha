package org.priha.core;

import javax.jcr.*;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.priha.AbstractTest;
import org.priha.RepositoryManager;

public class NodeImplTest extends AbstractTest
{
    private Session m_session;
    private Session m_session2;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        
        m_session = m_repository.login(new SimpleCredentials("foo",new char[0]));
        Node nd = m_session.getRootNode().addNode("gobble");
        nd.addMixin("mix:referenceable");
        m_session.save();
        
        m_session2 = m_repository.login(new SimpleCredentials("foo",new char[0]));
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        
        m_session.logout();
        m_session2.logout();
    }

    public static Test suite()
    {
        return new TestSuite( NodeImplTest.class );
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

    /** Multiple references */
    public void testReferences2() throws Exception
    {
        Node gobble = (Node)m_session.getItem("/gobble");

        Node nd = m_session.getRootNode().addNode( "zorp" );
        nd.addMixin("mix:referenceable");
        nd.setProperty("ownerOf", gobble);
        
        nd = m_session.getRootNode().addNode( "burp" );
        nd.addMixin("mix:referenceable");
        nd.setProperty("ownerOf", gobble);
        
        m_session.save();
        
        Node gobble2 = (Node)m_session.getItem("/gobble");
        
        PropertyIterator pi = gobble2.getReferences();
        
        assertEquals( "wrong # of refs", 2, pi.getSize() );
        
        Property p = pi.nextProperty();
        Property p2 = pi.nextProperty();
        
        assertEquals( "wrong ref", "ownerOf", p.getName() );
        assertEquals( "wrong val", gobble2.getUUID(), p.getValue().getString() );

        assertEquals( "wrong ref 2", "ownerOf", p2.getName() );
        assertEquals( "wrong val 2", gobble2.getUUID(), p2.getValue().getString() );
    }

    public void testSave() throws Exception
    {
        Node root = m_session.getRootNode();
        
        root.addNode("foo");
        
        root.save();
        
        Node test = m_session2.getRootNode().getNode("/foo");
        
        assertNotNull( test );
        
    }
    
    public void testGetNodes() throws Exception
    {
        Node root = m_session.getRootNode();
        
        if( root.hasNode( "getnodestest" ) )
        {
            fail("Repo not empty");
        }
        
        Node x = root.addNode("getnodestest");
        
        x.addNode("foo");
        x.addNode("bar");
        x.addNode("gobble");
        
        root.save();
        
        NodeIterator i = x.getNodes();
        
        assertEquals("3", 3, i.getSize());
        assertEquals("foo", "foo", i.nextNode().getName());
        assertEquals("bar", "bar", i.nextNode().getName());
        assertEquals("gobble", "gobble", i.nextNode().getName());
        
        try
        {
            i.nextNode();
            fail("Got past node count");
        }
        catch( Exception e )
        {}
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
    
    public void testDeleteAndReplace() throws Exception
    {
        Node n = m_session.getRootNode().addNode( "foo" );
        
        n.setProperty( "prop1", "testproperty" );
        
        m_session.save();
        
        assertTrue( "property never appeared", n.hasProperty( "prop1" ) );
        
        // Remove
        Property p = n.getProperty( "prop1" );
        p.remove();
        n.save();

        assertFalse( "property still here", n.hasProperty( "prop1" ) );
        
        n.setProperty( "prop1", "new value" );
        
        n.save();
        
        assertTrue( "property disappeared", n.hasProperty( "prop1" ) );
        assertEquals( "property value", "new value", n.getProperty( "prop1" ).getString());
    }
}
