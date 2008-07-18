package org.jspwiki.priha.core.values;

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
        return (double)m_value;
    }
    
    @Override
    public String getString() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        checkValue();
        return Long.toString( m_value );
    }
}
