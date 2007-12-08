package org.jspwiki.priha.nodetype;

import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

/**
 *  Stores the Node Types.
 *
 *  @author jalkanen
 *
 */
public class GenericNodeType
     implements NodeType
{
    /**
     *  Priha does not support multiple inheritance.
     */
    protected NodeType[]           m_parents = new NodeType[0];
    protected PropertyDefinition[] m_propertyDefinitions;
    protected PropertyDefinition[] m_declaredPropertyDefinitions;
    protected NodeDefinition[]     m_childNodeDefinitions;

    protected String               m_primaryItemName = null;
    protected String               m_name;

    protected boolean              m_ismixin;
    protected boolean              m_hasOrderableChildNodes;

    public GenericNodeType(String name)
    {
        m_name = name;
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
        return m_childNodeDefinitions;
    }

    public NodeDefinition[] getDeclaredChildNodeDefinitions()
    {
        return m_childNodeDefinitions;
    }

    public PropertyDefinition[] getDeclaredPropertyDefinitions()
    {
        return m_declaredPropertyDefinitions;
    }

    public NodeType[] getDeclaredSupertypes()
    {
        return getSupertypes();
    }

    public String getName()
    {
        return m_name;
    }

    public String getPrimaryItemName()
    {
        return m_primaryItemName;
    }

    public PropertyDefinition[] getPropertyDefinitions()
    {
        return m_propertyDefinitions;
    }

    public NodeType[] getSupertypes()
    {
        return m_parents;
    }

    public boolean hasOrderableChildNodes()
    {
        return m_hasOrderableChildNodes;
    }

    public boolean isMixin()
    {
        return m_ismixin;
    }

    public boolean isNodeType(String nodeTypeName)
    {
        if( m_name.equals(nodeTypeName) ) return true;

        for( int i = 0; i < m_parents.length; i++ )
        {
            if( m_parents[i].isNodeType(nodeTypeName) ) return true;
        }

        return false;
    }


    public NodeDefinition findNodeDefinition( String name )
    {
        for( NodeDefinition nd : m_childNodeDefinitions )
        {
            if( nd.getName().equals(name) )
            {
                return nd;
            }
        }

        for( NodeDefinition nd : m_childNodeDefinitions )
        {
            if( nd.getName().equals("*") )
            {
                return nd;
            }
        }

        return null;
    }

    public PropertyDefinition findPropertyDefinition( String name, boolean multiple )
    {
        for( PropertyDefinition pd : m_propertyDefinitions )
        {
            if( pd.getName().equals(name) && pd.isMultiple() == multiple )
            {
                return pd;
            }
        }

        //
        //  Attempt to find the default.
        //
        for( PropertyDefinition pd : m_propertyDefinitions )
        {
            if( pd.getName().equals("*") && pd.isMultiple() == multiple )
            {
                return pd;
            }
        }

        return null;
    }

    public String toString()
    {
        return "NodeType: "+m_name;
    }
}
