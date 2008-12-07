package org.priha.query;

import javax.jcr.*;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.priha.RepositoryManager;
import org.priha.TestUtil;
import org.priha.util.ConfigurationException;
import org.priha.util.PathTest;

public class XPathTest extends TestCase
{
    private Session m_session;
    private QueryManager m_mgr;
    
    public void setUp() throws LoginException, ConfigurationException, RepositoryException
    {
        TestUtil.emptyRepo(RepositoryManager.getRepository());
        m_session = RepositoryManager.getRepository().login();
        
        Node nd = m_session.getRootNode();
        
        nd = nd.addNode("bookstore");
        nd = nd.addNode("book");
        nd.setProperty("title","How to cook humans");
        nd.setProperty("price",35.0);
        
        nd = nd.getParent().addNode("book2");
        nd.setProperty("title","How to cook forty humans");
        nd.setProperty("price",42.0);       

        nd = nd.getParent().addNode("funbooks");
        
        nd = nd.addNode("book");
        nd.setProperty("title","How to cook for forty humans");
        nd.setProperty("price",19.0);       

        m_session.save();
        
        m_mgr = m_session.getWorkspace().getQueryManager();
    }
    
    public void tearDown() throws LoginException, RepositoryException
    {
        TestUtil.emptyRepo(m_session.getRepository());

        m_session.logout();
    }

    private void checkMatchedPaths( QueryResult qr, String... paths ) throws RepositoryException
    {
        int idx = 0;

        assertEquals("wrong number of results", paths.length, qr.getNodes().getSize() );
    
        for( NodeIterator ni = qr.getNodes(); ni.hasNext(); idx++ )
        {
            Node totest = ni.nextNode();
            
            assertEquals( paths[idx], paths[idx], totest.getPath() );
        }
    }
    
    public void testNodeQuery1() throws InvalidQueryException, RepositoryException
    {
        Query q = m_mgr.createQuery( "/bookstore/book", Query.XPATH );
        
        QueryResult qr = q.execute();
        
        checkMatchedPaths( qr, "/bookstore/book" );
    }

    public void testNodeQuery2() throws InvalidQueryException, RepositoryException
    {
        Query q = m_mgr.createQuery( "/bookstore/book/title", Query.XPATH );
        
        QueryResult qr = q.execute();

        checkMatchedPaths( qr );
    }

    public void testQuery1() throws InvalidQueryException, RepositoryException
    {
        Query q = m_mgr.createQuery( "/bookstore/book[@title]", Query.XPATH );
        
        QueryResult qr = q.execute();
        checkMatchedPaths( qr, "/bookstore/book" );
    }

    public void testQuery2() throws InvalidQueryException, RepositoryException
    {
        Query q = m_mgr.createQuery( "//*[@title]", Query.XPATH );
        
        QueryResult qr = q.execute();

        checkMatchedPaths( qr, "/bookstore/book", "/bookstore/book2", "/bookstore/funbooks/book" );
    }

    public void testQuery3() throws InvalidQueryException, RepositoryException
    {
        Query q = m_mgr.createQuery( "//*", Query.XPATH );
        
        QueryResult qr = q.execute();

        checkMatchedPaths( qr,  "/bookstore", "/bookstore/book", "/bookstore/book2", "/bookstore/funbooks", "/bookstore/funbooks/book");
    }
    
    public void testQuery4() throws InvalidQueryException, RepositoryException
    {
        Query q = m_mgr.createQuery( "/bookstore//book", Query.XPATH );
        
        QueryResult qr = q.execute();

        checkMatchedPaths( qr, "/bookstore/book", "/bookstore/funbooks/book" );
    }

    public void testQuery5() throws InvalidQueryException, RepositoryException
    {
        Query q = m_mgr.createQuery( "//book", Query.XPATH );
        
        QueryResult qr = q.execute();

        checkMatchedPaths( qr, "/bookstore/book", "/bookstore/funbooks/book" );
    }

    public void testQuery6() throws InvalidQueryException, RepositoryException
    {
        Query q = m_mgr.createQuery( "//funbooks/book", Query.XPATH );
        
        QueryResult qr = q.execute();

        checkMatchedPaths( qr, "/bookstore/funbooks/book" );
    }

    public void testDoubleValueQuery() throws InvalidQueryException, RepositoryException
    {
        Query q = m_mgr.createQuery( "//*[price > 40.0]", Query.XPATH );
        
        QueryResult qr = q.execute();

        checkMatchedPaths( qr, "/bookstore/book2" );
    }

    
    public static Test suite()
    {
        return new TestSuite( XPathTest.class );
    }
}
