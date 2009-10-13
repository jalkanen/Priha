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

import java.io.Serializable;
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

public class LongValueImpl extends ValueImpl implements Value, Serializable
{
    private static final long serialVersionUID = -6722775187710834343L;
    private Long m_value;
    
    public LongValueImpl( long value )
    {
        m_value = value;
    }
    
    public LongValueImpl(String value) throws ValueFormatException
    {
        try
        {
            m_value = Long.parseLong(value);
        }
        catch( Exception e )
        {
            throw new ValueFormatException("Cannot be parsed as long: "+value);
        }
    }

    public LongValueImpl(LongValueImpl value)
    {
        m_value = value.m_value;
    }

    public int getType()
    {
        return PropertyType.LONG;
    }

    @Override
    public long getLong() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        checkValue();
        return m_value;
    }
    
    @Override
    public Calendar getDate() 
    {
        checkValue();
        
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis( m_value );
        return cal;
    }
    
    @Override
    public double getDouble()
    {
        return m_value;
    }
    
    @Override
    public int compareTo( ValueImpl value )
    {
        try
        {
            if( value.getType() == PropertyType.DOUBLE || value.getType() == PropertyType.LONG )
                return m_value.compareTo(value.getLong());
        
            return super.compareTo( value );
        }
        catch( RepositoryException e ) {}
        
        return 0;
    }
    
    @Override
    public String getString() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        checkValue();
        return Long.toString( m_value );
    }
    
    @Override
    public String toString()
    {
        return m_value.toString();
    }
}
