package org.jspwiki.priha.core.values;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

public class LongValueImpl extends ValueImpl implements Value
{
    private Long m_value;
    
    public LongValueImpl( long value )
    {
        m_value = value;
    }
    
    public LongValueImpl(String value)
    {
        m_value = Long.parseLong(value);
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
    public String getString() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        checkValue();
        return Long.toString( m_value );
    }
}
