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
package org.priha.util;

import java.util.Iterator;
import java.util.List;

/**
 *  Provides a base class for the different Iterators that JCR defines.
 *  
 *  @author jalkanen
 */
public abstract class GenericIterator implements Iterator
{
    protected Iterator<?> m_iterator;
    protected int m_position;
    protected int m_size;
    protected List<?> m_list;

    public GenericIterator( List<?> list )
    {
        m_iterator = list.iterator();
        m_position = 0;
        m_size     = list.size();
        m_list     = list;
    }
    
    public long getPosition()
    {
        return m_position;
    }

    public long getSize()
    {
        return m_size;
    }

    public void skip(long skipNum)
    {
        for( int i = 0; i < skipNum; i++ )
        {
            next();
        }
    }

    public boolean hasNext()
    {
        return m_iterator.hasNext();
    }

    public Object next()
    {
        m_position++;

        return m_iterator.next();
    }

    public void remove()
    {
        m_iterator.remove();
        m_size--;
        if( m_size == m_position ) m_position = m_size-1;
        if( m_position < 0 ) m_position = 0;
    }

}
