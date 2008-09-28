package org.priha.core;

import javax.xml.namespace.QName;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.priha.core.namespace.NamespaceRegistryImpl;

public class NamespaceRegistryImplTest extends TestCase
{
    NamespaceRegistryImpl m_reg = new NamespaceRegistryImpl();
    
    public void testToQName()
        throws Exception
    {
        assertEquals( "{http://www.jcp.org/jcr/nt/1.0}unstructured", m_reg.toQName("nt:unstructured").toString() );
    }
    
    public void testToQName2()
        throws Exception
    {
        assertEquals( "test", m_reg.toQName("test").toString());
    }
    
    public void testFromQName()
        throws Exception
    {
        assertEquals( "nt:unstructured", m_reg.fromQName( QName.valueOf("{http://www.jcp.org/jcr/nt/1.0}unstructured")) );
    }

    public void testFromQName2()
        throws Exception
    {
        assertEquals( "test", m_reg.fromQName( QName.valueOf("test")) );
    }

    public static Test suite()
    {
        return new TestSuite( NamespaceRegistryImplTest.class );
    }

}
