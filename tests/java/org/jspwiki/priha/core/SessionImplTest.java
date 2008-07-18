package org.jspwiki.priha.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.jcr.Node;
import javax.jcr.Value;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jspwiki.priha.RepositoryManager;
import org.jspwiki.priha.TestUtil;
import org.jspwiki.priha.core.values.ValueFactoryImpl;
import org.jspwiki.priha.core.values.ValueImpl;

public class SessionImplTest extends TestCase
{
    private SessionImpl m_session;
    private RepositoryImpl m_repository;
    
    public void setUp() throws Exception
    {
        m_repository = RepositoryManager.getRepository();
        TestUtil.emptyRepo(m_repository);
        
        m_session = (SessionImpl) m_repository.login();
    }
    
    public void tearDown() throws Exception
    {
        m_session.logout();
        
        TestUtil.emptyRepo(m_repository);
    }

    public void testExportSystemView1() throws Exception
    {
        Node nd = m_session.getRootNode().addNode("foo");
        
        nd.setProperty("stringprop", "Barba<papa>");

        nd.setProperty("binaryprop", new ByteArrayInputStream("Barbabinary".getBytes()));
        
        nd.setProperty("multiprop", new Value[] { ValueFactoryImpl.getInstance().createValue("pimpim&"),
                                                  ValueFactoryImpl.getInstance().createValue("poppop\"")} );
        m_session.save();
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        m_session.exportSystemView("/", out, false, false);
        
        String s = out.toString("UTF-8");

        //System.out.println(s);
        assertTrue("Barbapapa wrong", s.indexOf("Barba&lt;papa&gt;") != -1 );
        assertTrue("pim wrong", s.indexOf("pimpim&amp;") != -1 );
        assertTrue("pop wrong", s.indexOf("poppop&quot;") != -1 );
    }
    
    public static Test suite()
    {
        return new TestSuite( SessionImplTest.class );
    }
    
   
}
