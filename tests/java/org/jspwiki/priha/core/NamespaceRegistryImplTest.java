package org.jspwiki.priha.core;

import org.jspwiki.priha.core.NamespaceRegistryImpl;

import junit.framework.TestCase;

public class NamespaceRegistryImplTest extends TestCase
{
    NamespaceRegistryImpl m_reg = new NamespaceRegistryImpl();
    
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

}
