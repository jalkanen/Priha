package org.priha.query;

import java.util.*;

import javax.jcr.*;
import javax.jcr.query.QueryResult;
import javax.xml.namespace.QName;

import org.priha.core.ItemImpl;
import org.priha.core.NodeImpl;
import org.priha.core.PropertyImpl;
import org.priha.core.SessionImpl;
import org.priha.query.aqt.*;
import org.priha.query.aqt.OrderQueryNode.OrderSpec;
import org.priha.util.NodeIteratorImpl;
import org.priha.util.Path;

/**
 *  This class provides a very simple query provider which does direct comparisons
 *  against the contents of the repository. The upside is that this makes it very
 *  simple; with the obvious downside that this is really slow because it
 *  traverses the entire repository one matched Node at a time.
 *  
 *  @author Janne Jalkanen
 */
public class SimpleQueryProvider extends TraversingQueryNodeVisitor implements QueryProvider
{
    public QueryResult query(SessionImpl session, QueryRootNode nd) throws RepositoryException
    {
        QueryCollector c = new QueryCollector();
        
        c.setCurrentItem( session.getRootNode() );
        visit( nd, c );
        
        /*
        System.out.println(nd.dump());
        
        if( c.m_matches.size() == 0 ) System.out.println("No matches");
        
        for( ItemImpl ii : c.m_matches )
        {
            System.out.println(ii);
        }
        */

        System.out.println("---");
        for( ItemImpl ii : c.m_matches )
        {
            System.out.println(ii);
        }

        if( nd.getOrderNode() != null )
        {
            Collections.sort( c.m_matches, new QuerySorter( nd.getOrderNode() ) );

            System.out.println("+++");
            for( ItemImpl ii : c.m_matches )
            {
                System.out.println(ii);
            }
        }

        return new QueryResultImpl( c.m_matches );
    }



