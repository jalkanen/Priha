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
package org.priha.nodetype;

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.priha.core.SessionImpl;
import org.priha.util.QName;

public class QNodeDefinition extends QItemDefinition
{
    protected boolean           m_allowsSameNameSiblings = false;
    protected QNodeType         m_defaultPrimaryType     = null;
    protected QNodeType[]       m_requiredPrimaryTypes   = new QNodeType[0];
    
    public QNodeDefinition(QNodeType type, QName name)
    {
        super(type,name);
    }

    public QNodeType getDefaultPrimaryType()
    {
        return m_defaultPrimaryType;
    }

    public QNodeType[] getRequiredPrimaryTypes()
    {
        return m_requiredPrimaryTypes;
    }


    public String toString()
    {
        return "QNodeType: "+getQName();
    }
    
    public class Impl extends QItemDefinition.Impl implements NodeDefinition
    {
        public Impl( SessionImpl ns )
        {
            super( ns );
        }

        public boolean allowsSameNameSiblings()
        {
            return m_allowsSameNameSiblings;
        }

        public NodeType getDefaultPrimaryType()
        {
            if( m_defaultPrimaryType == null ) return null;
            return m_defaultPrimaryType.new Impl(m_mapper);
        }

        public NodeType[] getRequiredPrimaryTypes()
        {
            NodeType[] nts = new NodeType[m_requiredPrimaryTypes.length];
            
            for( int i = 0; i < m_requiredPrimaryTypes.length; i++ )
            {
                nts[i] = m_requiredPrimaryTypes[i].new Impl(m_mapper);
            }
            
            return nts;
        }

        public String toString()
        {
            return "NodeType: "+getName();
        }
    }
}
