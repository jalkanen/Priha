package org.jspwiki.priha.core.values;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 *  Implements the Value interface and provides a number of constructors
 *  for easy development.
 *  @author jalkanen
 *
 */
public abstract class ValueImpl implements Value, Cloneable
{
    protected VALUE_STATE m_state   = VALUE_STATE.UNDEFINED;    

    private int    BLOB = 1000;
    private enum   VALUE_STATE { UNDEFINED, VALUE, STREAM };
    
    protected ValueImpl() {}

    protected final void checkStream() throws IllegalStateException
    {
        if( m_state == VALUE_STATE.VALUE ) throw new IllegalStateException();
        m_state = VALUE_STATE.STREAM;
    }

    protected final void checkValue() throws IllegalStateException
    {
        if( m_state == VALUE_STATE.STREAM ) throw new IllegalStateException();
        m_state = VALUE_STATE.VALUE;
    }

    public boolean getBoolean() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        throw new ValueFormatException("This is not a boolean type: "+getClass().getName());
    }

    public Calendar getDate() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        throw new ValueFormatException("This is not a date type: "+getClass().getName());
    }

    public double getDouble() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        throw new ValueFormatException("This is not a double type: "+getClass().getName());
    }

    public long getLong() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        throw new ValueFormatException("This is not a long type: "+getClass().getName());
    }

    public InputStream getStream() throws IllegalStateException, RepositoryException
    {
        throw new ValueFormatException("This is not a stream type: "+getClass().getName());
    }

    public String getString() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        throw new ValueFormatException("Cannot convert to String!");
    }
}
