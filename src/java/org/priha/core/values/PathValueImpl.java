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
import javax.jcr.nodetype.NodeType;

import org.priha.core.namespace.NamespaceMapper;
import org.priha.util.InvalidPathException;
import org.priha.util.Path;
import org.priha.util.PathFactory;
import org.priha.util.PathUtil;

public class PathValueImpl extends ValueImpl implements Value, Serializable
{
    private static final long serialVersionUID = -980121404025627369L;

    private Path m_value;
    
    public PathValueImpl(NamespaceMapper na, String value) throws ValueFormatException
    {
        try
        {
            PathUtil.validatePath(value);
            m_value = PathFactory.getPath( na, value );
        }
        catch( RepositoryException e )
        {
            throw new ValueFormatException("Invalid path "+e.getMessage());
        }
        
    }

    public int getType()
    {
        return PropertyType.PATH;
    }

}
