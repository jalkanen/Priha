package org.priha.query;

import java.util.ArrayList;

import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.query.QueryResult;
import javax.xml.namespace.QName;

import org.priha.core.ItemImpl;
import org.priha.core.NodeImpl;
import org.priha.core.PropertyImpl;
import org.priha.core.SessionImpl;
import org.priha.query.aqt.*;
import org.priha.util.NodeIteratorImpl;
import org.priha.util.Path;

public class SimpleQueryProvider extends TraversingQueryNodeVisitor implements QueryProvider
{
    public QueryResult query(SessionImpl session, QueryRootNode nd) throws RepositoryException
    {
        QueryCollector c = new QueryCollector();
        
        c.setCurrentItem( session.getRootNode() );
        visit( nd, c );
        
        System.out.println(nd.dump());
        
        if( c.m_matches.size() == 0 ) System.out.println("No matches");
        
        for( ItemImpl ii : c.m_matches )
        {
            System.out.println(ii);
        }
        
        return new QueryResultImpl( c.m_matches );
    }



    @Override
    public Object visit(PathQueryNode node, Object data) throws RepositoryException
    {
        LocationStepQueryNode[] steps = node.getPathSteps();
        
        QueryCollector c = (QueryCollector)data;

        try
        {
            for( int i = 0; i < steps.length; i++ )
            {
                if( i == steps.length-1 ) 
                    c.m_isLast = true;
                c = (QueryCollector) steps[i].accept( this, c );
            }        
        }
        catch( PathNotFoundException e )
        {
            // No matches is found
            
            c.m_matches.clear();
        }
        
        return c;
    }



    @Override
    public Object visit(RelationQueryNode node, Object data) throws RepositoryException
    {
        Path relPath = node.getRelativePath();
        QueryCollector qc = (QueryCollector)data;
        Object result = null;
        
        NodeImpl currNode = qc.getCurrentItem();
        
        switch( node.getOperation() )
        {
            case QueryConstants.OPERATION_NOT_NULL:
                if( currNode.hasProperty(relPath.toString()) )
                {
                    result = qc;
                }
                
                break;

            case QueryConstants.OPERATION_GT_VALUE:
            case QueryConstants.OPERATION_GT_GENERAL:
                if( currNode.hasProperty(relPath.getLastComponent()) )
                {
                    PropertyImpl p = currNode.getProperty(relPath.getLastComponent());
                
                    if( p.getDouble() > node.getDoubleValue() )
                        result = qc;
                }
                break;
                
            default:
                throw new UnsupportedRepositoryOperationException("Unknown operation "+node.getOperation());
        }
       
        return result;
    }



    @Override
    public Object visit(LocationStepQueryNode node, Object data) throws RepositoryException
    {
        QName name = node.getNameTest();
        QueryCollector c = (QueryCollector) data;
        NodeImpl currNode = c.getCurrentItem();
        

        //
        //  If there is a named path component, then check that one.
        //  If there isn't, then check all children.
        //
        if( name != null )
        {
            // Check if a child by this name exists.
            
            if( currNode.hasNode(name) )
            {
                NodeImpl ni = currNode.getNode(name);
                
                c.setCurrentItem(ni);
                
                if( c.m_isLast && checkPredicates(node,c) )
                {
                    c.addMatch( ni );
                }
            }
            
            //
            //  If required, perform also the same check to all descendants of
            //  the current node.
            //
            if( node.getIncludeDescendants() )
            {
                for( NodeIteratorImpl iter = currNode.getNodes(); iter.hasNext(); )
                {
                    NodeImpl child = iter.nextNode();
                    
                    c.setCurrentItem(child);
                    visit( node, c );
                }
            }
        }
        else
        {
            //
            //  It is required to match all of the children.
            //
            
            for( NodeIteratorImpl iter = currNode.getNodes(); iter.hasNext(); )
            {
                NodeImpl child = iter.nextNode();
                
                c.setCurrentItem(child);
                
                if( c.m_isLast && checkPredicates(node,c) )
                    c.addMatch(child);
                
                visit( node,c );
            }
        }
        
        return c;
    }

    private boolean checkPredicates( LocationStepQueryNode node, QueryCollector data ) throws RepositoryException
    {
        Object[] result = node.acceptOperands(this, data);
        
        if( node.getPredicates().length != 0 && result.length == 0 )
        {
            return false;
        }
        
        return true;
    }

    /**
     *  This class collects the results of the Query.
     */
    
    private class QueryCollector
    {
        public boolean m_isLast;
        private NodeImpl m_currentItem;
        private ArrayList<NodeImpl> m_matches = new ArrayList<NodeImpl>();
        
        public NodeImpl getCurrentItem()
        {
            return m_currentItem;
        }

        public void setCurrentItem(NodeImpl item)
        {
            m_currentItem = item;
        }
        
        public void addMatch( NodeImpl ii )
        {
            m_matches.add( ii );
        }
    }
}
