package org.priha.query;

import java.io.IOException;
import java.util.*;

import javax.jcr.*;
import javax.jcr.query.QueryResult;

import org.priha.core.*;
import org.priha.nodetype.QNodeType;
import org.priha.nodetype.QNodeTypeManager;
import org.priha.nodetype.QPropertyDefinition;
import org.priha.path.Path;
import org.priha.query.aqt.*;
import org.priha.query.aqt.OrderQueryNode.OrderSpec;
import org.priha.util.QName;

/**
 * This class provides a very simple query provider which does direct
 * comparisons against the contents of the repository. The upside is that this
 * makes it very simple; with the obvious downside that this is really slow
 * because it traverses the entire repository one matched Node at a time.
 * 
 * @author Janne Jalkanen
 */
public class SimpleQueryProvider extends TraversingQueryNodeVisitor implements QueryProvider
{
    public QueryResult query( SessionImpl session, QueryRootNode root ) throws RepositoryException
    {
        QueryCollector c = new QueryCollector();

        c.setCurrentItem( session.getRootNode() );
        visit( root, c );

        System.out.println( root.dump() );
        /*
         * if( c.m_matches.size() == 0 ) System.out.println("No matches"); for(
         * ItemImpl ii : c.m_matches ) { System.out.println(ii); }
         */

        System.out.println( "---" );
        for( ItemImpl ii : c.m_matches )
        {
            System.out.println( ii );
        }

        if( root.getOrderNode() != null )
        {
            Collections.sort( c.m_matches, new QuerySorter( root.getOrderNode() ) );

            System.out.println( "+++" );
            for( ItemImpl ii : c.m_matches )
            {
                System.out.println( ii );
            }
        }

        //
        // Figure out which columns were selected.
        //
        List<QName> columns = new ArrayList<QName>();
        columns.addAll( Arrays.asList( root.getSelectProperties() ) );

        // Shamelessly lifted from Jackrabbit :-)
        if( columns.size() == 0 )
        {
            // use node type constraint
            LocationStepQueryNode[] steps = root.getLocationNode().getPathSteps();
            final QName[] ntName = new QName[1];
            steps[steps.length - 1].acceptOperands( new DefaultQueryNodeVisitor() {

                public Object visit( AndQueryNode node, Object data ) throws RepositoryException
                {
                    return node.acceptOperands( this, data );
                }

                public Object visit( NodeTypeQueryNode node, Object data )
                {
                    ntName[0] = node.getValue();
                    return data;
                }
            }, null );
            
            if( ntName[0] == null )
            {
                ntName[0] = JCRConstants.Q_NT_BASE;
            }
            
            QNodeType nt = QNodeTypeManager.getInstance().getNodeType( ntName[0] );
            QPropertyDefinition[] propDefs = nt.getQPropertyDefinitions();
            
            for( QPropertyDefinition propDef : propDefs )
            {
                if( !propDef.isWildCard() && !propDef.isMultiple() )
                {
                    columns.add( propDef.getQName() );
                }
            }
        }

        return new QueryResultImpl( c.m_matches, columns );
    }

    @Override
    public Object visit( AndQueryNode node, Object data ) throws RepositoryException
    {
        QueryCollector c = (QueryCollector) data;

        QueryNode[] operands = node.getOperands();

        for( int i = 0; i < operands.length; i++ )
        {
            c = (QueryCollector) operands[i].accept( this, c );

            // Stop at the first sign of non-match
            if( c == null )
                return null;
        }

        return c;
    }

    @Override
    public Object visit( OrderQueryNode node, Object data ) throws RepositoryException
    {
        // TODO Auto-generated method stub
        return super.visit( node, data );
    }

    @Override
    public Object visit( PathQueryNode node, Object data ) throws RepositoryException
    {
        LocationStepQueryNode[] steps = node.getPathSteps();

        QueryCollector c = (QueryCollector) data;

        try
        {
            ArrayList<NodeImpl> nodesToBeChecked = new ArrayList<NodeImpl>();
            
            // Start with a single item
            nodesToBeChecked.add( c.getCurrentItem() );
            
            for( int i = 0; i < steps.length; i++ )
            {
                if( i == steps.length - 1 )
                    c.setLast(true);
                
                for( NodeImpl ni : nodesToBeChecked )
                {
                    c.setCurrentItem( ni );
                
                    c = (QueryCollector) steps[i].accept( this, c );
                }
                nodesToBeChecked.clear();
                nodesToBeChecked.addAll( c.m_matches );
                c.m_matches.clear();
            }
            
            c.m_matches = nodesToBeChecked; // Should be the final list of matches
        }
        catch( PathNotFoundException e )
        {
            // No matches is found

            c.m_matches.clear();
        }

        return c;
    }

