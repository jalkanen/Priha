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
package org.jspwiki.priha.core.values;

import java.io.Serializable;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;

import org.jspwiki.priha.core.NodeImpl;

public class ReferenceValueImpl extends NodeValueImpl implements Value, Serializable
{
    private static final long serialVersionUID = -3852563143401195069L;

    public ReferenceValueImpl(String value)
    {
        super( value,PropertyType.REFERENCE );
    }

    public ReferenceValueImpl(NodeImpl impl) 
        throws UnsupportedRepositoryOperationException, RepositoryException
    {
        super( impl.getUUID(), PropertyType.REFERENCE );
    }
    
    @Override
    public String toString()
    {
        return "REF="+m_value.toString();
    }
}
