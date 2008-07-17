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
import java.util.prefs.Preferences;

import org.jspwiki.priha.core.ProviderManager;
import org.jspwiki.priha.core.RepositoryImpl;
import org.jspwiki.priha.util.ConfigurationException;
import org.jspwiki.priha.util.FileUtil;


/**
 *  This is the main API for getting yourself a Repository object
 *  
 *  @author jalkanen
 *
 */
public class RepositoryManager
{
    private static RepositoryImpl m_repository = null;
     
    private static final String[] PROPERTYPATHS = 
    {
        "/priha.properties",
        "/WEB-INF/priha.properties"
    };
    
    /**
     *  Returns a default repository object for no-pain setup.
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
            return getRepository( FileUtil.findProperties(PROPERTYPATHS) );
        }
        catch (IOException e)
        {
            throw new ConfigurationException("Unable to load property file:"+e.getMessage());
        }
    }
    
    public static RepositoryImpl getRepository( Properties prefs ) throws ConfigurationException
    {
        if( m_repository == null )
            m_repository = new RepositoryImpl( prefs );
        
        return m_repository;
    }
    
}
