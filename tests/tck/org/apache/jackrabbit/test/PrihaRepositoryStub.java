package org.apache.jackrabbit.test;

import java.util.Properties;

import javax.jcr.*;

import org.priha.RepositoryManager;
import org.priha.core.RepositoryImpl;

public class PrihaRepositoryStub extends RepositoryStub
{
    static RepositoryImpl c_repo;
    
    public PrihaRepositoryStub(Properties env)
    {
        super(env);
    }

    public Repository getRepository() throws RepositoryStubException
    {
        if( c_repo != null )
            return c_repo;
        
        try
        {
            RepositoryImpl r = RepositoryManager.getRepository("jdbcnocache.properties");
//            RepositoryImpl r = RepositoryManager.getRepository("filenocache.properties");
            
            c_repo = r;
            
            Session s = r.login( getSuperuserCredentials() );

            String testroot = getProperty(PROP_PREFIX + "." + PROP_TESTROOT);
            if( testroot == null) throw new RepositoryStubException("No testroot defined");

            if(testroot.startsWith("/")) testroot = testroot.substring(1);

            Node testRoot;
                
            if( !s.getRootNode().hasNode(testroot) )
            {
                testRoot = s.getRootNode().addNode(testroot);
            }
            else
            {
                testRoot = s.getRootNode().getNode(testroot);
            }

            if( !s.getRootNode().hasNode("querytest") )
            {
                //
                //  Create some Nodes with properties for Query tests.
                //
                Node nd = s.getRootNode().addNode("querytest");
                nd.addMixin("mix:referenceable");
                nd.setProperty( getProperty(PROP_PREFIX + "." + PROP_PROP_NAME1), "mofa" );
                    
                Node nd2 = nd.addNode( PROP_NODE_NAME1 );
                nd2.setProperty( getProperty(PROP_PREFIX + "." + PROP_PROP_NAME1), "fafa" );

                nd2 = nd.addNode( PROP_NODE_NAME2 );
                nd2.setProperty( getProperty(PROP_PREFIX + "." + PROP_PROP_NAME1), "famo" );
            }
                
            s.save();
                
            s.logout();

            return r;
        }
        catch (LoginException e)
        {
            throw new RepositoryStubException(e.getMessage());
        }
        catch (NoSuchWorkspaceException e)
        {
            throw new RepositoryStubException(e.getMessage());
        }
        catch (RepositoryException e)
        {
            e.printStackTrace();
            throw new RepositoryStubException(e.getMessage());
        }
    }

    @Override
    public Credentials getReadOnlyCredentials()
    {
        return null;
    }

    @Override
    public Credentials getReadWriteCredentials()
    {
        return new SimpleCredentials("foo", new char[0]);
    }

    @Override
    public Credentials getSuperuserCredentials()
    {
        return new SimpleCredentials("foo", new char[0]);
    }
    
    
    
}
