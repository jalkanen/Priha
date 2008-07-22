package org.jspwiki.priha;

import java.util.Random;

import javax.jcr.*;

public class TestUtil
{

    public static void emptyRepo(Repository repository) throws LoginException, RepositoryException
    {
        Session anonSession = repository.login();
     
        String[] workspaces = anonSession.getWorkspace().getAccessibleWorkspaceNames();
       
        for( String ws : workspaces )
        {
            //System.out.println("Emptying repo "+ws);
            Session s = repository.login(new SimpleCredentials("username","password".toCharArray()),ws);
        
            s.refresh(false);
            deleteTree( s.getRootNode() );
        /*
            for( PropertyIterator i = s.getRootNode().getProperties(); i.hasNext(); )
            {
                Property p = i.nextProperty();

                if( !p.getDefinition().isProtected() )
                    p.remove();
            }
*/
            s.save();
        }
        
        anonSession.logout();
    }

    
    public static void deleteTree( Node start ) throws RepositoryException
    {
        for( NodeIterator i = start.getNodes(); i.hasNext(); )
        {
            Node n = i.nextNode();
            if( !n.getDefinition().isProtected() && !n.getPath().equals("/jcr:system"))
                n.remove();
        }
    }


    public static void printSpeed( String msg, int iters, long start, long end )
    {
        long time = end - start;
        float itersSec = (iters*100)/((float)time/1000) / 100;
        
        System.out.println( msg + ":" + iters + " iterations in "+time+" ms ("+itersSec+" iterations/second)");
    }


    /**
     *  Returns a random string of six uppercase characters.
     *
     *  @return A random string
     */
    public static String getUniqueID()
    {
        StringBuffer sb = new StringBuffer();
        Random rand = new Random();
    
        for( int i = 0; i < 6; i++ )
        {
            char x = (char)('A'+rand.nextInt(26));
    
            sb.append(x);
        }
    
        return sb.toString();
    }
}
