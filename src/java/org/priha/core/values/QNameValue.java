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
package org.priha.core.values;

import java.io.Serializable;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.xml.namespace.QName;

import org.priha.core.namespace.NamespaceMapper;

public class QNameValue extends QValue implements Serializable
{
    private static final long serialVersionUID = -5040292769406453341L;

    private QName m_value; 
    
    public QNameValue(QName value)
    {
        m_value = value;
    }
    
    public QNameValue(NamespaceMapper na, String value) throws RepositoryException
    {
        m_value = na.toQName( value );
    }

    @Override
    public ValueImpl getValue(NamespaceMapper nsm)
    {
        return new Impl(nsm);
    }
    
    public class Impl extends ValueImpl implements Value, Serializable, QValue.QValueInner
    {
        private static final long serialVersionUID = 1L;
        
        public NamespaceMapper m_mapper;
        
        public Impl(NamespaceMapper nsm)
        {
            m_mapper = nsm;
        }
        
        public String getString() throws ValueFormatException, IllegalStateException, RepositoryException
        {
            return m_mapper.fromQName( m_value );
        }

        public int getType()
        {
            return PropertyType.NAME;
        }
        
        public QNameValue getQValue()
        {
            return QNameValue.this;
        }
    }
}

