/*
    Priha - A JSR-170 implementation library.

    Copyright (C) 2007-2009 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    Licensed under the Apache License, Version 2.0 (the "License"); 
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at 
    
      http://www.apache.org/licenses/LICENSE-2.0 
      
    Unless required by applicable law or agreed to in writing, software 
    distributed under the License is distributed on an "AS IS" BASIS, 
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
    See the License for the specific language governing permissions and 
    limitations under the License. 
 */
package org.priha.core;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.priha.AbstractTest;
import org.priha.core.namespace.NamespaceRegistryImpl;
import org.priha.util.QName;

public class NamespaceRegistryImplTest extends AbstractTest
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
