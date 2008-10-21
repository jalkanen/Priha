package org.priha.query.xpath;

import java.util.ArrayList;

import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.priha.core.NodeImpl;
import org.priha.core.SessionImpl;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XPathQueryResult implements QueryResult
{
    private NodeList m_list;
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
        return null;
        //return m_list.getNodes();
    }

    public RowIterator getRows() throws RepositoryException
    {
        return new RowIteratorImpl();
    }

    public class RowIteratorImpl implements RowIterator
    {

        public Row nextRow()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public long getPosition()
        {
            // TODO Auto-generated method stub
            return 0;
        }

        public long getSize()
        {
            // TODO Auto-generated method stub
            return 0;
        }

        public void skip( long arg0 )
        {
            // TODO Auto-generated method stub
            
        }

        public boolean hasNext()
        {
            // TODO Auto-generated method stub
            return false;
        }

        public Object next()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public void remove()
        {
            // TODO Auto-generated method stub
            
        }
        
    }
}
