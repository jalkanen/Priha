package org.jspwiki.priha;

import java.util.Iterator;

import javax.jcr.*;

public class TestUtil
{

    public static void emptyRepo(Repository repository) throws LoginException, RepositoryException
    {
        Session s = repository.login();
        
        s.refresh(false);
        deleteTree( s.getRootNode() );
        
        for( PropertyIterator i = s.getRootNode().getProperties(); i.hasNext(); )
        {
            Property p = i.nextProperty();
            
            p.remove();
        }
        
        s.save();        
    }

    
    public static void deleteTree( Node start ) throws RepositoryException
    {
        for( NodeIterator i = start.getNodes(); i.hasNext(); )
        {
            i.nextNode().remove();
        }
    }
}
