package org.jspwiki.priha.core.values;

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

}
