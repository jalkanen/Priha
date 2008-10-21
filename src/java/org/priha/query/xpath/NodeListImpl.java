/**
 * 
 */
package org.priha.query.xpath;

import javax.jcr.RepositoryException;

import org.priha.core.NodeImpl;
import org.priha.util.NodeIteratorImpl;
import org.w3c.dom.NodeList;

public class NodeListImpl implements NodeList
{
    private NodeIteratorImpl m_nodes;
    
    public NodeListImpl( NodeIteratorImpl nodes )
    {
        m_nodes = nodes;
    }

    public int getLength()
    {
        return (int) m_nodes.getSize();
    }

    public DOMNode item( int index )
    {
        NodeImpl ni = m_nodes.get(index);
        
        try
        {
            return new DOMElement(ni);
        }
        catch( RepositoryException e )
        {
            return null;
        }
    }
    
    public NodeImpl getNode(int index)
    {
        return m_nodes.get(index);
    }
    
    public NodeIteratorImpl getNodes()
    {
        return new NodeIteratorImpl(m_nodes);
    }
}