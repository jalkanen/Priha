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
