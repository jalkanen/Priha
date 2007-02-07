package org.jspwiki.priha.nodetype;

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

public class NodeDefinitionImpl extends ItemDefinitionImpl implements NodeDefinition
{
    protected boolean  m_allowsSameNameSiblings = false;
    protected NodeType m_defaultPrimaryType     = null;
    protected NodeType[] m_requiredPrimaryTypes = new NodeType[0];
    
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

}
