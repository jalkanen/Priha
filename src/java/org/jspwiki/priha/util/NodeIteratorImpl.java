package org.jspwiki.priha.util;

import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

public class NodeIteratorImpl extends GenericIterator
    implements NodeIterator
{
    public NodeIteratorImpl( Collection<Node> list )
    {
        super(list);
    }
    
    public Node nextNode()
    {
        Node next = (Node)next();
        return next;
    }


}
