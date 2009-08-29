package org.priha.util;

public class PathRef
{
    private int m_ref;
    
    protected PathRef(int ref)
    {
        m_ref = ref;
    }
    
    public boolean equals(Object o)
    {
        if( o == this ) return true;
        
        if( o instanceof PathRef ) return ((PathRef)o).m_ref == m_ref;
        
        return false;
    }
    
    public int hashCode()
    {
        return m_ref;
    }
    
    public String toString()
    {
        return "[REF "+Integer.toHexString( m_ref )+"]";
    }
}
