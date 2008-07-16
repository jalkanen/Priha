package org.jspwiki.priha.core.values;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

public class StringValueImpl extends ValueImpl implements Value
{
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
            return new ByteArrayInputStream( ((String)m_value).getBytes("UTF-8") );
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
