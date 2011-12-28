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
package org.priha.nodetype;

import java.io.InputStream;
import java.util.logging.LogManager;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.*;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.VersionException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.priha.RepositoryManager;
import org.priha.TestUtil;
import org.priha.core.RepositoryImpl;
import org.priha.core.SessionImpl;

public class QNodeTypeManagerTest extends TestCase
{
    private RepositoryImpl m_repository;
    private SessionImpl m_session;
    private NodeTypeManager m_mgr;

    @Override
    protected void setUp() throws Exception
    {
        InputStream in = getClass().getClassLoader().getResourceAsStream("testlog.properties");
        if( in != null )
        {
            LogManager.getLogManager().readConfiguration(in);
            System.out.println("Using logging configuration in testlog.properties");
        }

        m_repository = RepositoryManager.getRepository();
        
        TestUtil.emptyRepo(m_repository);

        m_session = m_repository.login(new SimpleCredentials("foo",new char[0]));

        m_mgr = m_session.getWorkspace().getNodeTypeManager();
    }

    private PropertyDefinition findDef( PropertyDefinition[] pd, String name )
    {
        for( PropertyDefinition p : pd )
        {
            if( p.getName().equals(name) ) return p;
        }

        return null;
    }

    public void testBase() throws NoSuchNodeTypeException, RepositoryException
    {
        NodeType base = m_mgr.getNodeType("nt:base");

        assertNotNull("No base type", base);

        assertNull( "Wrong primary item name", base.getPrimaryItemName() );

        PropertyDefinition[] pd = base.getPropertyDefinitions();

        assertEquals("wrong # of definitions", 2, pd.length);

        PropertyDefinition prim = findDef( pd, "jcr:primaryType" );
        assertNotNull( "no jcr:primaryType", prim );

        PropertyDefinition mix = findDef( pd, "jcr:mixinTypes" );
        assertNotNull( "no jcr:mixinTypes", mix );

    }

    public void testMixin() throws Exception
    {
        NodeType mixin = m_mgr.getNodeType("mix:referenceable");

        assertNotNull( "no mixin type", mixin );
    }

    public void testVersionable() throws Exception
    {
        NodeType mixin = m_mgr.getNodeType("mix:versionable");

        assertNotNull( "no versionable type", mixin );

        assertTrue( "vers", mixin.isNodeType("mix:versionable") );
        assertTrue( "is referenceable", mixin.isNodeType("mix:referenceable") );
    }

    public void testAllowAddNode() throws Exception
    {
        try
        {
            Node nd = m_session.getRootNode().addNode("test","nt:file");
        
            nd.addNode("globbo","nt:unstructured");
        
            m_session.save();
            
            fail("Allowed adding; should fail!");
        }
        catch( ConstraintViolationException e ) {/* Expected */ }       
    }

    public void testAllowAddNode2() throws Exception
    {
        Node nd = m_session.getRootNode().addNode("test","nt:file");
        
        nd.addNode("jcr:content","nt:unstructured");
        
        m_session.save();
        
        assertTrue( m_session.itemExists("/test/jcr:content") );
    }

    public void testUuid() throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException
    {
        Node nd = m_session.getRootNode().addNode("test", "nt:unstructured");
        
        nd.addMixin( "mix:referenceable" );
        
        m_session.save();
        
        Property uuid = nd.getProperty( "jcr:uuid" );
        
        PropertyDefinition pd = uuid.getDefinition();
        
        assertEquals( "parentversion", OnParentVersionAction.INITIALIZE, pd.getOnParentVersion() );
        assertTrue( "autocreated", pd.isAutoCreated() );
        assertTrue( "mandatory", pd.isMandatory() );
        assertFalse( "multiple", pd.isMultiple() );
    }
    
    public void testUuid2() throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException
    {
        Node nd = m_session.getRootNode().addNode("test", "nt:unstructured");
        
        nd.addMixin( "mix:referenceable" );
        
        m_session.save();
        
        nd = (Node) m_session.getItem( "/test" );
        
        for( PropertyIterator pi = nd.getProperties(); pi.hasNext(); )
        {
            Property p = pi.nextProperty();
            
            if( nd.getName().equals("jcr:uuid") )
            {
                PropertyDefinition pd = p.getDefinition();
        
                assertEquals( "parentversion", OnParentVersionAction.INITIALIZE, pd.getOnParentVersion() );
                assertTrue( "autocreated", pd.isAutoCreated() );
                assertTrue( "mandatory", pd.isMandatory() );
                assertFalse( "multiple", pd.isMultiple() );
            }
        }
    }

    public static Test suite()
    {
        return new TestSuite( QNodeTypeManagerTest.class );
    }
}
