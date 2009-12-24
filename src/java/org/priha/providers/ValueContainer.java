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
package org.priha.providers;

import javax.jcr.*;

import org.priha.core.SessionImpl;
import org.priha.core.values.QValue;
import org.priha.core.values.ValueImpl;

/**
 *  Stores a Value with its type.  This is what a PropertyImpl stores internally,
 *  and this is what gets passed down to a RepositoryProvider.  Most noticeably
 *  the difference is that the Value that a ValueContainer contains is NOT cloned
 *  when you do a getValue().  Therefore, if you getValue() and read its contents
 *  with getStream(), you cannot read it again until you clone the ValueContainer.
 *  Ditto, if you read it with getString() or one of the other scalar values, you
 *  cannot use getStream() anymore.
 *  <p>
 *  To work around this problem, use the sessionInstance() and deepClone() methods,
 *  which will return you a new ValueContainer - or use ValueFactory.cloneValues().
 */
public class ValueContainer
{
    public static final ValueContainer UNDEFINED_CONTAINER = new ValueContainer();
    
    private Object  m_value;
    private int     m_type = PropertyType.UNDEFINED;
    private boolean m_multiple;
    
    /** 
     *  Empty private constructor; use UNDEFINED_CONTAINER if you need something.
     */
    private ValueContainer()
    {}
    
    public ValueContainer(ValueImpl v)
    {
        m_value = v;
        m_multiple = false;
        m_type = v.getType();
    }
    
    public ValueContainer(ValueImpl[] v, int type)
    {
        m_value = v;
        m_multiple = true;
        m_type = type;        
    }
    
    public ValueContainer(Value[] values, int propertyType)
    {
        m_multiple = true;
        m_type = propertyType;
        m_value = new ValueImpl[values.length];

        System.arraycopy(values, 0, m_value, 0, values.length);
    }

    public int getType()
    {
        return m_type;
    }
    
    public ValueImpl getValue() throws ValueFormatException
    {
        if( m_multiple == true ) throw new ValueFormatException("Attempt to get a multivalue");
        return (ValueImpl)m_value;
    }
    
    public ValueImpl[] getValues() throws ValueFormatException
    {
        if( m_multiple == false ) throw new ValueFormatException("Attempt to get a single value");
        return (ValueImpl[])m_value;
    }
    
    public boolean isMultiple()
    {
        return m_multiple;
    }
    
    public String toString()
    {
        try
        {
            return isMultiple() ? (PropertyType.nameFromValue( m_type )+"["+getValues().length+"]") : m_value.toString();
        }
        catch( ValueFormatException e ) {}
        return "Error?";
    }
    
    /**
     *  Returns a new ValueContainer which has mappings to the given Session.
     *  
     *  @param session
     *  @return
     *  @throws ValueFormatException
     */
    public ValueContainer sessionInstance( SessionImpl session ) throws ValueFormatException
    {
        if( m_multiple )
        {
            ValueImpl[] newValues = new ValueImpl[getValues().length];
            
            for( int i = 0; i < getValues().length; i++ )
            {
                ValueImpl vi = getValues()[i];
                
                if( vi instanceof QValue.QValueInner )
                {
                    vi = ((QValue.QValueInner)vi).getQValue().getValue( session );
                }
                newValues[i] = vi;
            }
            
            return new ValueContainer( newValues, m_type );
        }
        
        if( getValue() instanceof QValue.QValueInner )
        {
            return new ValueContainer( ((QValue.QValueInner)getValue()).getQValue().getValue( session ) );
        }
        
        return this;
    }

    /**
     *  Cloning of a ValueContainer is a deep operation, i.e. you will get all new Value instances.
     *  This is because the Values might be consumed already, esp. if they are streams.
     *  
     *  @throws RepositoryException 
     *  @throws UnsupportedRepositoryOperationException 
     *  @throws IllegalStateException 
     *  @throws ValueFormatException 
     */
    public ValueContainer deepClone( SessionImpl session ) throws ValueFormatException, IllegalStateException, UnsupportedRepositoryOperationException, RepositoryException
    {
        if( m_multiple )
        {
            Value[] newValues = session.getValueFactory().cloneValues( getValues() );
            
            return new ValueContainer(newValues, getType());
        }
        else
        {
            ValueImpl newValue = session.getValueFactory().cloneValue( getValue() );
            return new ValueContainer( newValue );
        }
    }
    
    public boolean isEmpty()
    {
        return m_value == null;
    }
}
