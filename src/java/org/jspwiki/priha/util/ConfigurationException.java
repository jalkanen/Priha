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
