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
package org.jspwiki.priha.util;

import javax.jcr.RepositoryException;

/**
 *  Thrown when the configuration is faulty.
 */
public class ConfigurationException extends RepositoryException
{
    private static final long serialVersionUID = 1L;

    /**
     *  Construct a ConfigurationException.
     *  
     *  @param msg The exception message
     *  @param propertyKey If there is a property key, you can add it here. May be null.
     */
    public ConfigurationException( String msg, String propertyKey )
    {
        super( msg + ((propertyKey!=null) ? (":"+propertyKey) : ""));
    }
    
    public ConfigurationException( String msg )
    {
        this( msg, null );
    }
}
