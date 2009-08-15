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
package org.priha.nodetype;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;
import javax.xml.namespace.QName;

import org.priha.core.SessionImpl;

public class QPropertyDefinition extends QItemDefinition
{
    protected String[] m_defaults   = new String[0];
    protected boolean m_isMultiple = false;
    protected String[] m_valueConstraints = new String[0];
    
    protected int     m_requiredType = PropertyType.UNDEFINED;
    
    public QPropertyDefinition(QNodeType type, QName name)
    {
        super(type,name);
    }

    public boolean isMultiple()
    {
        return m_isMultiple;
    }

    /**
     *  Implements the PropertyDefinition with its Session-specific
     *  thingies.
     */
    
    public class Impl extends QItemDefinition.Impl implements PropertyDefinition
    {
        public Impl( SessionImpl ns )
        {
            super( ns );
        }

        public Value[] getDefaultValues()
        {
            Value[] v = new Value[m_defaults.length];
            
            for( int i = 0; i < m_defaults.length; i++ )
            {
                try
                {
                    Value vv;
                    vv = m_mapper.getValueFactory().createValue( m_defaults[i], m_requiredType );
                    
                    v[i] = vv;
                }
                catch( RepositoryException e )
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            
            return v;
        }

        public int getRequiredType()
        {
            return m_requiredType;
        }

        public String[] getValueConstraints()
        {
            return m_valueConstraints;
        }

        public boolean isMultiple()
        {
            return m_isMultiple;
        }
    
        /**
         * Returns a human-readable description string. Useful only for debugging
         * purposes.
         */
        public String toString()
        {
            return "PropertyDefinition: "+m_qname;
        }
        
        public QPropertyDefinition getQPropertyDefinition()
        {
            return QPropertyDefinition.this;
        }
    }
}
