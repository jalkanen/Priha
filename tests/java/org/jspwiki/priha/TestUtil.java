package org.jspwiki.priha;

import javax.jcr.*;

public class TestUtil
{

    public static void emptyRepo(Repository repository) throws LoginException, RepositoryException
    {
        Session anonSession = repository.login();
     
        String[] workspaces = anonSession.getWorkspace().getAccessibleWorkspaceNames();
       
        for( String ws : workspaces )
        {
            System.out.println("Emptying repo "+ws);
            Session s = repository.login(ws);
        
            s.refresh(false);
            deleteTree( s.getRootNode() );
        
            for( PropertyIterator i = s.getRootNode().getProperties(); i.hasNext(); )
            {
                Property p = i.nextProperty();
            
                p.remove();
            }
        
            s.save();
        }
        
        anonSession.logout();
    }

    
    public static void deleteTree( Node start ) throws RepositoryException
    {
        for( NodeIterator i = start.getNodes(); i.hasNext(); )
        {
            i.nextNode().remove();
        }
    }


    public static void printSpeed( String msg, int iters, long start, long end )
    {
        long time = end - start;
        float itersSec = (iters*100)/((float)time/1000) / 100;
        
        System.out.println( msg + ":" + iters + " iterations in "+time+" ms ("+itersSec+" iterations/second)");
    }
}
