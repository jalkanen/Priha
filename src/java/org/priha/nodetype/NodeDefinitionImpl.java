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

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

public class NodeDefinitionImpl extends ItemDefinitionImpl implements NodeDefinition
{
    protected boolean           m_allowsSameNameSiblings = false;
    protected GenericNodeType   m_defaultPrimaryType     = null;
    protected GenericNodeType[] m_requiredPrimaryTypes = new GenericNodeType[0];
    
    public NodeDefinitionImpl(NodeType type, String name)
    {
        super(type,name);
    }

    public boolean allowsSameNameSiblings()
    {
        return m_allowsSameNameSiblings;
    }

    public NodeType getDefaultPrimaryType()
    {
        return m_defaultPrimaryType;
    }

    public NodeType[] getRequiredPrimaryTypes()
    {
        return m_requiredPrimaryTypes;
    }

    public String toString()
    {
        return "NodeType: "+getName();
    }
}
