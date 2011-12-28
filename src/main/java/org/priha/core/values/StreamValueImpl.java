/*
    Priha - A JSR-170 implementation library.

    Copyright (C) 2007-2009 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    Licensed under the Apache License, Version 2.0 (the "License"); 
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at 
    
      http://www.apache.org/licenses/LICENSE-2.0 
      
    Unless required by applicable law or agreed to in writing, software 
    distributed under the License is distributed on an "AS IS" BASIS, 
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
    See the License for the specific language governing permissions and 
    limitations under the License. 
 */
package org.priha.core.values;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.priha.core.binary.BinarySource;
import org.priha.core.binary.MemoryBinarySource;
import org.priha.util.FileUtil;

// FIXME: This class does not yet implement Serializable - it needs to serialize
//        itself as a proper bytearraystream or use the serialization from the BinarySource
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

    public long getLength() throws IOException
    {
        return m_value.getLength();
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
    
    @Override
    public long getSize()
    {
        try
        {
            return m_value.getLength();
        }
        catch (IOException e)
        {
            return -1;
        }
    }
}
