package org.jspwiki.priha.core.values;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.jspwiki.priha.core.binary.BinarySource;
import org.jspwiki.priha.core.binary.MemoryBinarySource;
import org.jspwiki.priha.util.FileUtil;

public class StreamValueImpl extends ValueImpl implements Value
{
    private BinarySource m_value;
    
    public StreamValueImpl(ValueImpl val) throws IllegalStateException, RepositoryException
    {
        if( val instanceof StreamValueImpl )
        {
            m_value = ((StreamValueImpl)val).m_value.clone();
        }
        else
        {
            try
            {
                m_value = new MemoryBinarySource( val.getStream() );
            }
            catch (IOException e)
            {
                throw new ValueFormatException("Cannot construct a binary source: "+e.getMessage());
            }
        }
    }

    public StreamValueImpl(InputStream in) throws ValueFormatException
    {
        try
        {
            m_value = new MemoryBinarySource( in );
        }
        catch (IOException e)
        {
            throw new ValueFormatException("Cannot construct a binary source: "+e.getMessage());
        }        
    }
    
    public StreamValueImpl(BinarySource source)
    {
        m_value = source;
    }
    
    public StreamValueImpl(String value) throws IOException
    {
        byte[] v = value.getBytes("UTF-8");
        
        m_value = new MemoryBinarySource(v);
    }

    public int getType()
    {
        return PropertyType.BINARY;
    }

    @Override
    public InputStream getStream() throws IllegalStateException, RepositoryException
    {
        checkStream();
        try
        {
            return m_value.getStream();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Stream read error:"+e.getMessage());
        }
    }
    
    @Override
    public String getString() throws ValueFormatException
    {
        checkStream();
        
        String res;
        try
        {
            res = FileUtil.readContents( m_value.getStream(), "UTF-8" );
        }
        catch (IOException e)
        {
            throw new ValueFormatException("Stream cannot be interpreted in UTF-8");
        }
        
        return res;
    }
    
    @Override
    public Calendar getDate() throws ValueFormatException
    {
        checkStream();
        
        Calendar cal = Calendar.getInstance();

        cal.setTime( CalendarValueImpl.parse( getString() ) );

        return cal;
    }
    
    @Override
    public double getDouble() throws ValueFormatException
    {
        checkStream();
        
        try
        {
            return Double.parseDouble( getString() );
        }
        catch( NumberFormatException e )
        {
            throw new ValueFormatException("Conversion from Stream to Double failed: "+e.getMessage());
        }
    }
    
    @Override
    public long getLong() throws ValueFormatException
    {
        checkStream();
        try
        {
            return Long.parseLong( getString() );
        }
        catch( NumberFormatException e )
        {
            throw new ValueFormatException("Conversion from Stream to Long failed: "+e.getMessage());
        }        
    }
    
    @Override
    public boolean getBoolean() throws ValueFormatException
    {
        checkStream();
        return Boolean.parseBoolean( getString() );
    }
}
