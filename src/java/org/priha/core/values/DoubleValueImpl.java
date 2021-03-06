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
package org.priha.core.values;

import java.io.Serializable;
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

public class DoubleValueImpl extends ValueImpl implements Value, Serializable
{
    private static final long serialVersionUID = -3927800064326452318L;
    private Double m_value;
    
    public DoubleValueImpl( double value )
    {
        m_value = value;
    }
    
    public DoubleValueImpl(String value) throws ValueFormatException
    {
        try
        {
            m_value = Double.parseDouble(value);
        }
        catch( Exception e )
        {
            throw new ValueFormatException( e.getMessage() );
        }
    }

    public DoubleValueImpl(DoubleValueImpl value)
    {
        m_value = value.m_value;
    }

    public int getType()
    {
        return PropertyType.DOUBLE;
    }

    @Override
    public Calendar getDate()
    {
        checkValue();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis( m_value.longValue() );
        
        return cal;
    }
    
    @Override
    public long getLong()
    {
        return m_value.longValue();
    }
    
    @Override
    public double getDouble() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        checkValue();
        return m_value;
    }

    @Override
    public String getString() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        checkValue();
        return Double.toString( m_value );
    }

    @Override
    public int compareTo( ValueImpl value )
    {
        try
        {
            if( value.getType() == PropertyType.DOUBLE || value.getType() == PropertyType.LONG )
                return m_value.compareTo(value.getDouble());
        
            return super.compareTo( value );
        }
        catch( RepositoryException e ) {}
        
        return 0;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        // TODO Auto-generated method stub
        return super.clone();
    }

    @Override
    public String toString()
    {
        return m_value.toString();
    }
    
    @Override
    public long getSize()
    {
        return 8;
    }
}