    @Override
    public Object visit( OrderQueryNode node, Object data ) throws RepositoryException
    {
        System.out.println("O "+node.dump());
        // TODO Auto-generated method stub
        return super.visit( node, data );
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
        Boolean result = false;
        PropertyImpl prop = null;
        
        NodeImpl currNode = qc.getCurrentItem();
        
        if( currNode.hasProperty(relPath.toString()) )
        {
            prop = currNode.getProperty(relPath.toString());
            
            if( prop.getDefinition().isMultiple() ) return null;
        }
        
        switch( node.getOperation() )
        {
            case QueryConstants.OPERATION_NOT_NULL:
                result = prop != null;
                break;

            case QueryConstants.OPERATION_GT_VALUE:
            case QueryConstants.OPERATION_GT_GENERAL:
                if( prop != null )
                {
                    switch( node.getValueType() )
                    {
                        case QueryConstants.TYPE_DOUBLE:
                            result = prop.getDouble() > node.getDoubleValue();
                            break;
                        case QueryConstants.TYPE_LONG:
                            result = prop.getLong() > node.getLongValue();
                            break;
                        case QueryConstants.TYPE_STRING:
                            result = prop.getString().compareTo(node.getStringValue()) > 0;
                            break;
                        case QueryConstants.TYPE_DATE:
                        case QueryConstants.TYPE_TIMESTAMP:
                            result = prop.getDate().getTime().after(node.getDateValue());
                            break;
                    }
                }
                    
                break;
              
            case QueryConstants.OPERATION_EQ_VALUE:
            case QueryConstants.OPERATION_EQ_GENERAL:
                if( prop != null )
                {
                    switch(node.getValueType())
                    {
                        case QueryConstants.TYPE_LONG:
                            result = prop.getLong() == node.getLongValue();
                            break;
                        case QueryConstants.TYPE_DOUBLE:
                            result = prop.getDouble() == node.getDoubleValue();
                            break;                            
                        case QueryConstants.TYPE_STRING:
                            result = prop.getString().equals(node.getStringValue());
                            break;
                        case QueryConstants.TYPE_DATE:
                        case QueryConstants.TYPE_TIMESTAMP:
                            result = prop.getDate().getTime().compareTo(node.getDateValue()) == 0;
                            break;
                    }
                }
                break;
                
            case QueryConstants.OPERATION_LT_VALUE:
            case QueryConstants.OPERATION_LT_GENERAL:
                if( prop != null )
                {
                    switch(node.getValueType())
                    {
                        case QueryConstants.TYPE_LONG:
                            result = prop.getLong() < node.getLongValue();
                            break;
                        case QueryConstants.TYPE_DOUBLE:
                            result = prop.getDouble() < node.getDoubleValue();
                            break;                            
                        case QueryConstants.TYPE_STRING:
                            result = prop.getString().compareTo(node.getStringValue()) < 0;
                            break;
                        case QueryConstants.TYPE_DATE:
                        case QueryConstants.TYPE_TIMESTAMP:
                            result = prop.getDate().getTime().before(node.getDateValue());
                            break;

                    }                    
                }
                break;
                
            case QueryConstants.OPERATION_GE_VALUE:
            case QueryConstants.OPERATION_GE_GENERAL:
                if( prop != null )
                {
                    switch(node.getValueType())
                    {
                        case QueryConstants.TYPE_LONG:
                            result = prop.getLong() >= node.getLongValue();
                            break;
                        case QueryConstants.TYPE_DOUBLE:
                            result = prop.getDouble() >= node.getDoubleValue();
                            break;                            
                        case QueryConstants.TYPE_STRING:
                            result = prop.getString().compareTo(node.getStringValue()) >= 0;
                            break;
                        case QueryConstants.TYPE_DATE:
                        case QueryConstants.TYPE_TIMESTAMP:
                            result = prop.getDate().getTime().compareTo(node.getDateValue()) >= 0;
                            break;

                    }
                }
                break;
                
            case QueryConstants.OPERATION_LE_VALUE:
            case QueryConstants.OPERATION_LE_GENERAL:
                if( prop != null )
                {
                    switch(node.getValueType())
                    {
                        case QueryConstants.TYPE_LONG:
                            result = prop.getLong() <= node.getLongValue();
                            break;
                        case QueryConstants.TYPE_DOUBLE:
                            result = prop.getDouble() <= node.getDoubleValue();
                            break;                            
                        case QueryConstants.TYPE_STRING:
                            result = prop.getString().compareTo(node.getStringValue()) <= 0;
                            break;
                        case QueryConstants.TYPE_DATE:
                        case QueryConstants.TYPE_TIMESTAMP:
                            result = prop.getDate().getTime().compareTo(node.getDateValue()) <= 0;
                            break;

                    }
                }
                break;
                                
            case QueryConstants.OPERATION_NULL:
                result = prop == null;
                break;
                
            default:
                throw new UnsupportedRepositoryOperationException("Unknown operation "+node.getOperation());
        }
       
        return result ? qc : null;
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

    @Override
    public Object visit( TextsearchQueryNode node, Object data ) throws RepositoryException
    {
        // System.out.println("Searching for "+node.getQuery()+" from path "+node.getRelativePath());
        
        QueryCollector c = (QueryCollector) data;
        NodeImpl currNode = c.getCurrentItem();
        
        int score = 0;
        
        PropertyIterator pi = currNode.getProperties();
        
        Path checkPath = node.getRelativePath();

        //
        //  The tokens are cached in the QueryNode so that we can avoid reparsing them
        //  every time a node is visited.
        //  We do this in a lazy manner.
        //
        String[] tokens = (String[])node.getAttribute( "parsedTokens" );
        
        if( tokens == null )
        {
            tokens = parseLine(node.getQuery());
            node.setAttribute("parsedTokens",tokens);
        }
        
        while( pi.hasNext() )
        {
            PropertyImpl p = (PropertyImpl)pi.nextProperty();
            
            if( checkPath != null && !checkPath.getLastComponent().equals( p.getQName() ) )
                continue;
            
            switch( p.getType() )
            {
                case PropertyType.STRING:
                    String val = p.getString();
                    
                    for( int i = 0; i < tokens.length; i++ )
                    {
                        // FIXME: Should be cached somehow so that there's no need to create a new String.
                        if( tokens[i].charAt(0) == '-' && val.contains(tokens[i].substring(1)))
                        {
                            score = 0;
                            break;
                        }
                        else if( val.contains( tokens[i] ) ) 
                        {
                            score++;
                        }
                    }
                    
                    break;
                    
                default:
                    // No action
                    break;    
            }
        }
        
        return score > 0 ? c : null;
    }


    /**
     * //element(*,"nt:base")
     */
    @Override
    public Object visit( NodeTypeQueryNode node, Object data ) throws RepositoryException
    {
        QueryCollector c = (QueryCollector) data;
        NodeImpl currNode = c.getCurrentItem();
        
        if( currNode.isNodeType( currNode.getSession().fromQName( node.getValue() ) ) )
            return c;
        
        return null;
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
    
    /**
     * Parses an incoming String and returns an array of elements.
     * 
     * 
     * @param nextLine
     *            the string to parse
     * @return the comma-tokenized list of elements, or null if nextLine is null
     * @throws IOException if bad things happen during the read
     */
    /*
    Copyright 2005 Bytecode Pty Ltd.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
    */

    private String[] parseLine(String nextLine) 
    {
        if (nextLine == null) 
        {
            return null;
        }

        List<String> tokensOnThisLine = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < nextLine.length(); i++) 
        {
            char c = nextLine.charAt(i);
            if (c == '\"') 
            {
                // this gets complex... the quote may end a quoted block, or escape another quote.
                // do a 1-char lookahead:
                if( inQuotes  // we are in quotes, therefore there can be escaped quotes in here.
                    && nextLine.length() > (i+1)  // there is indeed another character to check.
                    && nextLine.charAt(i+1) == '\"' )
                {   // ..and that char. is a quote also.
                    // we have two quote chars in a row == one quote char, so consume them both and
                    // put one on the token. we do *not* exit the quoted text.
                    sb.append(nextLine.charAt(i+1));
                    i++;
                }
                else
                {
                    inQuotes = !inQuotes;
                    // the tricky case of an embedded quote in the middle: a,bc"d"ef,g
                    if(i>2 //not on the begining of the line
                        && nextLine.charAt(i-1) != ' ' //not at the begining of an escape sequence 
                            && nextLine.length()>(i+1) &&
                            nextLine.charAt(i+1) != ' ' )//not at the end of an escape sequence
                    {
                        sb.append(c);
                    }
                }
            } 
            else if (c == ' ' && !inQuotes) 
            {
                tokensOnThisLine.add(sb.toString());
                sb = new StringBuilder(); // start work on next token
            }
            else 
            {
                sb.append(c);
            }
        }
        
        tokensOnThisLine.add(sb.toString());
        return tokensOnThisLine.toArray(new String[0]);
    }

    private class QuerySorter implements Comparator<NodeImpl>
    {
        OrderSpec[] m_specs;
        
        public QuerySorter( OrderQueryNode orderNode )
        {
            m_specs = orderNode.getOrderSpecs();
            System.out.println("Sorting by "+m_specs[0].getProperty());
        }

        public int compare( NodeImpl o1, NodeImpl o2 )
        {
            for( int i = 0; i < m_specs.length; i++ )
            {
                QName propName = m_specs[i].getProperty();
                PropertyImpl p1 = null, p2 = null;
                int result = 0;
                
                try
                {
                    p1 = o1.getProperty( propName );
                    p2 = o2.getProperty( propName );
                    
                    result = p1.getValue().compareTo( p2.getValue() );
                    System.out.println("p1 ="+p1+", p2="+p2+", result="+result);                    
                }
                catch( PathNotFoundException e )
                {
                    if( p1 == null ) result = 1;
                    if( p2 == null ) result = -1;
                }
                catch( RepositoryException e )
                {
                    return 0;
                }
                
                if( result == 0 ) continue; // Next property
                
                return m_specs[i].isAscending() ? result : -result;
            }
            return 0;
        }
        
    }
}
