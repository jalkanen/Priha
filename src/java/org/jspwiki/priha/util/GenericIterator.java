package org.jspwiki.priha.util;

import java.util.Iterator;
import java.util.List;

public abstract class GenericIterator implements Iterator
{
    protected Iterator m_iterator;
    protected int m_position;
    protected int m_size;

    public GenericIterator( List list )
    {
        m_iterator = list.iterator();
        m_position = 0;
        m_size     = list.size();

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
