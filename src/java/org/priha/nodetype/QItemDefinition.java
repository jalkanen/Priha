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

import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.OnParentVersionAction;

import org.priha.core.SessionImpl;
import org.priha.util.QName;

public class QItemDefinition
{
    protected QNodeType m_nodeType;
    protected QName     m_qname;
    protected boolean   m_isAutoCreated = false;
    protected boolean   m_isMandatory   = false;
    protected boolean   m_isProtected   = false;
    protected int       m_onParentVersion = OnParentVersionAction.COMPUTE;
    
    protected QItemDefinition( QNodeType nt, QName qname )
    {
        super();
        m_nodeType = nt;
        m_qname    = qname;
    }

    public QName getQName()
    {
        return m_qname;
    }

    public boolean isAutoCreated()
    {
        return m_isAutoCreated;
    }

    public boolean isMandatory()
    {
        return m_isMandatory;
    }

    public boolean isProtected()
    {
        return m_isProtected;
    }

    /**
     *  Returns true, if this property type defines a wildcard type
     *  instead of being specific.  All property definitions which
     *  have the name "*" are considered to be wildcards.
     *  
     *  @return True, if this is a wildcard definition.
     */
    public boolean isWildCard()
    {
        return getQName().toString().equals("*");
    }

    /**
     *  Really implements the ItemDefinition
     *
     */
    public class Impl implements ItemDefinition
    {
        SessionImpl m_mapper;
        
        public Impl( SessionImpl ns )
        {
            m_mapper = ns;
        }
    
        public NodeType getDeclaringNodeType()
        {
            return m_nodeType.new Impl(m_mapper);
        }

        public String getName()
        {
            return m_mapper.fromQName( getQName() );
        }

        public int getOnParentVersion()
        {
            return m_onParentVersion;
        }

        public boolean isAutoCreated()
        {
            return m_isAutoCreated;
        }

        public boolean isMandatory()
        {
            return m_isMandatory;
        }

        public boolean isProtected()
        {
            return m_isProtected;
        }
    }
}
