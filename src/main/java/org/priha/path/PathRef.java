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
package org.priha.path;

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
