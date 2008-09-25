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

import javax.jcr.*;
import javax.xml.namespace.QName;

import org.priha.core.namespace.NamespaceAware;

public class NameValueImpl extends ValueImpl implements Value, Serializable
{
    private static final long serialVersionUID = -5040292769406453341L;

    private QName m_value; 
    
    public NameValueImpl(NamespaceAware na, String value) throws RepositoryException
    {
        m_value = na.toQName( value );
    }
    
    @Override
    public String getString()
    {
        return m_value.toString(); // FIXME: Not correct
    }

    public int getType()
    {
        return PropertyType.NAME;
    }
    
}
