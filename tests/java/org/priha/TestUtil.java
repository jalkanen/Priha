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

import java.util.Random;

import javax.jcr.*;

public class TestUtil
{

    public static void emptyRepo(Repository repository) throws LoginException, RepositoryException
    {
        Session anonSession = repository.login();
     
        try
        {
            String[] workspaces = anonSession.getWorkspace().getAccessibleWorkspaceNames();
       
            for( String ws : workspaces )
            {
                //System.out.println("Emptying repo "+ws);
                Session s = repository.login(new SimpleCredentials("username","password".toCharArray()),ws);
        
                try
                {
                    s.refresh(false);
                    deleteTree( s.getRootNode() );
                    s.save();
                }
                finally
                {
                    s.logout();
                }
            }
        }
        catch( Exception e )
        {
            e.printStackTrace( System.err );
        }
        finally
        {
            anonSession.logout();
        }
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
        float itersSec = iters / ((float)time/1000);
        
        String itersSecString = "";
        if( itersSec < 1000.0 )   itersSecString = String.format( "%.0f", itersSec );
        else if( itersSec < 1e6 ) itersSecString = String.format( "%.2fk", itersSec/1000 );
        else if( itersSec < 1e9 ) itersSecString = String.format( "%.2fM", itersSec/1e6 );
        
        System.out.println( msg + ":" + iters + " iterations in "+time+" ms ("+itersSecString+" iterations/second)");
    }


    /**
     *  Returns a random string of sixteen uppercase characters.
     *
     *  @return A random string
     */
    public static String getUniqueID(int length)
    {
        StringBuffer sb = new StringBuffer();
        Random rand = new Random();
    
        for( int i = 0; i < length; i++ )
        {
            char x = (char)('A'+rand.nextInt(26));
    
            sb.append(x);
        }
    
        return sb.toString();
    }
}
