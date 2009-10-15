/*
    Priha - A JSR-170 implementation library.

    Copyright (C) 2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;

import org.priha.core.NodeImpl;

/**
 *  Superclass of all classes which reference a Node
 *  
 */
public abstract class NodeValueImpl extends ValueImpl implements Value, Serializable
{
    protected int    m_type;
    protected String m_value;
    
    protected NodeValueImpl( NodeImpl value, int type ) 
        throws UnsupportedRepositoryOperationException, 
               RepositoryException
    {
        m_value = value.getUUID();
        m_type  = type;
    }

    protected NodeValueImpl(String value, int type)
    {
        m_value = value;
        m_type  = type;
    }

    @Override
    public String getString()
    {
        return m_value.toString();
    }
    
    @Override
    public InputStream getStream()
    {
        return new ByteArrayInputStream( m_value.toString().getBytes() );
    }
    
    public int getType()
    {
        return m_type;
    }

    @Override
    public String valueAsString()
    {
        return m_value;
    }
    
    @Override
    public String toString()
    {
        return m_value.toString();
    }
}
