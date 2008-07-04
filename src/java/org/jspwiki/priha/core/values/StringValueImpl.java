package org.jspwiki.priha.core.values;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

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
        return "true".equalsIgnoreCase(m_value);
    }
    
    @Override
    public String getString() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        checkValue();
        return m_value;
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
        return m_value;
    }
}
