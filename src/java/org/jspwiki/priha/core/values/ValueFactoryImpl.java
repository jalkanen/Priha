package org.jspwiki.priha.core.values;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.*;

import org.jspwiki.priha.core.NodeImpl;


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
    
    public ValueImpl createValue(Value value) throws ValueFormatException, IllegalStateException, RepositoryException
    {
        switch( value.getType() )
        {
            case PropertyType.BOOLEAN:
                return new BooleanValueImpl( value.getBoolean() );
                
            case PropertyType.DOUBLE:
                return new DoubleValueImpl( value.getDouble() );
                
            case PropertyType.LONG:
                return new LongValueImpl( value.getLong() );
                
            case PropertyType.NAME:
                return new NodeValueImpl( value.getString() );
                
            case PropertyType.PATH:
                return new NodeValueImpl( value.getString() );
                
            case PropertyType.REFERENCE:
                return new NodeValueImpl( value.getString() );
                
            case PropertyType.STRING:
                return new StringValueImpl( value.getString() );

        }
        
        
        throw new ValueFormatException("Illegal type "+ PropertyType.nameFromValue( value.getType() ) );
    }
    
    public ValueImpl createValue(String value)
    {
        return new StringValueImpl(value);
    }

    public ValueImpl createValue(long value)
    {
        return new LongValueImpl(value);
    }

    public ValueImpl createValue(double value)
    {
        return new DoubleValueImpl(value);
    }

    public ValueImpl createValue(boolean value)
    {
        return new BooleanValueImpl(value);
    }

    public ValueImpl createValue(Calendar value)
    {
        return new CalendarValueImpl(value);
    }

    public ValueImpl createValue(InputStream value)
    {
        return new StreamValueImpl(value);
    }

    public ValueImpl createValue(Node value) throws RepositoryException
    {
        return new NodeValueImpl( (NodeImpl)value, PropertyType.REFERENCE );
    }

    public ValueImpl createValue(String value, int type) throws ValueFormatException
    {
        switch( type )
        {
            case PropertyType.BOOLEAN:
                return new BooleanValueImpl(value);
                
            case PropertyType.DOUBLE:
                return new DoubleValueImpl(value);
                
            case PropertyType.LONG:
                return new LongValueImpl(value);
                
            case PropertyType.NAME:
                return new NodeValueImpl(value,type);
                
            case PropertyType.PATH:
                return new NodeValueImpl(value,type);
                
            case PropertyType.REFERENCE:
                return new NodeValueImpl(value,type);
                
            case PropertyType.STRING:
                return new StringValueImpl(value);
        }
        
        throw new ValueFormatException("Illegal type "+PropertyType.nameFromValue(type));
    }

}
