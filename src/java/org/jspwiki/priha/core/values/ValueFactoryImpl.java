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
package org.jspwiki.priha.core.values;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.*;

import org.jspwiki.priha.core.NodeImpl;
import org.jspwiki.priha.core.binary.BinarySource;
import org.jspwiki.priha.util.FileUtil;


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
                return new NameValueImpl( value.getString() );
                
            case PropertyType.PATH:
                return new PathValueImpl( value.getString() );
                
            case PropertyType.REFERENCE:
                return new ReferenceValueImpl( value.getString() );
                
            case PropertyType.STRING:
                return new StringValueImpl( value.getString() );

            case PropertyType.DATE:
                return new CalendarValueImpl( value.getDate() );
                
            case PropertyType.BINARY:
                return new StreamValueImpl( (ValueImpl)value );
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
        try
        {
            return new StreamValueImpl(value);
        }
        catch (ValueFormatException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public ValueImpl createValue(Node value) throws RepositoryException
    {
        return new ReferenceValueImpl( (NodeImpl)value );
    }

    public ValueImpl createValue(BinarySource source)
    {
        return new StreamValueImpl(source);
    }


    public ValueImpl createValue(boolean value, int type) throws ValueFormatException
    {
        switch( type )
        {
            case PropertyType.BOOLEAN:
                return new BooleanValueImpl(value);
                                
            case PropertyType.STRING:
                return new StringValueImpl( Boolean.toString(value) );
                
            case PropertyType.BINARY:
                try
                {
                    return new StreamValueImpl( Boolean.toString(value) );
                }
                catch (IOException e)
                {
                    throw new ValueFormatException("Cannot create a binary object");
                }
        }
        
        throw new ValueFormatException("Illegal type "+PropertyType.nameFromValue(type));
    }

    public ValueImpl createValue(InputStream value, int type) throws ValueFormatException
    {
        try
        {
            switch (type)
            {
                case PropertyType.BOOLEAN:
                    return new BooleanValueImpl(FileUtil.readContents(value, "UTF-8"));

                case PropertyType.DOUBLE:
                    return new DoubleValueImpl(FileUtil.readContents(value, "UTF-8"));

                case PropertyType.LONG:
                    return new LongValueImpl(FileUtil.readContents(value, "UTF-8"));

                case PropertyType.DATE:
                    return new CalendarValueImpl(FileUtil.readContents(value, "UTF-8"));

                case PropertyType.NAME:
                    return new NameValueImpl(FileUtil.readContents(value, "UTF-8"));

                case PropertyType.PATH:
                    return new PathValueImpl(FileUtil.readContents(value, "UTF-8"));

                case PropertyType.REFERENCE:
                    return new ReferenceValueImpl(FileUtil.readContents(value, "UTF-8"));

                case PropertyType.STRING:
                    return new StringValueImpl(FileUtil.readContents(value, "UTF-8"));

                case PropertyType.BINARY:
                    return new StreamValueImpl(value);
            }
        }
        catch (IOException e)
        {
            throw new ValueFormatException("Unable to read data from binary stream");
        }
        
        throw new ValueFormatException("Illegal type "+PropertyType.nameFromValue(type));
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
                
            case PropertyType.DATE:
                return new CalendarValueImpl(value);
                
            case PropertyType.NAME:
                return new NameValueImpl(value);
                
            case PropertyType.PATH:
                return new PathValueImpl(value);
                
            case PropertyType.REFERENCE:
                return new ReferenceValueImpl(value);
                
            case PropertyType.STRING:
                return new StringValueImpl(value);
                
            case PropertyType.BINARY:
                try
                {
                    return new StreamValueImpl(value);
                }
                catch (IOException e)
                {
                    throw new ValueFormatException("Cannot create a binary object");
                }
        }
        
        throw new ValueFormatException("Illegal type "+PropertyType.nameFromValue(type));
    }

    /**
     *  Clones a value array.  This creates a new instance of every single value
     *  contained in this array, a so-called deep clone.
     *  
     *  @param values  The array to clone.
     *  @return A deep clone of the array.
     *  @throws ValueFormatException
     *  @throws IllegalStateException
     *  @throws RepositoryException
     */
    public Value[] cloneValues(Value[] values) 
        throws ValueFormatException, IllegalStateException, RepositoryException
    {
        Value[] v = new Value[values.length];
        
        for( int i = 0; i < values.length; i++ )
        {
            v[i] = createValue( values[i] );
        }
        
        return v;
    }

}