    /**
     * Is used to evaluate a single Value against a constraint.
     */
    private interface OperationResolver
    {
        public Boolean eval( Value value ) throws RepositoryException;
    }

    /**
     * Evaluates all values of the given property using the given
     * OperationResolver and returns a logical OR of the results.
     */
    private Boolean resolveOp( PropertyImpl prop, OperationResolver resolver ) throws ValueFormatException, RepositoryException
    {
        Boolean result = false;

        if( prop.getDefinition().isMultiple() )
        {
            for( Value v : prop.getValues() )
            {
                result |= resolver.eval( v );
            }
        }
        else
        {
            result = resolver.eval( prop.getValue() );
        }

        return result;
    }

    @Override
    public Object visit( final RelationQueryNode node, Object data ) throws RepositoryException
    {
        Path relPath = node.getRelativePath();
        QueryCollector qc = (QueryCollector) data;
        Boolean result = false;
        PropertyImpl prop = null;

        NodeImpl currNode = qc.getCurrentItem();

        if( currNode.hasProperty( relPath.toString() ) )
        {
            prop = currNode.getProperty( relPath.toString() );
        }

        switch( node.getOperation() )
        {
            case QueryConstants.OPERATION_NOT_NULL:
                result = (prop != null);
                break;

            case QueryConstants.OPERATION_GT_VALUE:
            case QueryConstants.OPERATION_GT_GENERAL:
                if( prop != null )
                {
                    switch( node.getValueType() )
                    {
                        case QueryConstants.TYPE_DOUBLE:
                            result = resolveOp( prop, new OperationResolver() {
                                public Boolean eval( Value value )
                                                                  throws ValueFormatException,
                                                                      IllegalStateException,
                                                                      RepositoryException
                                {
                                    return value.getDouble() > node.getDoubleValue();
                                }
                            } );
                            break;

                        case QueryConstants.TYPE_LONG:
                            result = resolveOp( prop, new OperationResolver() {
                                public Boolean eval( Value value )
                                                                  throws ValueFormatException,
                                                                      IllegalStateException,
                                                                      RepositoryException
                                {
                                    return value.getLong() > node.getLongValue();
                                }
                            } );
                            break;

                        case QueryConstants.TYPE_STRING:
                            result = resolveOp( prop, new OperationResolver() {
                                public Boolean eval( Value value )
                                                                  throws ValueFormatException,
                                                                      IllegalStateException,
                                                                      RepositoryException
                                {
                                    return value.getString().compareTo( node.getStringValue() ) > 0;
                                }
                            } );
                            break;

                        case QueryConstants.TYPE_DATE:
                        case QueryConstants.TYPE_TIMESTAMP:
                            result = resolveOp( prop, new OperationResolver() {
                                public Boolean eval( Value value )
                                                                  throws ValueFormatException,
                                                                      IllegalStateException,
                                                                      RepositoryException
                                {
                                    return value.getDate().getTime().after( node.getDateValue() );
                                }
                            } );
                            break;
                    }
                }

                break;

            case QueryConstants.OPERATION_EQ_VALUE:
            case QueryConstants.OPERATION_EQ_GENERAL:
                if( prop != null )
                {
                    switch( node.getValueType() )
                    {
                        case QueryConstants.TYPE_LONG:
                            result = resolveOp( prop, new OperationResolver() {
                                public Boolean eval( Value value )
                                                                  throws ValueFormatException,
                                                                      IllegalStateException,
                                                                      RepositoryException
                                {
                                    return value.getLong() == node.getLongValue();
                                }
                            } );
                            break;
                        case QueryConstants.TYPE_DOUBLE:
                            result = resolveOp( prop, new OperationResolver() {
                                public Boolean eval( Value value )
                                                                  throws ValueFormatException,
                                                                      IllegalStateException,
                                                                      RepositoryException
                                {
                                    return (value.getDouble() - node.getDoubleValue()) < 1e-12;
                                }
                            } );
                            break;
                        case QueryConstants.TYPE_STRING:
                            result = resolveOp( prop, new OperationResolver() {
                                public Boolean eval( Value value )
                                                                  throws ValueFormatException,
                                                                      IllegalStateException,
                                                                      RepositoryException
                                {
                                    return value.getString().equals( node.getStringValue() );
                                }
                            } );
                            break;
                        case QueryConstants.TYPE_DATE:
                        case QueryConstants.TYPE_TIMESTAMP:
                            result = resolveOp( prop, new OperationResolver() {
                                public Boolean eval( Value value )
                                                                  throws ValueFormatException,
                                                                      IllegalStateException,
                                                                      RepositoryException
                                {
                                    return value.getDate().getTime().compareTo( node.getDateValue() ) == 0;
                                }
                            } );
                            break;
                    }
                }
                break;

            case QueryConstants.OPERATION_LT_VALUE:
            case QueryConstants.OPERATION_LT_GENERAL:
                if( prop != null )
                {
                    switch( node.getValueType() )
                    {
                        case QueryConstants.TYPE_LONG:
                            result = resolveOp( prop, new OperationResolver() {
                                public Boolean eval( Value value )
                                                                  throws ValueFormatException,
                                                                      IllegalStateException,
                                                                      RepositoryException
                                {
                                    return value.getLong() < node.getLongValue();
                                }
                            } );

                            break;
                        case QueryConstants.TYPE_DOUBLE:
                            result = resolveOp( prop, new OperationResolver() {
                                public Boolean eval( Value value )
                                                                  throws ValueFormatException,
                                                                      IllegalStateException,
                                                                      RepositoryException
                                {
                                    return value.getDouble() < node.getDoubleValue();
                                }
                            } );
                            break;
                        case QueryConstants.TYPE_STRING:
                            result = resolveOp( prop, new OperationResolver() {
                                public Boolean eval( Value value )
                                                                  throws ValueFormatException,
                                                                      IllegalStateException,
                                                                      RepositoryException
                                {
                                    return value.getString().compareTo( node.getStringValue() ) < 0;
                                }
                            } );
                            break;
                        case QueryConstants.TYPE_DATE:
                        case QueryConstants.TYPE_TIMESTAMP:
                            result = resolveOp( prop, new OperationResolver() {
                                public Boolean eval( Value value )
                                                                  throws ValueFormatException,
                                                                      IllegalStateException,
                                                                      RepositoryException
                                {
                                    return value.getDate().getTime().before( node.getDateValue() );
                                }
                            } );
                            break;

                    }
                }
                break;

            case QueryConstants.OPERATION_GE_VALUE:
            case QueryConstants.OPERATION_GE_GENERAL:
                if( prop != null )
                {
                    switch( node.getValueType() )
                    {
                        case QueryConstants.TYPE_LONG:
                            result = resolveOp( prop, new OperationResolver() {
                                public Boolean eval( Value value )
                                                                  throws ValueFormatException,
                                                                      IllegalStateException,
                                                                      RepositoryException
                                {
                                    return value.getLong() >= node.getLongValue();
                                }
                            } );
                            break;
                        case QueryConstants.TYPE_DOUBLE:
                            result = resolveOp( prop, new OperationResolver() {
                                public Boolean eval( Value value )
                                                                  throws ValueFormatException,
                                                                      IllegalStateException,
                                                                      RepositoryException
                                {
                                    return value.getDouble() >= node.getDoubleValue();
                                }
                            } );
                            break;
                        case QueryConstants.TYPE_STRING:
                            result = resolveOp( prop, new OperationResolver() {
                                public Boolean eval( Value value )
                                                                  throws ValueFormatException,
                                                                      IllegalStateException,
                                                                      RepositoryException
                                {
                                    return value.getString().compareTo( node.getStringValue() ) >= 0;
                                }
                            } );
                            break;
                        case QueryConstants.TYPE_DATE:
                        case QueryConstants.TYPE_TIMESTAMP:
                            result = resolveOp( prop, new OperationResolver() {
                                public Boolean eval( Value value )
                                                                  throws ValueFormatException,
                                                                      IllegalStateException,
                                                                      RepositoryException
                                {
                                    return value.getDate().getTime().compareTo( node.getDateValue() ) >= 0;
                                }
                            } );
                            break;

                    }
                }
                break;

            case QueryConstants.OPERATION_LE_VALUE:
            case QueryConstants.OPERATION_LE_GENERAL:
                if( prop != null )
                {
                    switch( node.getValueType() )
                    {
                        case QueryConstants.TYPE_LONG:
                            result = resolveOp( prop, new OperationResolver() {
                                public Boolean eval( Value value )
                                                                  throws ValueFormatException,
                                                                      IllegalStateException,
                                                                      RepositoryException
                                {
                                    return value.getLong() <= node.getLongValue();
                                }
                            } );
                            break;
                        case QueryConstants.TYPE_DOUBLE:
                            result = resolveOp( prop, new OperationResolver() {
                                public Boolean eval( Value value )
                                                                  throws ValueFormatException,
                                                                      IllegalStateException,
                                                                      RepositoryException
                                {
                                    return value.getDouble() <= node.getDoubleValue();
                                }
                            } );
                            break;
                        case QueryConstants.TYPE_STRING:
                            result = resolveOp( prop, new OperationResolver() {
                                public Boolean eval( Value value )
                                                                  throws ValueFormatException,
                                                                      IllegalStateException,
                                                                      RepositoryException
                                {
                                    return value.getString().compareTo( node.getStringValue() ) <= 0;
                                }
                            } );
                            break;
                        case QueryConstants.TYPE_DATE:
                        case QueryConstants.TYPE_TIMESTAMP:
                            result = resolveOp( prop, new OperationResolver() {
                                public Boolean eval( Value value )
                                                                  throws ValueFormatException,
                                                                      IllegalStateException,
                                                                      RepositoryException
                                {
                                    return value.getDate().getTime().compareTo( node.getDateValue() ) <= 0;
                                }
                            } );
                            break;

                    }
                }
                break;

            case QueryConstants.OPERATION_NULL:
                result = prop == null;
                break;

            default:
                throw new UnsupportedRepositoryOperationException( "Unknown operation " + node.getOperation() );
        }

        return result ? qc : null;
    }

