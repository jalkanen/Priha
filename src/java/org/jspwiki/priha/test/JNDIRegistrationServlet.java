/*
    Priha - A JSR-170 implementation library.

    Copyright (C) 2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package org.jspwiki.priha.test;

import java.util.Enumeration;
import java.util.Properties;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.jspwiki.priha.RepositoryManager;
import org.jspwiki.priha.core.RepositoryImpl;

/**
 * This is a small helper class which can be used to register Priha
 * to the JSR-170 TCK web application.  It also shows you how to
 * put Priha into JNDI.
 * 
 * @author jalkanen
 *
 */
public class JNDIRegistrationServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    public void init() throws ServletException
    {
        super.init();
        
        Properties env = new Properties();

        Context jndiContext;
        
        RepositoryImpl repository = null;
        try
        {
            repository = RepositoryManager.getRepository();
            
            Session s = repository.login();
            
            s.getRootNode().addNode("testroot");
            s.getRootNode().addNode("testnode");
            s.getRootNode().addNode("testdata");
            
            s.save();
            
            s.logout();
        }
        catch (LoginException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (RepositoryException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        Enumeration names = getServletConfig().getInitParameterNames();
        while (names.hasMoreElements()) 
        {
            String name = (String) names.nextElement();
            if (name.startsWith("java.naming.")) 
            {
                env.put(name, getServletConfig().getInitParameter(name));
                getServletContext().log("  adding property to JNDI environment: " + name + "=" + env.getProperty(name));
            }
        }
        
        try 
        {
            jndiContext = new InitialContext(env);
            jndiContext.bind("priha.repository", repository);
            
            getServletContext().log("Priha added to JNDI under name 'priha.repository'");
        } 
        catch (NamingException e) 
        {
            e.printStackTrace();
        }

    }

}
