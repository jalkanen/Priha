package org.apache.jackrabbit.test;

import java.util.Properties;

import javax.jcr.*;

import org.jspwiki.priha.RepositoryManager;
import org.jspwiki.priha.core.RepositoryImpl;

public class PrihaRepositoryStub extends RepositoryStub
{
    public PrihaRepositoryStub(Properties env)
    {
        super(env);
    }

    public Repository getRepository() throws RepositoryStubException
    {
        try
        {
            RepositoryImpl r = RepositoryManager.getRepository();

            if( r.getProperty("stub.initialized") == null )
            {
                String testws = getProperty(PROP_WORKSPACE_NAME);
                Session s = r.login(testws);

                String testroot = getProperty(PROP_TESTROOT);
                if( testroot == null) throw new RepositoryStubException("No testroot defined");

                if(testroot.startsWith("/")) testroot = testroot.substring(1);

                if( !s.getRootNode().hasNode(testroot) )
                {
                    Node nd = s.getRootNode().addNode(testroot);
                    nd = nd.addNode("footest");
                    nd.addMixin("mix:referenceable");
                    
                    s.save();
                }
                s.logout();
                r.setProperty("stub.initialized", "true");
            }

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
}
