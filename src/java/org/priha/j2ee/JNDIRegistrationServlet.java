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
package org.priha.j2ee;

import java.util.Enumeration;
import java.util.Properties;

import javax.jcr.RepositoryException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.priha.RepositoryManager;
import org.priha.core.RepositoryImpl;

/**
 * <p>This is a small helper class which can be used to register Priha
 * to JNDI.  To use this, just initialize it in any web.xml file, like thus:
 * <pre>
 *    &lt;servlet>
 *      &lt;servlet-name>PrihaServlet&lt;/servlet-name>
 *      &lt;servlet-class>org.priha.j2ee.JNDIRegistrationServlet&lt;/servlet-class>
 *      &lt;load-on-startup>100&lt;/load-on-startup>
 *      
 *      &lt;!-- The following is optional.  If there is no propertyfile parameter
 *           stated, will attempt to find "priha.properties" from the classpath. -->
 *              
 *      &lt;init-param>
 *         &lt;param-name>propertyfile&lt;param-name>
 *         &lt;param-value>/foo/bar/priha.properties&lt;param-value>
 *      &lt;/init-param>
 *    &lt;/servlet>
 * </pre>
 * 
 * <p>Any servlet parameters which start with "java.naming." are added to the
 * InitialContext environment.
 * 
 * <p>In order then to fetch it in your web application, you would use something like this:
 * <pre>
 *    Repository repository;
 *    Properties environment = new Properties();
 *    String lookupName = "priha.repository";
 *    
 *    InitialContext initial = new InitialContext(environment);
 *    Object obj = initial.lookup(lookupName);
 *
 *    repository = (Repository)PortableRemoteObject.narrow(obj, Repository.class);
 * </pre>
 */
public class JNDIRegistrationServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    /**
     *  Value under which the Repository object is bound by default.  Value is {@value}.
     */
    public static final String  JNDI_NAME = "priha.repository";
    
    /**
     *  Servlet initialization parameter name for stating the name of the
     *  property file.  Value is {@value}.
     */
    public static final String  PARAM_PROPERTYFILE = "propertyfile";
    
    public void init() throws ServletException
    {
        super.init();
        
        Properties env = new Properties();

        Context jndiContext;
        
        RepositoryImpl repository = null;
        
        String propertyfile = getServletConfig().getInitParameter(PARAM_PROPERTYFILE);
        
        try
        {
            if( propertyfile == null )
                repository = RepositoryManager.getRepository();
            else
                repository = RepositoryManager.getRepository( propertyfile );
        }
        catch (RepositoryException e)
        {
            e.printStackTrace();
        }
        
        Enumeration<?> names = getServletConfig().getInitParameterNames();
        while( names.hasMoreElements() )
        {
            String name = (String) names.nextElement();
            if( name.startsWith("java.naming.") )
            {
                env.put(name, getServletConfig().getInitParameter(name));
                getServletContext().log("  adding property to JNDI environment: " + name + "=" + env.getProperty(name));
            }
        }
        
        try 
        {
            jndiContext = new InitialContext(env);
            jndiContext.bind(JNDI_NAME, repository);
            
            getServletContext().log("Priha added to JNDI under name '"+JNDI_NAME+"'");
        } 
        catch (NamingException e) 
        {
            e.printStackTrace();
        }

    }

}
