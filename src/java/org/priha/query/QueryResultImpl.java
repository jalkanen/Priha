package org.priha.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.*;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.priha.core.NodeImpl;
import org.priha.util.NodeIteratorImpl;
import org.priha.util.QName;

public class QueryResultImpl implements QueryResult
{
    private List<NodeImpl> m_matches;
    private List<QName>    m_columnNames;
    
    public QueryResultImpl(List<NodeImpl> matches, List<QName> columnNames)
    {
        m_matches     = matches;
        m_columnNames = columnNames;
    }

    public String[] getColumnNames() throws RepositoryException
    {
        Set<String> names = new TreeSet<String>();
        
        if( m_matches.size() > 0 )
        {
            NodeImpl ni = m_matches.iterator().next();
            
            for( QName q : m_columnNames )
            {
                names.add( ni.getSession().fromQName( q ) );
                System.out.println("COLUMN "+q);
            }
        }
        //System.out.println("Column names:");
/*
        // FIXME: Is kinda slow.
        for( NodeImpl nd : m_matches )
        {
            PropertyIterator props = nd.getProperties();
            
            while( props.hasNext() )
            {
                Property pi = props.nextProperty();
                //
                //  Multiproperties are never matched.
                //
                if( !pi.getDefinition().isMultiple() )
                    names.add( pi.getName() );
            }
        }
  */      
        //
        //  These two always exist in addition to the search result.
        //
        names.add( "jcr:path" );
        names.add( "jcr:score" );
        
        return names.toArray( new String[0] );
    }

    public NodeIterator getNodes() throws RepositoryException
    {
        return new NodeIteratorImpl( m_matches );
    }

    public RowIterator getRows() throws RepositoryException
    {
        return new RowIteratorImpl( m_matches );
    }

    private class RowIteratorImpl extends NodeIteratorImpl implements RowIterator
    {
        public RowIteratorImpl(List<NodeImpl> list)
        {
            super(list);
        }

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
