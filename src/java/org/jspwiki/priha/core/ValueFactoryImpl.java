package org.jspwiki.priha.core;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.*;

public class ValueFactoryImpl implements ValueFactory
{
    private static ValueFactoryImpl c_factory = null;
    
    public static ValueFactoryImpl getInstance()
    {
        if( c_factory == null )
        {
            c_factory = new ValueFactoryImpl();
        }
        
        return c_factory;
    }
    
    public Value createValue(String value)
    {
        return new ValueImpl(value);
    }

    public Value createValue(long value)
    {
        return new ValueImpl(value);
    }

    public Value createValue(double value)
    {
        return new ValueImpl(value);
    }

    public Value createValue(boolean value)
    {
        return new ValueImpl(value);
    }

    public Value createValue(Calendar value)
    {
        return new ValueImpl(value);
    }

    public Value createValue(InputStream value)
    {
        return new ValueImpl(value);
    }

    public Value createValue(Node value) throws RepositoryException
    {
        throw new UnsupportedRepositoryOperationException("ValueFactory.createValue()");
    }

    public Value createValue(String value, int type) throws ValueFormatException
    {
        return new ValueImpl( value, type );
    }

}
