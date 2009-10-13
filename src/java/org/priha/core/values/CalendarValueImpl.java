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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

public class CalendarValueImpl extends ValueImpl implements Value, Serializable
{
    private static final long serialVersionUID = -8918655334999478605L;
    private Calendar m_value = Calendar.getInstance();
    private static ThreadLocal<SimpleDateFormat> c_isoFormat = new ThreadLocal<SimpleDateFormat>() {
        protected synchronized SimpleDateFormat initialValue()
        {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");            
        }
    };
    
    public CalendarValueImpl( Calendar c )
    {
        m_value = c;
    }

    public CalendarValueImpl( Double value ) throws ValueFormatException
    {
        this( value.longValue() );
    }
    
    public CalendarValueImpl( Long value ) throws ValueFormatException
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis( value );
        m_value = cal;                
    }
    
    public CalendarValueImpl(String value) throws ValueFormatException
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime( parse(value) );
        m_value = cal;
    }

    public CalendarValueImpl(CalendarValueImpl value)
    {
        m_value = value.m_value;
    }

    public int getType()
    {
        return PropertyType.DATE;
    }

    /**
     *  Formats a date in the ISO 8601 format required by JCR.
     *  
     *  @param date
     *  @return
     */
    public static String format( Date date )
    {
        return c_isoFormat.get().format(date);
    }
    
    public static Date parse( String date ) throws ValueFormatException
    {
        try
        {
            Date d = c_isoFormat.get().parse(date);
            if( d == null ) throw new ValueFormatException("Cannot be parsed as date: "+date);
            
            return d;
        }
        catch( Exception e )
        {
            throw new ValueFormatException("Cannot be parsed as date: "+date);
        }
    }
    
    @Override
    public Calendar getDate() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        checkValue();
        return m_value;
    }
    
    @Override
    public double getDouble()
    {
        checkValue();
        return m_value.getTimeInMillis();
    }

    @Override
    public long getLong()
    {
        checkValue();
        return m_value.getTimeInMillis();
    }
    
    @Override
    public String getString() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        checkValue();
        return c_isoFormat.get().format(m_value.getTime());
    }
    
    @Override
    public InputStream getStream()
    {
        checkStream();
        String val = c_isoFormat.get().format( m_value.getTime() );
        return new ByteArrayInputStream(val.getBytes());
    }
    
    public String toString()
    {
        if( m_value != null )
            return c_isoFormat.get().format( m_value.getTime() );
        
        return "Null value";
        
    }
}
