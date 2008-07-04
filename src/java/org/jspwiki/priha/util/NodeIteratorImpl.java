package org.jspwiki.priha.util;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

public class NodeIteratorImpl extends GenericIterator
    implements NodeIterator
{
    public NodeIteratorImpl( List<Node> list )
    {
        super(list);
    }
    
    public Node nextNode()
    {
        Node next = (Node)next();
        return next;
    }


}
