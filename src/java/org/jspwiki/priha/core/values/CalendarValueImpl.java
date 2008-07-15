package org.jspwiki.priha.core.values;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

public class CalendarValueImpl extends ValueImpl implements Value
{
    private Calendar m_value;
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
    
    public static Date parse( String date ) throws ParseException
    {
        return c_isoFormat.get().parse(date);
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
        checkValue();
        String val = Long.toString( m_value.getTimeInMillis() );
        return new ByteArrayInputStream(val.getBytes());
    }
}
