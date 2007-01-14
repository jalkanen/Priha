package org.jspwiki.priha.nodetype;

import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

/**
 *  Implements the nt:base type.
 *  
 *  @author jalkanen
 *
 */
public class BaseNodeType 
     implements NodeType
{
    protected NodeType[]           m_superTypes            = new NodeType[0];
    protected PropertyDefinition[] m_propertyDefinitions;
    protected NodeDefinition[]     m_childNodeDefinitions;

    public BaseNodeType()
    {
        m_propertyDefinitions = new PropertyDefinition[2];
        // FIXME: Add definitions
    }
    public boolean canAddChildNode(String childNodeName)
    {
        return true;
    }

    public boolean canAddChildNode(String childNodeName, String nodeTypeName)
    {
        return true;
    }

    public boolean canRemoveItem(String itemName)
    {
        return true;
    }

    public boolean canSetProperty(String propertyName, Value value)
    {
        return true;
    }

    public boolean canSetProperty(String propertyName, Value[] values)
    {
        return true;
    }

    public NodeDefinition[] getChildNodeDefinitions()
    {
        return     m_childNodeDefinitions;
    }

    public NodeDefinition[] getDeclaredChildNodeDefinitions()
    {
        return m_childNodeDefinitions;        
    }

    public PropertyDefinition[] getDeclaredPropertyDefinitions()
    {
        return m_propertyDefinitions;
    }

    public NodeType[] getDeclaredSupertypes()
    {
        return m_superTypes;
    }

    public String getName()
    {
        return "nt:base";
    }

    public String getPrimaryItemName()
    {
        return null;
    }

    public PropertyDefinition[] getPropertyDefinitions()
    {
        return m_propertyDefinitions;
    }

    public NodeType[] getSupertypes()
    {
        return m_superTypes ;
    }

    public boolean hasOrderableChildNodes()
    {
        return false;
    }

    public boolean isMixin()
    {
        return false;
    }

    public boolean isNodeType(String nodeTypeName)
    {
        return true;
    }

}
