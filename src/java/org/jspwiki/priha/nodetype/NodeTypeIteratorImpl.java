package org.jspwiki.priha.nodetype;

import java.util.List;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;

import org.jspwiki.priha.util.GenericIterator;

public class NodeTypeIteratorImpl extends GenericIterator 
    implements NodeTypeIterator
{
    public NodeTypeIteratorImpl(List list)
    {
        super(list);
    }

    public NodeType nextNodeType()
    {
        return (NodeType)next();
    }
}
