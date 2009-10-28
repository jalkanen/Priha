package org.priha;

import javax.jcr.*;

import junit.framework.TestCase;

import org.priha.core.RepositoryImpl;
import org.priha.core.SessionImpl;

public abstract class AbstractTest extends TestCase
{
    protected RepositoryImpl m_repository;
    protected static final String WORKSPACE = "default";
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        
        m_repository = RepositoryManager.getRepository();
    }

    protected SessionImpl getNewSession() throws LoginException, RepositoryException
    {
        return m_repository.login( new SimpleCredentials("foo", new char[0]) );
    }
    
    protected void tearDown() throws Exception
    {
        super.tearDown();

        TestUtil.emptyRepo( m_repository );
        SessionImpl session = m_repository.superUserLogin( WORKSPACE );
        try
        {
            Node nd = session.getRootNode();

            NodeIterator ni = nd.getNodes();
        
            // jcr:system
            assertTrue("Repository not empty: "+ni.getSize()+" Nodes left", ni.getSize() == 1);
        
            PropertyIterator pi = nd.getProperties();
        
            // jcr:uuid, jcr:mixinTypes and jcr:primaryType
            assertTrue("Properties not removed: "+pi.getSize()+" Props left", pi.getSize() == 3);
        }
        finally
        {
            session.logout();
        }
    }
}
