package org.jspwiki.priha.util;

import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

public class NodeIteratorImpl extends GenericIterator
    implements NodeIterator, Iterator
{
    public NodeIteratorImpl( List list )
    {
        super(list);
    }
    
    public Node nextNode()
    {
        return (Node) next();
    }


}
