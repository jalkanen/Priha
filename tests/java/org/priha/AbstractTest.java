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
