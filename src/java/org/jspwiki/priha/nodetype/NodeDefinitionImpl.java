package org.jspwiki.priha.nodetype;

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

public class NodeDefinitionImpl extends ItemDefinitionImpl implements NodeDefinition
{
    public NodeDefinitionImpl(NodeType type)
    {
        super(type);
    }

    public boolean allowsSameNameSiblings()
    {
        return false;
    }

    public NodeType getDefaultPrimaryType()
    {
        return null;
    }

    public NodeType[] getRequiredPrimaryTypes()
    {
        return null;
    }

}