    @Override
    public Object visit( LocationStepQueryNode node, Object data ) throws RepositoryException
    {
        QName checkedName = node.getNameTest();
        QueryCollector c = (QueryCollector) data;
        NodeImpl currNode = c.getCurrentItem();

        System.out.println("LOC = "+currNode+", check="+checkedName);

        //
        // If there is a named path component, then check that one.
        // If there isn't, then check all children.
        //
        if( checkedName != null )
        {
            // Check if a child by this name exists.
            // Also include same name siblings.
            if( checkedName.getLocalPart().length() == 0 )
            {
                // Root node
                if( checkPredicates( node, c ) )
                {
                    c.addMatch( currNode );
                    System.out.println("   MATCH");
                }
            }
            else if( currNode.hasNode( checkedName ) )
            {
                for( NodeIterator iter = currNode.getNodes( currNode.getSession().fromQName( checkedName ) ); iter.hasNext(); )
                {
                    NodeImpl ni = (NodeImpl)iter.nextNode();
                    c.setCurrentItem( ni );
                    if( checkPredicates( node, c ) )
                    {
                        c.addMatch( ni );
                        System.out.println("   MATCH");
                    }
                }
            }

            //
            // If required, perform also the same check to all descendants of
            // the current node.
            //
            if( node.getIncludeDescendants() )
            {
                for( NodeIterator iter = currNode.getNodes(); iter.hasNext(); )
                {
                    NodeImpl child = (NodeImpl)iter.nextNode();

                    c.setCurrentItem( child );
                    visit( node, c );
                }
            }
        }
        else
        {
            //
            // It is required to match all of the children.
            //

            for( NodeIterator iter = currNode.getNodes(); iter.hasNext(); )
            {
                NodeImpl child = (NodeImpl)iter.nextNode();

                c.setCurrentItem( child );

                if( checkPredicates( node, c ) )
                    c.addMatch( child );

                if( node.getIncludeDescendants() )
                    visit( node, c );
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
        // The tokens are cached in the QueryNode so that we can avoid reparsing
        // them
        // every time a node is visited.
        // We do this in a lazy manner.
        //
        String[] tokens = (String[]) node.getAttribute( "parsedTokens" );

        if( tokens == null )
        {
            tokens = parseLine( node.getQuery() );
            node.setAttribute( "parsedTokens", tokens );
        }

        while ( pi.hasNext() )
        {
            PropertyImpl p = (PropertyImpl) pi.nextProperty();

            if( checkPath != null && !checkPath.getLastComponent().equals( p.getQName() ) )
                continue;

            switch( p.getType() )
            {
                case PropertyType.STRING:
                    String val = p.getString();

                    for( int i = 0; i < tokens.length; i++ )
                    {
                        // FIXME: Should be cached somehow so that there's no
                        // need to create a new String.
                        if( tokens[i].charAt( 0 ) == '-' && val.contains( tokens[i].substring( 1 ) ) )
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
        Object[] result = node.acceptOperands( this, data );

        if( node.getPredicates().length != 0 && result.length == 0 )
        {
            return false;
        }

        return true;
    }

    /**
     * This class collects the results of the Query.
     */

    private static class QueryCollector
    {
        private boolean m_isLast;

        private NodeImpl m_currentItem;

        private ArrayList<NodeImpl> m_matches = new ArrayList<NodeImpl>();

        /**
         *  Is this the last path component to be matched?
         */
        public final boolean isLast()
        {
            return m_isLast;
        }
        
        public final void setLast( boolean b )
        {
            m_isLast = b;
        }
        
        public final NodeImpl getCurrentItem()
        {
            return m_currentItem;
        }

        public final void setCurrentItem( NodeImpl item )
        {
            m_currentItem = item;
        }

        public final void addMatch( NodeImpl ii )
        {
            m_matches.add( ii );
        }
        
        public final String toString()
        {
            StringBuilder sb = new StringBuilder();
            
            try
            {
                sb.append("Current: ");
                sb.append( m_currentItem.getPath() );
                sb.append(" matches=[");
            
                for( NodeImpl ni : m_matches )
                {
                    sb.append( ni.getPath() );
                    sb.append( "," );
                }
                sb.append("]");
                if( isLast() ) sb.append(" (last step processed)");
            }
            catch( Exception e )
            {
                sb.append( e.getMessage() );
            }
            return sb.toString();
        }
    }

    /**
     * Parses an incoming String and returns an array of elements.
     * 
     * @param nextLine the string to parse
     * @return the comma-tokenized list of elements, or null if nextLine is null
     * @throws IOException if bad things happen during the read
     */
    /*
     * Copyright 2005 Bytecode Pty Ltd. Licensed under the Apache License,
     * Version 2.0 (the "License"); you may not use this file except in
     * compliance with the License. You may obtain a copy of the License at
     * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
     * law or agreed to in writing, software distributed under the License is
     * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
     * KIND, either express or implied. See the License for the specific
     * language governing permissions and limitations under the License.
     */

    private String[] parseLine( String nextLine )
    {
        if( nextLine == null )
        {
            return null;
        }

        List<String> tokensOnThisLine = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for( int i = 0; i < nextLine.length(); i++ )
        {
            char c = nextLine.charAt( i );
            if( c == '\"' )
            {
                // this gets complex... the quote may end a quoted block, or
                // escape another quote.
                // do a 1-char lookahead:
                if( inQuotes // we are in quotes, therefore there can be escaped
                             // quotes in here.
                    && nextLine.length() > (i + 1) // there is indeed another
                                                   // character to check.
                    && nextLine.charAt( i + 1 ) == '\"' )
                { // ..and that char. is a quote also.
                    // we have two quote chars in a row == one quote char, so
                    // consume them both and
                    // put one on the token. we do *not* exit the quoted text.
                    sb.append( nextLine.charAt( i + 1 ) );
                    i++;
                }
                else
                {
                    inQuotes = !inQuotes;
                    // the tricky case of an embedded quote in the middle:
                    // a,bc"d"ef,g
                    if( i > 2 // not on the begining of the line
                        && nextLine.charAt( i - 1 ) != ' ' // not at the
                                                           // begining of an
                                                           // escape sequence
                        && nextLine.length() > (i + 1) && nextLine.charAt( i + 1 ) != ' ' )// not
                                                                                           // at
                                                                                           // the
                                                                                           // end
                                                                                           // of
                                                                                           // an
                                                                                           // escape
                                                                                           // sequence
                    {
                        sb.append( c );
                    }
                }
            }
            else if( c == ' ' && !inQuotes )
            {
                tokensOnThisLine.add( sb.toString() );
                sb = new StringBuilder(); // start work on next token
            }
            else
            {
                sb.append( c );
            }
        }

        tokensOnThisLine.add( sb.toString() );
        return tokensOnThisLine.toArray( new String[0] );
    }

    private static class QuerySorter implements Comparator<NodeImpl>
    {
        OrderSpec[] m_specs;

        public QuerySorter( OrderQueryNode orderNode )
        {
            m_specs = orderNode.getOrderSpecs();
            System.out.println( "Sorting by " + m_specs[0].getProperty() );
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
                    System.out.println( "p1 =" + p1 + ", p2=" + p2 + ", result=" + result );
                }
                catch( PathNotFoundException e )
                {
                    if( p1 == null )
                        result = 1;
                    if( p2 == null )
                        result = -1;
                }
                catch( RepositoryException e )
                {
                    return 0;
                }

                if( result == 0 )
                    continue; // Next property

                return m_specs[i].isAscending() ? result : -result;
            }
            return 0;
        }

    }
}
