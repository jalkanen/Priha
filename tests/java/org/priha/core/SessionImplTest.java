package org.priha.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.jcr.Node;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.priha.AbstractTest;
import org.priha.TestUtil;

public class SessionImplTest extends AbstractTest
{
    private SessionImpl m_session;
    
    public void setUp() throws Exception
    {
        super.setUp();
        TestUtil.emptyRepo(m_repository);
        
        m_session = m_repository.login(new SimpleCredentials("foo",new char[0]));
    }
    
    public void tearDown() throws Exception
    {
        m_session.logout();
        
        super.tearDown();
    }

    /**
     *  Makes sure nobody tampers with root node UUID.
     */
    public void testRootUUID() throws Exception
    {
        Node nd = m_session.getRootNode();
        
        assertTrue( "uuid missing", nd.hasProperty( "jcr:uuid" ) );
        
        assertEquals( "uuid value", "93b885ad-fe0d-3089-8df6-34904fd59f71", nd.getUUID() );
    }
    
    public void testExportSystemView1() throws Exception
    {
        Node nd = m_session.getRootNode().addNode("foo");
        
        nd.setProperty("stringprop", "Barba<papa>");

        nd.setProperty("binaryprop", new ByteArrayInputStream("Barbabinary".getBytes()));
        
        nd.setProperty("multiprop", new Value[] { m_session.getValueFactory().createValue("pimpim&"),
                                                  m_session.getValueFactory().createValue("poppop\"")} );
        m_session.save();
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        m_session.exportSystemView("/", out, false, false);
        
        String s = out.toString("UTF-8");

        System.out.println(s);
        assertTrue("Barbapapa wrong", s.indexOf("Barba&lt;papa&gt;") != -1 );
        assertTrue("pim wrong", s.indexOf("pimpim&amp;") != -1 );
        assertTrue("pop wrong", s.indexOf("poppop&quot;") != -1 );
    }
    
    public void testMove() throws Exception
    {
        Node nd = m_session.getRootNode().addNode("source");
        nd = nd.addNode("tobemoved");
        nd.setProperty("test", 20);
        nd = nd.addNode("childprop");
        nd.setProperty("test2", "foo");
        
        nd = m_session.getRootNode().addNode("dest");
        
        m_session.save();
        
        m_session.move("/source/tobemoved", "/dest/newnode");
        
        assertTrue( "node", m_session.hasNode("/dest/newnode") );
        assertEquals( "prop", 20, m_session.getRootNode().getProperty("dest/newnode/test").getLong() );
    }
    
    public static Test suite()
    {
        return new TestSuite( SessionImplTest.class );
    }
    
   
}
