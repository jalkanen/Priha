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
package org.priha.core.values;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.*;
import javax.xml.namespace.QName;

import org.priha.core.NodeImpl;
import org.priha.core.SessionImpl;
import org.priha.core.binary.BinarySource;
import org.priha.util.FileUtil;

/**
 *  This is a session-specific holder of things.
 */
// FIXME: This class could stand some refactoring - lots of duplicated code here.
public class ValueFactoryImpl implements ValueFactory
{
    private SessionImpl m_session;
    
    public ValueFactoryImpl( SessionImpl session )
    {
        m_session = session;
    }
    
    public ValueImpl createValue(ValueImpl value) throws ValueFormatException, IllegalStateException, RepositoryException
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
                return new QNameValue( ((QNameValue.Impl)value).getQValue().getValue() ).new Impl(m_session);
                
            case PropertyType.PATH:
                return new QPathValue( ((QPathValue.Impl)value).getQValue().getPath() ).new Impl(m_session);
                
            case PropertyType.REFERENCE:
                return new ReferenceValueImpl( value.getString() );
                
            case PropertyType.STRING:
                return new StringValueImpl( value.getString() );

            case PropertyType.DATE:
                return new CalendarValueImpl( value.getDate() );
                
            case PropertyType.BINARY:
                return new StreamValueImpl( value );
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
                    return new QNameValue(m_session,FileUtil.readContents(value, "UTF-8")).new Impl(m_session);

                case PropertyType.PATH:
                    return new QPathValue(m_session,FileUtil.readContents(value, "UTF-8")).new Impl(m_session);

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
        catch( RepositoryException e )
        {
            throw new ValueFormatException("Unable to create value "+e.getMessage());
        }
        
        throw new ValueFormatException("Illegal type "+PropertyType.nameFromValue(type));
    }

    /**
     *  Returns true, if the given String value can be converted to the given
     *  type.
     *  
     *  @param value
     *  @param type
     *  @return
     */
    public static boolean canConvert( ValueImpl value, int type )
    {
        Value v = null;
        
        try
        {
            switch( type )
            {
                case PropertyType.BOOLEAN:
                    v = new BooleanValueImpl(value.getString());
                    break;
                
                case PropertyType.DOUBLE:
                    v = new DoubleValueImpl(value.getString());
                    break;
                
                case PropertyType.LONG:
                    v = new LongValueImpl(value.getString());
                    break;
                
                case PropertyType.DATE:
                    switch( value.getType() )
                    {
                        case PropertyType.LONG:
                            v = new CalendarValueImpl(value.getLong());
                            break;
                        case PropertyType.DOUBLE:
                            v = new CalendarValueImpl(value.getDouble());
                            break;
                        default:
                            v = new CalendarValueImpl(value.getString());
                    }
                    break;
                
                case PropertyType.NAME:
                case PropertyType.PATH:
                case PropertyType.REFERENCE:
                    break;
                
                case PropertyType.STRING:
                    v = new StringValueImpl(value.getString());
                    break;
                
                case PropertyType.BINARY:
                    v = new StreamValueImpl(value);
                    break;
            }
        }
        catch( Exception e ) {}

        return v != null;
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
                try
                {
                    return new QNameValue(m_session,value).new Impl(m_session);
                }
                catch( RepositoryException e1 )
                {
                    throw new ValueFormatException("Cannot create Name "+e1.getMessage());
                }
                
            case PropertyType.PATH:
                return new QPathValue(m_session,value).new Impl(m_session);
                
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
        int len = (values != null) ? values.length : 0;
        
        Value[] v = new Value[len];
        
        for( int i = 0; i < len; i++ )
        {
            v[i] = createValue( (ValueImpl)values[i] );
        }
        
        return v;
    }

    public ValueImpl createValue( QName qn, int type ) throws ValueFormatException
    {
        if( type != PropertyType.NAME ) throw new ValueFormatException("Can only create NAME types from QNames");
        
        return new QNameValue(qn).new Impl(m_session);
    }

}
