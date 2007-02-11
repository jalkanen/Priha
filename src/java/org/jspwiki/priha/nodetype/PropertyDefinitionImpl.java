package org.jspwiki.priha.nodetype;

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

public class PropertyDefinitionImpl extends ItemDefinitionImpl implements PropertyDefinition
{
    protected Value[] m_defaults   = new Value[0];
    protected boolean m_isMultiple = false;
    
    protected int     m_requiredType = PropertyType.UNDEFINED;
    
    public PropertyDefinitionImpl(NodeType type, String name)
    {
        super(type,name);
        // TODO Auto-generated constructor stub
    }

    public Value[] getDefaultValues()
    {
        return m_defaults;
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
        return m_isMultiple;
    }
    
    /**
     *  Returns a human-readable description string.  Useful only for debugging
     *  purposes.
     */
    public String toString()
    {
        return "PropertyDefinition: "+m_name;
    }
}
