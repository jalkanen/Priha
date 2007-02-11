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
    protected NodeType             m_parent;
    protected PropertyDefinition[] m_propertyDefinitions;
    protected PropertyDefinition[] m_declaredPropertyDefinitions;
    protected NodeDefinition[]     m_childNodeDefinitions;

    protected String               m_primaryItemName = null;
    protected String               m_name; 
    
    protected boolean              m_ismixin;
    public boolean m_hasOrderableChildNodes;
    
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
        NodeType[] res;
        
        if( m_parent != null )
        {
            res = new NodeType[] { m_parent } ;
        }
        else
        {
            res = new NodeType[0];
        }
        
        return res;
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
        
        if( m_parent != null )
        {
            return m_parent.isNodeType(nodeTypeName);
        }
        
        return false;
    }

    
    public NodeDefinition findNodeDefinition( String name )
    {
        for( NodeDefinition nd : m_childNodeDefinitions )
        {
            if( nd.getName().equals(name) || nd.getName().equals("*") )
            {
                return nd;
            }
        }
        
        return null;
    }

    public PropertyDefinition findPropertyDefinition( String name )
    {
        for( PropertyDefinition pd : m_propertyDefinitions )
        {
            if( pd.getName().equals(name) || pd.getName().equals("*") )
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
