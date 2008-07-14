package org.jspwiki.priha.core.values;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

public class BooleanValueImpl extends ValueImpl implements Value
{
    Boolean m_value;
    
    public BooleanValueImpl(boolean value)
    {
        m_value = value;
    }

    public BooleanValueImpl(String value)
    {
        m_value = "true".equals(value);
    }

    public int getType()
    {
        return PropertyType.BOOLEAN;
    }
    
    @Override
    public boolean getBoolean()
    {
        checkValue();

        return m_value;
    }
    
    @Override
    public String getString() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        checkValue();
        return m_value.toString();
    }
}
