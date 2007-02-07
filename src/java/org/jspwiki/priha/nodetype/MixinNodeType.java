package org.jspwiki.priha.nodetype;

import javax.jcr.nodetype.NodeType;

public class MixinNodeType extends GenericNodeType implements NodeType
{

    public MixinNodeType(String name)
    {
        super(name);
        // TODO Auto-generated constructor stub
    }

    public boolean isMixin()
    {
        return true;
    }
    
}
