package org.jspwiki.priha.nodetype;

import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

public class PropertyDefinitionImpl extends ItemDefinitionImpl implements PropertyDefinition
{
    public PropertyDefinitionImpl(NodeType type)
    {
        super(type);
        // TODO Auto-generated constructor stub
    }

    public Value[] getDefaultValues()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public int getRequiredType()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public String[] getValueConstraints()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isMultiple()
    {
        // TODO Auto-generated method stub
        return false;
    }
    
}
