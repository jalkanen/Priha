package org.priha.util;

import java.io.IOException;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;

import org.priha.RepositoryManager;
import org.priha.core.RepositoryImpl;
import org.priha.core.SessionImpl;

/**
 *  A simple class which just exports a Priha repository as an XML document.
 */
public class Exporter
{
    public static void main( String[] args ) throws LoginException, RepositoryException, IOException
    {
        String propFileName = null;
        
        if( args.length > 0 )
            propFileName = args[0];
        
        RepositoryImpl ri;
        
        if( propFileName == null) ri = RepositoryManager.getRepository();
        else ri = RepositoryManager.getRepository(propFileName);
        
        SessionImpl session = ri.login();
        
        session.exportSystemView( "/", System.out, false, false );
    }

}
