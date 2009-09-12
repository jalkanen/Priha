package org.priha.providers;

import javax.jcr.PropertyType;

import org.priha.core.SessionImpl;
import org.priha.core.values.QValue;
import org.priha.core.values.ValueImpl;

/**
 *  Stores a Value.
 */
public class ValueContainer
{
    Object m_value;
    int    m_type;
    boolean m_multiple;
    
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
    
    public int getType()
    {
        return m_type;
    }
    
    public ValueImpl getValue() throws IllegalStateException
    {
        if( m_multiple == true ) throw new IllegalStateException("Attempt to get a multivalue");
        return (ValueImpl)m_value;
    }
    
    public ValueImpl[] getValues() throws IllegalStateException
    {
        if( m_multiple == false ) throw new IllegalStateException("Attempt to get a single value");
        return (ValueImpl[])m_value;
    }
    
    public boolean isMultiple()
    {
        return m_multiple;
    }
    
    public String toString()
    {
        return isMultiple() ? (PropertyType.nameFromValue( m_type )+"["+getValues().length+"]") : m_value.toString();
    }

    public ValueContainer sessionInstance( SessionImpl session )
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
}
