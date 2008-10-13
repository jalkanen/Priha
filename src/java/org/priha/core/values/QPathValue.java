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

import org.priha.core.namespace.NamespaceMapper;
import org.priha.util.Path;
import org.priha.util.PathFactory;
import org.priha.util.PathUtil;

public class QPathValue extends QValue
{
    private static final long serialVersionUID = -980121404025627369L;

    private Path m_value;
    
    public QPathValue(NamespaceMapper na, String value) throws ValueFormatException
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

    public QPathValue( Path path )
    {
        m_value = path;
    }

    @Override
    public ValueImpl getValue(NamespaceMapper nsm)
    {
        return new Impl(nsm);
    }
    
    public Path getPath()
    {
        return m_value;
    }
    
    public class Impl extends ValueImpl implements Value, Serializable, QValue.QValueInner
    {
        private static final long serialVersionUID = 1L;
        private NamespaceMapper m_mapper;
        
        public Impl(NamespaceMapper nsm)
        {
            m_mapper = nsm;
        }
        
        public String getString() throws NamespaceException, RepositoryException
        {
            return m_value.toString(m_mapper);
        }
        
        public int getType()
        {
            return PropertyType.PATH;
        }
        
        public QPathValue getQValue()
        {
            return QPathValue.this;
        }
    }

    @Override
    public String getString()
    {
        return m_value.toString();
    }
}
