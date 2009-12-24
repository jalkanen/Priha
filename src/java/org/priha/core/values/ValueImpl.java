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
import java.io.UnsupportedEncodingException;
import java.util.Calendar;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 *  Implements the Value interface and provides a number of default accessors
 *  for easy development.  All accessors by default throw a ValueFormatException,
 *  so you will want to implement those which you can convert to.
 *
 */
public abstract class ValueImpl implements Value, Cloneable, Comparable<ValueImpl>
{
    protected VALUE_STATE m_state   = VALUE_STATE.UNDEFINED;    

    private enum   VALUE_STATE { UNDEFINED, VALUE, STREAM }
    
    protected ValueImpl() {}

    /**
     *  This method makes sure that the Value value is a stream, not a value.
     *  
     *  @throws IllegalStateException If this is a value Value.
     */
    protected final void checkStream() throws IllegalStateException
    {
        if( m_state == VALUE_STATE.VALUE )  throw new IllegalStateException("This is a scalar value");
        if( m_state == VALUE_STATE.STREAM ) throw new IllegalStateException("Stream already consumed");
        
        m_state = VALUE_STATE.STREAM;
    }

    /**
     *  This method makes sure that the Value value is a value, not a stream.
     *  
     *  @throws IllegalStateException If this is a stream value.
     */
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

    /**
     *  By default, returns the inputstream of the String representation.
     */
    public InputStream getStream() throws IllegalStateException, RepositoryException
    {
        try
        {
            return new ByteArrayInputStream( getString().getBytes("UTF-8") );
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RepositoryException("Yeah right, you don't have UTF-8.  Your platform is b0rked.");
        }
    }

    public String getString() throws ValueFormatException, IllegalStateException, RepositoryException
    {
        throw new ValueFormatException("Cannot convert to String!");
    }
    
    public boolean equals( Object o )
    {
        if( o instanceof Value )
        {
            Value v = (Value) o;
            
            try
            {
                return v.getType() == getType() && v.getString().equals(getString());
            }
            catch( Exception e )
            {
            }
        }
        
        return false;
    }
    
    @Override
    public String toString()
    {
        return getClass().getName()+":"+m_state;
    }

    /**
     *  Allows getting the Value as a String without regard to the current Stream/Value format
     *  setting.  This means that this method can be called at any time.
     *   
     *  @return The value as a String. However, may return null in case the conversion
     *          cannot be made.  Default implementation returns null and subclasses are expected
     *          to override this.
     *  
     */
    public String valueAsString()
    {
        return null;
    }
    
    /**
     *  By default, does String comparison, which means it may fail.  Subclasses are expected
     *  to override.
     */
    public int compareTo( ValueImpl value )
    {
        try
        {
            String s1 = getString();
            String s2 = value.getString();
        
            return s1.compareTo( s2 );
        }
        catch(Exception e)
        {
            // FIXME: Probably not correct.
            return 0;
        }
    }
}
