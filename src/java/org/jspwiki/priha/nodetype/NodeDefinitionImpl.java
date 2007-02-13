package org.jspwiki.priha.nodetype;

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
