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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

public class StringValueImpl extends ValueImpl implements Value, Serializable
{
    private static final long serialVersionUID = 6874691155431565288L;
    String m_value;
    
    public StringValueImpl( String value )
    {
        m_value = value;
    }
    
    public int getType()
    {
        return PropertyType.STRING;
    }

    @Override
    public boolean getBoolean()
    {
        checkValue();
        return Boolean.parseBoolean( m_value );
    }
    
    @Override
    public String getString() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        checkValue();
        return m_value;
    }
    
    @Override
    public Calendar getDate() throws ValueFormatException
    {
        checkValue();
        Calendar cal = Calendar.getInstance();
        
        cal.setTime( CalendarValueImpl.parse(m_value) );
        
        return cal;
    }

    @Override
    public double getDouble() throws ValueFormatException
    {
        checkValue();
        try
        {
            return Double.parseDouble( m_value );
        }
        catch( NumberFormatException e )
        {
            throw new ValueFormatException("Conversion from String to Double failed: "+e.getMessage());
        }
    }
    
    @Override
    public long getLong() throws ValueFormatException
    {
        checkValue();
        try
        {
            return Long.parseLong( m_value );
        }
        catch( NumberFormatException e )
        {
            throw new ValueFormatException("Conversion from String to Long failed: "+e.getMessage());
        }        
    }
    
    public InputStream getStream() throws IllegalStateException, RepositoryException
    {
        checkStream();
        
        try
        {
            return new ByteArrayInputStream( m_value != null ?
                m_value.getBytes("UTF-8") :
                    new byte[0] );
        }
        catch (UnsupportedEncodingException e)
        {
            // Shall never happen
            throw new RepositoryException(e);
        }
    }
    
    @Override
    public String toString()
    {
        checkValue();
        return m_value;
    }
}
