package org.priha.query.xpath;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.*;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.priha.core.NodeImpl;
import org.priha.core.SessionImpl;
import org.priha.util.GenericIterator;
import org.w3c.dom.NodeList;

public class XPathQueryResult implements QueryResult
{

    private NodeList    m_list;
    private SessionImpl m_session;
    
    public XPathQueryResult( SessionImpl session, NodeList ns )
    {
        m_session = session;
        m_list = ns;
    }

    public String[] getColumnNames() throws RepositoryException
    {
        ArrayList<String> names = new ArrayList<String>();
        
        //System.out.println("Column names:");
        
        for( int i = 0; i < m_list.getLength(); i++ )
        {
            //System.out.println(":"+m_list.item( i ));
            DOMNode n = (DOMNode)m_list.item( i );

            NodeImpl nd = n.getJCRNode();
            
            PropertyIterator props = nd.getProperties();
            
            while( props.hasNext() )
                names.add( props.nextProperty().getName() );
            
        }
        
        //
        //  These two always exist in addition to the search result.
        //
        names.add( "jcr:path" );
        names.add( "jcr:score" );
        
        return names.toArray( new String[0] );
    }
    
    public NodeIterator getNodes() throws RepositoryException
    {
        return new NodeListIterator();
    }

    public RowIterator getRows() throws RepositoryException
    {
        return new RowIteratorImpl();
    }

    /**
     *  Provides a custom NodeIterator for the results of the
     *  search.
     */
    private class NodeListIterator implements NodeIterator
    {
        private int      m_currIdx = 0;

        public Node nextNode()
        {
            if(!hasNext()) throw new NoSuchElementException();
            
            try
            {
                return ((DOMNode)m_list.item(m_currIdx++)).getJCRNode();
            }
            catch (RepositoryException e)
            {
                e.printStackTrace();
                return null;
            }
        }

        public long getPosition()
        {
            return m_currIdx;
        }

        public long getSize()
        {
            return m_list.getLength();
        }

        public void skip(long arg0)
        {
            m_currIdx += arg0;
        }

        public boolean hasNext()
        {
            return m_currIdx < getSize();
        }

        public Object next()
        {
            return nextNode();
        }

        public void remove()
        {
            // No-op
        }

    }
    
    private class RowIteratorImpl extends NodeListIterator implements RowIterator
    {
        public Row nextRow()
        {
            Node nd = nextNode();
            
            return new RowImpl(nd);
        }

        public Object next()
        {
            return nextRow();
        }        
    }
    
    public static class RowImpl implements Row
    {
        private Node m_node;
        
        public RowImpl(Node nd)
        {
            m_node = nd;
        }

        public Value getValue(String arg0) throws ItemNotFoundException, RepositoryException
        {
            if( arg0.equals("jcr:path") )
            {
                return m_node.getSession().getValueFactory().createValue(m_node.getPath(),PropertyType.PATH);
            }
            else if( arg0.equals("jcr:score") )
            {
                return m_node.getSession().getValueFactory().createValue( 0 ); // FIXME!
            }
            
            return m_node.getProperty(arg0).getValue();
        }

        public Value[] getValues() throws RepositoryException
        {
            ArrayList<Value> values = new ArrayList<Value>();
            
            for( PropertyIterator pi = m_node.getProperties(); pi.hasNext(); )
            {
                Property p = pi.nextProperty();
                
                values.add( p.getValue() );
            }
            
            values.add( getValue("jcr:path") );
            values.add( getValue("jcr:score") );
            
            return values.toArray(new Value[values.size()]);
        }

    }

}
