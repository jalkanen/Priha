package org.jspwiki.priha.core.values;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

public class CalendarValueImpl extends ValueImpl implements Value
{
    private Calendar m_value;
    
    public CalendarValueImpl( Calendar c )
    {
        m_value = c;
    }

    public int getType()
    {
        return PropertyType.DATE;
    }

    @Override
    public Calendar getDate() throws ValueFormatException, IllegalStateException, RepositoryException
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
    
    @Override
    public InputStream getStream()
    {
        String val = Long.toString( m_value.getTimeInMillis() );
        return new ByteArrayInputStream(val.getBytes());
    }
}
