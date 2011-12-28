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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

public class BooleanValueImpl extends ValueImpl implements Value, Serializable
{
    private static final long serialVersionUID = 1L;
    Boolean m_value;
    
    public BooleanValueImpl(boolean value)
    {
        m_value = value;
    }

    public BooleanValueImpl(String value)
    {
        m_value = Boolean.valueOf(value);
    }

    public BooleanValueImpl(BooleanValueImpl value)
    {
        m_value = value.m_value;
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
    public InputStream getStream() throws RepositoryException
    {
        checkStream();
        
        try
        {
            return new ByteArrayInputStream( m_value.toString().getBytes("UTF-8") );
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RepositoryException("Yeah right, you don't have UTF-8.  Your platform is b0rked.");
        }

    }
    
    @Override
    public String getString() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        checkValue();
        return m_value.toString();
    }
    
    @Override
    public String toString()
    {
        return m_value.toString();
    }
    
    @Override
    public long getSize()
    {
        return 1;
    }
}
