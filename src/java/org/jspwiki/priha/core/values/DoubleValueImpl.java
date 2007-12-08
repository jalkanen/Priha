package org.jspwiki.priha.core.values;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

public class DoubleValueImpl extends ValueImpl implements Value
{
    private Double m_value;
    
    public DoubleValueImpl( double value )
    {
        m_value = value;
    }
    
    public DoubleValueImpl(String value)
    {
        m_value = Double.parseDouble(value);
    }

    public int getType()
    {
        return PropertyType.DOUBLE;
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
