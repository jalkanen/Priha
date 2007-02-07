package org.jspwiki.priha.nodetype;

import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.OnParentVersionAction;

public class ItemDefinitionImpl implements ItemDefinition
{
    protected NodeType m_nodeType;
    protected String   m_name; 
    protected boolean  m_isAutoCreated = false;
    protected boolean  m_isMandatory   = false;
    protected boolean  m_isProtected   = false;
    protected int      m_onParentVersion = OnParentVersionAction.COMPUTE;
    
    public ItemDefinitionImpl( NodeType nt, String name )
    {
        super();
        m_nodeType = nt;
        m_name     = name;
    }

    public NodeType getDeclaringNodeType()
    {
        return m_nodeType;
    }

    public String getName()
    {
        return m_name;
    }

    public int getOnParentVersion()
    {
        return m_onParentVersion;
    }

    public boolean isAutoCreated()
    {
        return m_isAutoCreated;
    }

    public boolean isMandatory()
    {
        return m_isMandatory;
    }

    public boolean isProtected()
    {
        return m_isProtected;
    }
    
}
