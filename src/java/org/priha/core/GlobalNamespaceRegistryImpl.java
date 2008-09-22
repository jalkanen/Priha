/*
    Priha - A JSR-170 implementation library.

    Copyright (C) 2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import javax.jcr.*;

import org.xml.sax.helpers.NamespaceSupport;

/**
 *  Provides the global name space mappings, which are unmodifiable.
 */
public class GlobalNamespaceRegistryImpl extends NamespaceRegistryImpl implements NamespaceRegistry
{
    public static final String NS_JCP_SV  = "http://www.jcp.org/jcr/sv/1.0";
    public static final String NS_JCP_MIX = "http://www.jcp.org/jcr/mix/1.0";
    public static final String NS_JCP_NT  = "http://www.jcp.org/jcr/nt/1.0";
    public static final String NS_JCP     = "http://www.jcp.org/jcr/1.0";
    
    public GlobalNamespaceRegistryImpl()
    {
        super();
        m_nsmap.put("jcr", NS_JCP);
        m_nsmap.put("nt",  NS_JCP_NT);
        m_nsmap.put("mix", NS_JCP_MIX);
        m_nsmap.put("xml", NamespaceSupport.XMLNS);
        m_nsmap.put("sv",  NS_JCP_SV);
        m_nsmap.put("", "");
    }

    @Override
    public void registerNamespace(String prefix, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException
    {
        throw new NamespaceException("Global namespaces cannot be changed.");
    }

    @Override
    public void unregisterNamespace(String prefix) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException
    {
        throw new NamespaceException("Global namespaces cannot be changed.");
    }

}
