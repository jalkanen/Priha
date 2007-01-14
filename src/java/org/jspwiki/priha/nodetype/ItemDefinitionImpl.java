package org.jspwiki.priha.nodetype;

import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeType;

public class ItemDefinitionImpl implements ItemDefinition
{
    private NodeType m_nodeType;
    
    public ItemDefinitionImpl( NodeType nt )
    {
        super();
        m_nodeType = nt;
    }

    public NodeType getDeclaringNodeType()
    {
        return m_nodeType;
    }

    public String getName()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public int getOnParentVersion()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean isAutoCreated()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isMandatory()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isProtected()
    {
        // TODO Auto-generated method stub
        return false;
    }
    
}
