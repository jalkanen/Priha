package org.jspwiki.priha.nodetype;

import java.io.InputStream;
import java.util.logging.LogManager;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jspwiki.priha.RepositoryManager;
import org.jspwiki.priha.core.RepositoryImpl;

public class NodeTypeManagerImplTest extends TestCase
{
    private RepositoryImpl m_repository;
    private Session m_session;
    private NodeTypeManagerImpl m_mgr;

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
        m_session = m_repository.login();

        m_mgr = (NodeTypeManagerImpl)m_session.getWorkspace().getNodeTypeManager();
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


    public static Test suite()
    {
        return new TestSuite( NodeTypeManagerImplTest.class );
    }
}
