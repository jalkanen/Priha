package org.priha.core;

import org.priha.core.GlobalNamespaceRegistryImpl;
import org.priha.core.NamespaceRegistryImpl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class NamespaceRegistryImplTest extends TestCase
{
    NamespaceRegistryImpl m_reg = new GlobalNamespaceRegistryImpl();
    
    public void testToQName()
        throws Exception
    {
        assertEquals( "{http://www.jcp.org/jcr/nt/1.0}unstructured", m_reg.toQName("nt:unstructured") );
    }
    
    public void testToQName2()
        throws Exception
    {
        assertEquals( "test", m_reg.toQName("test"));
    }
    
    public void testFromQName()
        throws Exception
    {
        assertEquals( "nt:unstructured", m_reg.fromQName("{http://www.jcp.org/jcr/nt/1.0}unstructured") );
    }

    public void testFromQName2()
        throws Exception
    {
        assertEquals( "test", m_reg.fromQName("test") );
    }

    public static Test suite()
    {
        return new TestSuite( NamespaceRegistryImplTest.class );
    }

}
