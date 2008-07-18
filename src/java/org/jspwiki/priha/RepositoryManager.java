/* 
    Priha - A JSR-170 implementation library.

    Copyright (C) 2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jspwiki.priha;

import java.io.IOException;
import java.util.Properties;

import org.jspwiki.priha.core.RepositoryImpl;
import org.jspwiki.priha.util.ConfigurationException;
import org.jspwiki.priha.util.FileUtil;


/**
 *  This is the main API for getting yourself a Repository object, and as 
 *  such, probably the only class you will need outside the basic JCR classes
 *  (unless you want to develop a RepositoryProvider, but that's a whole another story).
 *  <p>
 *  The simplest way of getting yourself a Priha JCR Repository is to simply call
 *  <code>RepositoryManager.getRepository</code>, which will find your "priha.properties"
 *  file and return a ready-to-use repository.  This default repository is a singleton.
 *  <p>
 *  Simple basic usage example which stores "some text" to the repository, and
 *  then retrieves and prints it:
 *  <pre>
 *     Repository rep = RepositoryManager.getRepository();
 *     Session session = rep.login();
 *     
 *     Node nd = session.getRootNode().addNode("myfirstnode");
 *     nd.addProperty("myfirstproperty", "some text");
 *     session.save();
 *     
 *     Property newp = (Property)session.getItem("/myfirstnode/myfirstproperty");
 *     System.out.println( newp.getString() );
 *  </pre>
 *  
 */
// TODO: Add JNDI configuration somewhere here
public class RepositoryManager
{
    /**
     *  An instance of the default repository.
     */
    private static RepositoryImpl m_repository = null;
     
    private static final String[] PROPERTYPATHS = 
    {
        "/priha.properties",
        "/WEB-INF/priha.properties"
    };
    
    /**
     *  Returns a default repository object for no-pain setup.  The Repository
     *  object is shared among all requestors of the default repository.
     *  <p>
     *  This method will search for a "priha.properties" configuration file from
     *  your classpath (and /WEB-INF/) and will use that.  Failing to find one,
     *  it will use the internal defaults (from the built-in priha_default.properties),
     *  which almost certainly is not something you want - unless you just want
     *  to test Priha.
     *  
     *  @return
     *  @throws ClassNotFoundException
     *  @throws InstantiationException
     *  @throws IllegalAccessException
     */
    public static RepositoryImpl getRepository() throws ConfigurationException
    {
        try
        {
            if( m_repository == null ) 
                m_repository = getRepository( FileUtil.findProperties(PROPERTYPATHS) );
            
            return m_repository;
        }
        catch (IOException e)
        {
            throw new ConfigurationException("Unable to load property file:"+e.getMessage());
        }
    }
    
    /**
     *  Returns a new repository object based on the Properties given.  This method
     *  guarantees a new Repository object, so it's a good idea to cache whatever
     *  you get from here.
     *  <p>
     *  Note that if you do get two Repositories who share the same property file,
     *  you will almost certainly hit some nasty race conditions with the repository
     *  itself.  So be very, very careful.
     *  
     *  @param prefs
     *  @return
     *  @throws ConfigurationException
     */
    public static RepositoryImpl getRepository( Properties prefs ) throws ConfigurationException
    {
        return new RepositoryImpl( prefs );
    }
    
    public static RepositoryImpl getRepository( String propertyFilename ) throws ConfigurationException
    {
        String[] propertyPaths = { propertyFilename, 
                                   "/"+propertyFilename,
                                   "/WEB-INF/propertyFileName" };
        
        try
        {
            Properties props = FileUtil.findProperties(propertyPaths);
            
            if( props == null || props.isEmpty() )
            {
                throw new ConfigurationException("The defined property file could not be located");
            }
            
            return new RepositoryImpl(props);
        }
        catch (IOException e)
        {
            throw new ConfigurationException("Unable to find or read the property file!");
        }
    }
}
