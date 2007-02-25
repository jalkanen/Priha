package org.jspwiki.priha.core;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;

import javax.jcr.*;

/**
 *  Implements the Value interface and provides a number of constructors
 *  for easy development.
 *  @author jalkanen
 *
 */
public class ValueImpl implements Value, Cloneable
{
    private Object      m_value;
    private int         m_type    = PropertyType.UNDEFINED;
    private VALUE_STATE m_state   = VALUE_STATE.UNDEFINED;
    

    private int    BLOB = 1000;
    private enum   VALUE_STATE { UNDEFINED, VALUE, STREAM };
    
    private ValueImpl() {}
    
    public ValueImpl clone()
    {
        ValueImpl v = new ValueImpl();
        
        v.m_type  = m_type;
        v.m_value = m_value;
        
        return v;
    }
    
    public ValueImpl(boolean value)
    {
        m_type = PropertyType.BOOLEAN;
        m_value = new Boolean(value);
    }


    public ValueImpl(Calendar value)
    {
        m_type = PropertyType.DATE;
        m_value = value;
    }


    public ValueImpl(double value)
    {
        m_type = PropertyType.DOUBLE;
        m_value = new Double(value);
    }

    public ValueImpl(long value)
    {
        m_type = PropertyType.LONG;
        m_value = new Long(value);
    }

    public ValueImpl(String string)
    {
        m_type = PropertyType.STRING;
        m_value = string;
    }

    /**
     * Can be used to create a NAME, REFERENCE, or STRING
     * @param string
     * @param type
     */
    public ValueImpl( String string, int type )
    {
        m_type = type;
        m_value = string;
    }
    
    public ValueImpl(InputStream in)
    {
        m_type = PropertyType.BINARY;
        m_value = in;
    }

    protected ValueImpl( byte[] blob )
    {
        m_type = BLOB;
        m_value = blob;
    }

    public ValueImpl(Value value)
        throws RepositoryException
    {
        m_type = value.getType();
        
        switch( m_type )
        {
            case PropertyType.BINARY:
                m_value = value.getStream();
                break;
            case PropertyType.BOOLEAN:
                m_value = value.getBoolean();
                break;
            case PropertyType.DATE:
                m_value = value.getDate();
                break;
            case PropertyType.DOUBLE:
                m_value = value.getDouble();
                break;
            case PropertyType.LONG:
                m_value = value.getLong();
                break;
            default:
                m_value = value.getString();
                break;
        }
    }

    public ValueImpl(Node value)
    {
        //
        //  Reference objects are stored as paths; not direct references.
        //
        
        try
        {
            m_value = value.getPath();
            m_type  = PropertyType.REFERENCE;
        }
        catch (RepositoryException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private final void checkStream() throws IllegalStateException
    {
        if( m_state == VALUE_STATE.VALUE ) throw new IllegalStateException();
    }

    private final void checkValue() throws IllegalStateException
    {
        if( m_state == VALUE_STATE.STREAM ) throw new IllegalStateException();
    }

    public boolean getBoolean() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        boolean retVal;

        checkValue();
        if( m_type == PropertyType.BOOLEAN )
            retVal = ((Boolean)m_value).booleanValue();
        else if( m_type == PropertyType.STRING )
            retVal = Boolean.parseBoolean((String)m_value);
        else
            throw new ValueFormatException("This is not a boolean type: "+m_value.getClass().getName());
        
        m_state = VALUE_STATE.VALUE;
        
        return retVal;
    }

    public Calendar getDate() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        Calendar retVal;
        checkValue();
        
        if( m_type == PropertyType.DATE )
            retVal = ((Calendar)m_value);
        else
            throw new ValueFormatException("This is not a date type: "+m_value.getClass().getName());
        
        m_state = VALUE_STATE.VALUE;
        return retVal;        
    }

    public double getDouble() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        double retVal;
        
        checkValue();
        
        if( m_type == PropertyType.DOUBLE )
            retVal = ((Double)m_value).doubleValue();
        else
            throw new ValueFormatException("This is not a double type: "+m_value.getClass().getName());
        
        m_state = VALUE_STATE.VALUE;
        return retVal;
    }

    public long getLong() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        long retVal;
        
        checkValue();
        
        if( m_type == PropertyType.LONG )
            retVal = ((Long)m_value).longValue();
        else
            throw new ValueFormatException("This is not a long type: "+m_value.getClass().getName());

        m_state = VALUE_STATE.VALUE;
        return retVal;
    }

    public InputStream getStream() throws IllegalStateException, RepositoryException
    {
        InputStream retVal;
        
        checkStream();
        
        if( m_type == PropertyType.BINARY )
            retVal = (InputStream)m_value;
        else if( m_type == BLOB )
            retVal = new ByteArrayInputStream( (byte[])m_value );
        else if( m_type == PropertyType.STRING )
        {
            try
            {
                retVal = new ByteArrayInputStream( ((String)m_value).getBytes("UTF-8") );
            }
            catch (UnsupportedEncodingException e)
            {
                // Shall never happen
                throw new RepositoryException(e);
            }
        }
        else
            throw new ValueFormatException("This is not a stream type: "+m_value.getClass().getName());
        
        m_state = VALUE_STATE.STREAM;
        return retVal;
    }

    public String getString() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        checkValue();
        
        m_state = VALUE_STATE.VALUE;
        return m_value.toString();
    }

    public int getType()
    {
        return m_type;
    }
    
    public String toString()
    {
        return "Value["+m_value.toString()+"]";
    }

}
