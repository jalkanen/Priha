package org.jspwiki.priha.core;

import javax.jcr.*;

import junit.framework.TestCase;

import org.jspwiki.priha.RepositoryManager;

public class NodeImplTest extends TestCase
{

    private RepositoryImpl m_repository;
    private Session m_session;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        
        m_repository = RepositoryManager.getRepository();
        m_session = m_repository.login();
        Node nd = m_session.getRootNode().addNode("gobble");
        nd.addMixin("mix:referenceable");
        m_session.save();
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        
        Node nd = m_session.getRootNode();
        
        removeAll( nd.getNodes() );
        m_session.save();
    }

    private void removeAll( NodeIterator ni ) throws RepositoryException
    {
        while( ni.hasNext() )
        {
            Node nd = ni.nextNode();
            removeAll( nd.getNodes () );
        
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

}
