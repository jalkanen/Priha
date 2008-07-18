package org.jspwiki.priha.core.values;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.text.ParseException;
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

    public CalendarValueImpl(String value) throws ValueFormatException
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime( parse(value) );
        m_value = cal;
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
        checkValue();
        String val = Long.toString( m_value.getTimeInMillis() );
        return new ByteArrayInputStream(val.getBytes());
    }
}
