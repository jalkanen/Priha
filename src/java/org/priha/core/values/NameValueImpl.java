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
import java.net.URI;
import java.net.URISyntaxException;

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

public class NameValueImpl extends NodeValueImpl implements Value, Serializable
{
    private static final long serialVersionUID = -5040292769406453341L;

    public NameValueImpl(String value) throws ValueFormatException
    {
        super( value, PropertyType.NAME );

        try
        {
            @SuppressWarnings("unused")
            URI uri = new URI( value );
        }
        catch (URISyntaxException e)
        {
            throw new ValueFormatException("Not in URI format");
        } 
    }
    
    @Override
    public String getString()
    {
        return m_value;
    }
    
}
