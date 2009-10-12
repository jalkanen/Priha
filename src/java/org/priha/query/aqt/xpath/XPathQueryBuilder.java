/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.priha.query.aqt.xpath;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;

import org.priha.RepositoryManager;
import org.priha.core.JCRConstants;
import org.priha.core.namespace.NamespaceMapper;
import org.priha.path.Path;
import org.priha.query.aqt.*;
import org.priha.util.ISO8601;
import org.priha.util.QName;
import org.priha.xml.XMLUtils;

/**
 * Query builder that translates a XPath statement into a query tree structure.
 */
public class XPathQueryBuilder implements XPathVisitor, XPathTreeConstants {

    /**
     * Namespace uri for xpath functions. See also class SearchManager
     */
    static final String NS_FN_URI = "http://www.w3.org/2005/xpath-functions";

    /**
     * Name for 'fn:not'
     */
    static final QName FN_NOT = new QName(NS_FN_URI, "not");

    /**
     * Name for 'fn:lower-case'
     */
    static final QName FN_LOWER_CASE = new QName(NS_FN_URI, "lower-case");

    /**
     * Name for 'fn:upper-case'
     */
    static final QName FN_UPPER_CASE = new QName(NS_FN_URI, "upper-case");

    /**
     * Name for 'not' as defined in XPath 1.0 (no prefix)
     */
    static final QName FN_NOT_10 = new QName("", "not");

    /**
     * Name for true function.
     */
    static final QName FN_TRUE = new QName("", "true");

    /**
     * Name for false function.
     */
    static final QName FN_FALSE = new QName("", "false");

    /**
     * Name for position function.
     */
    static final QName FN_POSITION = new QName("", "position");

    /**
     * Name for element function.
     */
    static final QName FN_ELEMENT = new QName("", "element");

    /**
     * Name for the full position function including bracket
     */
    static final QName FN_POSITION_FULL = new QName("", "position()");

    /**
     * Name for jcr:xmltext
     */
    static final QName JCR_XMLTEXT = new QName(JCRConstants.NS_JCP, "xmltext");

    /**
     * Name for last function.
     */
    static final QName FN_LAST = new QName("", "last");

    /**
     * Name for first function.
     */
    static final QName FN_FIRST = new QName("", "first");

    /**
     * Name for xs:dateTime
     */
    static final QName XS_DATETIME = new QName("http://www.w3.org/2001/XMLSchema", "dateTime");

    /**
     * Name for jcr:like
     */
    static final QName JCR_LIKE = new QName(JCRConstants.NS_JCP, "like");

    /**
     * Name for jcr:deref
     */
    static final QName JCR_DEREF = new QName(JCRConstants.NS_JCP, "deref");

    /**
     * Name for jcr:contains
     */
    static final QName JCR_CONTAINS = new QName(JCRConstants.NS_JCP, "contains");

    /**
     * Name for jcr:root
     */
    static final QName JCR_ROOT = new QName(JCRConstants.NS_JCP, "root");

    /**
     * Name for jcr:score
     */
    static final QName JCR_SCORE = new QName(JCRConstants.NS_JCP, "score");

    /**
     * Name for rep:similar
     */
    static final QName REP_SIMILAR = new QName(RepositoryManager.NS_PRIHA, "similar");

    /**
     * Name for rep:spellcheck
     */
    static final QName REP_SPELLCHECK = new QName(RepositoryManager.NS_PRIHA, "spellcheck");

    /**
     * String constant for operator 'eq'
     */
    private static final String OP_EQ = "eq";

    /**
     * String constant for operator 'ne'
     */
    private static final String OP_NE = "ne";

    /**
     * String constant for operator 'gt'
     */
    private static final String OP_GT = "gt";

    /**
     * String constant for operator 'ge'
     */
    private static final String OP_GE = "ge";

    /**
     * String constant for operator 'lt'
     */
    private static final String OP_LT = "lt";

    /**
     * String constant for operator 'le'
     */
    private static final String OP_LE = "le";

    /**
     * String constant for operator '='
     */
    private static final String OP_SIGN_EQ = "=";

    /**
     * String constant for operator '!='
     */
    private static final String OP_SIGN_NE = "!=";

    /**
     * String constant for operator '>'
     */
    private static final String OP_SIGN_GT = ">";

    /**
     * String constant for operator '>='
     */
    private static final String OP_SIGN_GE = ">=";

    /**
     * String constant for operator '<'
     */
    private static final String OP_SIGN_LT = "<";

    /**
     * String constant for operator '<='
     */
    private static final String OP_SIGN_LE = "<=";

    /**
     * Map of reusable XPath parser instances indexed by NamespaceResolver.
     */
//    private static final Map parsers = new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.WEAK);

    /**
     * The root <code>QueryNode</code>
     */
    private final QueryRootNode root;

    /**
     * The {@link NameResolver} in use
     */
    private final NamespaceMapper resolver;

    /**
     * List of exceptions that are created while building the query tree
     */
    private final List<RepositoryException> exceptions = new ArrayList<RepositoryException>();

    /**
     * Temporary relative path
     */
    private Path tmpRelPath;

    /**
     * The query node factory.
     */
    private final QueryNodeFactory factory;

    /**
     * Creates a new <code>XPathQueryBuilder</code> instance.
     *
     * @param statement the XPath statement.
     * @param resolver  the name resolver to use.
     * @param factory   the query node factory.
     * @throws InvalidQueryException if the XPath statement is malformed.
     */
    private XPathQueryBuilder(String statement,
                              NamespaceMapper resolver,
                              QueryNodeFactory factory)
            throws InvalidQueryException {
        this.resolver = resolver;
        this.factory = factory;
        this.root = factory.createQueryRootNode();
        try {
            // create an XQuery statement because we're actually using an
            // XQuery parser.
            statement = "for $v in " + statement + " return $v";
            // get parser
            XPath parser;
//            synchronized (parsers) {
//                parser = (XPath) parsers.get(resolver);
//                if (parser == null) {
                    parser = new XPath(new StringReader(statement));
//                    parsers.put(resolver, parser);
//                }
//            }

            SimpleNode query;
            // guard against concurrent use within same session
            synchronized (parser) {
                parser.ReInit(new StringReader(statement));
                query = parser.XPath2();
            }
            query.jjtAccept(this, root);
        } catch (ParseException e) {
            throw new InvalidQueryException(e.getMessage() + " for statement: " + statement, e);
        } catch (Throwable t) {
            // also catch any other exception
            throw new InvalidQueryException(t.getMessage() + " for statement: " + statement, t);
        }
        if (exceptions.size() > 0) {
            // simply report the first one
            Exception e = (Exception) exceptions.get(0);
            if (e instanceof InvalidQueryException) {
                // just re-throw
                throw (InvalidQueryException) e;
            } else {
                // otherwise package
                throw new InvalidQueryException(e.getMessage(), e);
            }
        }
    }

    /**
     * Creates a <code>QueryNode</code> tree from a XPath statement using the
     * passed query node <code>factory</code>.
     *
     * @param statement the XPath statement.
     * @param resolver  the name resolver to use.
     * @param factory   the query node factory.
     * @return the <code>QueryNode</code> tree for the XPath statement.
     * @throws InvalidQueryException if the XPath statement is malformed.
     */
    public static QueryRootNode createQuery(String statement,
                                            NamespaceMapper resolver,
                                            QueryNodeFactory factory)
            throws InvalidQueryException {
        return new XPathQueryBuilder(statement, resolver, factory).getRootNode();
    }

    /**
     * Creates a String representation of the query node tree in XPath syntax.
     *
     * @param root     the root of the query node tree.
     * @param resolver to resolve <code>Name</code>s.
     * @return a String representation of the query node tree.
     * @throws InvalidQueryException if the query node tree cannot be converted
     *                               into a String representation due to restrictions in XPath.
     */
    public static String toString(QueryRootNode root, NamespaceMapper resolver)
            throws InvalidQueryException {
        return QueryFormat.toString(root, resolver);
    }

    /**
     * Returns the root node of the <code>QueryNode</code> tree.
     *
     * @return the root node of the <code>QueryNode</code> tree.
     */
    QueryRootNode getRootNode() {
        return root;
    }

    //---------------------< XPathVisitor >-------------------------------------

    /**
     * Implements the generic visit method for this <code>XPathVisitor</code>.
     *
     * @param node the current node as created by the XPath parser.
     * @param data the current <code>QueryNode</code> created by this
     *             <code>XPathVisitor</code>.
     * @return the current <code>QueryNode</code>. Can be different from
     *         <code>data</code>.
     */
    public Object visit(SimpleNode node, Object data) {
        QueryNode queryNode = (QueryNode) data;
        switch (node.getId()) {
            case JJTXPATH2:
                queryNode = createPathQueryNode(node);
                break;
            case JJTROOT:
            case JJTROOTDESCENDANTS:
                if (queryNode instanceof PathQueryNode) {
                    ((PathQueryNode) queryNode).setAbsolute(true);
                } else {
                    exceptions.add(new InvalidQueryException(
                            "Unsupported root level query node: " + queryNode));
                }
                break;
            case JJTSTEPEXPR:
                if (isAttributeAxis(node)) {
                    if (queryNode.getType() == QueryNode.TYPE_RELATION
                            || (queryNode.getType() == QueryNode.TYPE_DEREF && ((DerefQueryNode) queryNode).getRefProperty() == null)
                            || queryNode.getType() == QueryNode.TYPE_ORDER
                            || queryNode.getType() == QueryNode.TYPE_PATH
                            || queryNode.getType() == QueryNode.TYPE_TEXTSEARCH) {
                        // traverse
                        node.childrenAccept(this, queryNode);
                    } else if (queryNode.getType() == QueryNode.TYPE_NOT) {
                        // is null expression
                        RelationQueryNode isNull
                                = factory.createRelationQueryNode(queryNode,
                                        RelationQueryNode.OPERATION_NULL);
                        applyRelativePath(isNull);
                        node.childrenAccept(this, isNull);
                        NotQueryNode notNode = (NotQueryNode) queryNode;
                        NAryQueryNode parent = (NAryQueryNode) notNode.getParent();
                        parent.removeOperand(notNode);
                        parent.addOperand(isNull);
                    } else {
                        // not null expression
                        RelationQueryNode notNull =
                                factory.createRelationQueryNode(queryNode,
                                        RelationQueryNode.OPERATION_NOT_NULL);
                        applyRelativePath(notNull);
                        node.childrenAccept(this, notNull);
                        ((NAryQueryNode) queryNode).addOperand(notNull);
                    }
                } else {
                    if (queryNode.getType() == QueryNode.TYPE_PATH) {
                        createLocationStep(node, (NAryQueryNode) queryNode);
                    } else if (queryNode.getType() == QueryNode.TYPE_TEXTSEARCH
                            || queryNode.getType() == QueryNode.TYPE_RELATION) {
                        node.childrenAccept(this, queryNode);
                    } else {
                        // step within a predicate
                        RelationQueryNode tmp = factory.createRelationQueryNode(
                                null, RelationQueryNode.OPERATION_NOT_NULL);
                        node.childrenAccept(this, tmp);
                        if (tmpRelPath == null) {
                            tmpRelPath = Path.EMPTY_RELATIVE_PATH;
                        }
                        tmpRelPath = tmpRelPath.resolve(tmp.getRelativePath().getLastComponent());
                    }
                }
                break;
            case JJTNAMETEST:
                if (queryNode.getType() == QueryNode.TYPE_LOCATION
                        || queryNode.getType() == QueryNode.TYPE_DEREF
                        || queryNode.getType() == QueryNode.TYPE_RELATION
                        || queryNode.getType() == QueryNode.TYPE_TEXTSEARCH
                        || queryNode.getType() == QueryNode.TYPE_PATH) {
                    createNodeTest(node, queryNode);
                } else if (queryNode.getType() == QueryNode.TYPE_ORDER) {
                    createOrderSpec(node, (OrderQueryNode) queryNode);
                } else {
                    // traverse
                    node.childrenAccept(this, queryNode);
                }
                break;
            case JJTELEMENTNAMEORWILDCARD:
                if (queryNode.getType() == QueryNode.TYPE_LOCATION) {
                    SimpleNode child = (SimpleNode) node.jjtGetChild(0);
                    if (child.getId() != JJTANYNAME) {
                        createNodeTest(child, queryNode);
                    }
                }
                break;
            case JJTTEXTTEST:
                if (queryNode.getType() == QueryNode.TYPE_LOCATION) {
                    LocationStepQueryNode loc = (LocationStepQueryNode) queryNode;
                    loc.setNameTest(JCR_XMLTEXT);
                }
                break;
            case JJTTYPENAME:
                if (queryNode.getType() == QueryNode.TYPE_LOCATION) {
                    LocationStepQueryNode loc = (LocationStepQueryNode) queryNode;
                    String ntName = ((SimpleNode) node.jjtGetChild(0)).getValue();
                    try {
                        QName nt = resolver.toQName(ntName);
                        NodeTypeQueryNode nodeType = factory.createNodeTypeQueryNode(loc, nt);
                        loc.addPredicate(nodeType);
                    } catch (NamespaceException e) {
                        exceptions.add(new InvalidQueryException("Not a valid name: " + ntName));
                    }
                    catch (RepositoryException e)
                    {
                        exceptions.add(new InvalidQueryException("Not a valid name: " + ntName));
                    }
                }
                break;
            case JJTOREXPR:
                NAryQueryNode parent = (NAryQueryNode) queryNode;
                QueryNode orQueryNode = factory.createOrQueryNode(parent);
                parent.addOperand(orQueryNode);
                // traverse
                node.childrenAccept(this, orQueryNode);
                break;
            case JJTANDEXPR:
                parent = (NAryQueryNode) queryNode;
                QueryNode andQueryNode = factory.createAndQueryNode(parent);
                parent.addOperand(andQueryNode);
                // traverse
                node.childrenAccept(this, andQueryNode);
                break;
            case JJTCOMPARISONEXPR:
                createExpression(node, (NAryQueryNode) queryNode);
                break;
            case JJTSTRINGLITERAL:
            case JJTDECIMALLITERAL:
            case JJTDOUBLELITERAL:
            case JJTINTEGERLITERAL:
                if (queryNode.getType() == QueryNode.TYPE_RELATION) {
                    assignValue(node, (RelationQueryNode) queryNode);
                } else if (queryNode.getType() == QueryNode.TYPE_LOCATION) {
                    if (node.getId() == JJTINTEGERLITERAL) {
                        int index = Integer.parseInt(node.getValue());
                        ((LocationStepQueryNode) queryNode).setIndex(index);
                    } else {
                        exceptions.add(new InvalidQueryException("LocationStep only allows integer literal as position index"));
                    }
                } else {
                    exceptions.add(new InvalidQueryException("Parse error: data is not a RelationQueryNode"));
                }
                break;
            case JJTUNARYMINUS:
                if (queryNode.getType() == QueryNode.TYPE_RELATION) {
                    ((RelationQueryNode) queryNode).setUnaryMinus(true);
                } else {
                    exceptions.add(new InvalidQueryException("Parse error: data is not a RelationQueryNode"));
                }
                break;
            case JJTFUNCTIONCALL:
                queryNode = createFunction(node, queryNode);
                break;
            case JJTORDERBYCLAUSE:
                root.setOrderNode(factory.createOrderQueryNode(root));
                queryNode = root.getOrderNode();
                node.childrenAccept(this, queryNode);
                break;
            case JJTORDERMODIFIER:
                if (node.jjtGetNumChildren() > 0
                        && ((SimpleNode) node.jjtGetChild(0)).getId() == JJTDESCENDING) {
                    OrderQueryNode.OrderSpec[] specs = ((OrderQueryNode) queryNode).getOrderSpecs();
                    specs[specs.length - 1].setAscending(false);
                }
                break;
            case JJTPREDICATELIST:
                if (queryNode.getType() == QueryNode.TYPE_PATH) {
                    // switch to last location
                    QueryNode[] operands = ((PathQueryNode) queryNode).getOperands();
                    queryNode = operands[operands.length - 1];
                }
                node.childrenAccept(this, queryNode);
                break;
            case JJTPREDICATE:
                if (queryNode.getType() == QueryNode.TYPE_LOCATION
                        || queryNode.getType() == QueryNode.TYPE_DEREF) {
                    node.childrenAccept(this, queryNode);
                } else {
                    // predicate not allowed here
                    exceptions.add(new InvalidQueryException("Unsupported location for predicate"));
                }
                break;
            case JJTDOTDOT:
                exceptions.add(new InvalidQueryException("Parent axis is not supported"));
                break;
            default:
                // per default traverse
                node.childrenAccept(this, queryNode);
        }
        return queryNode;
    }

    //----------------------< internal >----------------------------------------

    /**
     * Applies {@link #tmpRelPath} to <code>node</code> and reset the path to
     * <code>null</code>.
     *
     * @param node a relation query node.
     */
    private void applyRelativePath(RelationQueryNode node) {
        if (tmpRelPath != null) {
            Path relPath = tmpRelPath;
            for (int i = 0; i < relPath.depth(); i++) {
                node.addPathElement(relPath.getElements()[i]);
            }
            tmpRelPath = null;
        }
    }

    /**
     * Creates a <code>LocationStepQueryNode</code> at the current position
     * in parent.
     *
     * @param node   the current node in the xpath syntax tree.
     * @param parent the parent <code>PathQueryNode</code>.
     * @return the created <code>LocationStepQueryNode</code>.
     */
    private LocationStepQueryNode createLocationStep(SimpleNode node, NAryQueryNode parent) {
        LocationStepQueryNode queryNode = null;
        boolean descendant = false;
        Node p = node.jjtGetParent();
        for (int i = 0; i < p.jjtGetNumChildren(); i++) {
            SimpleNode c = (SimpleNode) p.jjtGetChild(i);
            if (c == node) {
                queryNode = factory.createLocationStepQueryNode(parent);
                queryNode.setNameTest(null);
                queryNode.setIncludeDescendants(descendant);
                parent.addOperand(queryNode);
                break;
            }
            descendant = (c.getId() == JJTSLASHSLASH
                    || c.getId() == JJTROOTDESCENDANTS);
        }

        node.childrenAccept(this, queryNode);

        return queryNode;
    }

    /**
     * Assigns a Name to one of the follwing QueryNodes:
     * {@link RelationQueryNode}, {@link DerefQueryNode}, {@link RelationQueryNode},
     * {@link PathQueryNode}, {@link OrderQueryNode}, {@link TextsearchQueryNode}.
     *
     * @param node      the current node in the xpath syntax tree.
     * @param queryNode the query node.
     */
    private void createNodeTest(SimpleNode node, QueryNode queryNode) {
        if (node.jjtGetNumChildren() > 0) {
            SimpleNode child = (SimpleNode) node.jjtGetChild(0);
            if (child.getId() == JJTQNAME || child.getId() == JJTQNAMEFORITEMTYPE) {
                try {
                    QName name = decode(resolver.toQName(child.getValue()));
                    if (queryNode.getType() == QueryNode.TYPE_LOCATION) {
                        if (name.equals(JCR_ROOT)) {
                            name = LocationStepQueryNode.EMPTY_NAME;
                        }
                        ((LocationStepQueryNode) queryNode).setNameTest(name);
                    } else if (queryNode.getType() == QueryNode.TYPE_DEREF) {
                        ((DerefQueryNode) queryNode).setRefProperty(name);
                    } else if (queryNode.getType() == QueryNode.TYPE_RELATION) {
                        QName element = name;
                        ((RelationQueryNode) queryNode).addPathElement(element);
                    } else if (queryNode.getType() == QueryNode.TYPE_PATH) {
                        root.addSelectProperty(name);
                    } else if (queryNode.getType() == QueryNode.TYPE_ORDER) {
                        root.getOrderNode().addOrderSpec(name, true);
                    } else if (queryNode.getType() == QueryNode.TYPE_TEXTSEARCH) {
                        TextsearchQueryNode ts = (TextsearchQueryNode) queryNode;
                        ts.addPathElement(name);
                        if (isAttributeNameTest(node)) {
                            ts.setReferencesProperty(true);
                        }
                    }
                } catch (RepositoryException e) {
                    exceptions.add(new InvalidQueryException("Illegal name: " + child.getValue()));
                }
            } else if (child.getId() == JJTSTAR) {
                if (queryNode.getType() == QueryNode.TYPE_LOCATION) {
                    ((LocationStepQueryNode) queryNode).setNameTest(null);
                } else if (queryNode.getType() == QueryNode.TYPE_RELATION) {
                    ((RelationQueryNode) queryNode).addPathElement(
                            RelationQueryNode.STAR_NAME_TEST);
                } else if (queryNode.getType() == QueryNode.TYPE_TEXTSEARCH) {
                    ((TextsearchQueryNode) queryNode).addPathElement(
                            RelationQueryNode.STAR_NAME_TEST);
                }
            } else {
                exceptions.add(new InvalidQueryException("Unsupported location for name test: " + child));
            }
        }
    }

    /**
     * Creates a new {@link org.apache.jackrabbit.spi.commons.query.RelationQueryNode}
     * with <code>queryNode</code> as its parent node.
     *
     * @param node      a comparison expression node.
     * @param queryNode the current <code>QueryNode</code>.
     */
    private void createExpression(SimpleNode node, NAryQueryNode queryNode) {
        if (node.getId() != JJTCOMPARISONEXPR) {
            throw new IllegalArgumentException("node must be of type ComparisonExpr");
        }
        // get operation type
        String opType = node.getValue();
        int type = 0;
        if (opType.equals(OP_EQ)) {
            type = RelationQueryNode.OPERATION_EQ_VALUE;
        } else if (opType.equals(OP_SIGN_EQ)) {
            type = RelationQueryNode.OPERATION_EQ_GENERAL;
        } else if (opType.equals(OP_GT)) {
            type = RelationQueryNode.OPERATION_GT_VALUE;
        } else if (opType.equals(OP_SIGN_GT)) {
            type = RelationQueryNode.OPERATION_GT_GENERAL;
        } else if (opType.equals(OP_GE)) {
            type = RelationQueryNode.OPERATION_GE_VALUE;
        } else if (opType.equals(OP_SIGN_GE)) {
            type = RelationQueryNode.OPERATION_GE_GENERAL;
        } else if (opType.equals(OP_LE)) {
            type = RelationQueryNode.OPERATION_LE_VALUE;
        } else if (opType.equals(OP_SIGN_LE)) {
            type = RelationQueryNode.OPERATION_LE_GENERAL;
        } else if (opType.equals(OP_LT)) {
            type = RelationQueryNode.OPERATION_LT_VALUE;
        } else if (opType.equals(OP_SIGN_LT)) {
            type = RelationQueryNode.OPERATION_LT_GENERAL;
        } else if (opType.equals(OP_NE)) {
            type = RelationQueryNode.OPERATION_NE_VALUE;
        } else if (opType.equals(OP_SIGN_NE)) {
            type = RelationQueryNode.OPERATION_NE_GENERAL;
        } else {
            exceptions.add(new InvalidQueryException("Unsupported ComparisonExpr type:" + node.getValue()));
        }

        final RelationQueryNode rqn = factory.createRelationQueryNode(queryNode, type);

        // traverse
        node.childrenAccept(this, rqn);

        // check if string transformation is valid
        try {
            rqn.acceptOperands(new DefaultQueryNodeVisitor() {
                public Object visit(PropertyFunctionQueryNode node, Object data) {
                    String functionName = node.getFunctionName();
                    if ((functionName.equals(PropertyFunctionQueryNode.LOWER_CASE)
                            || functionName.equals(PropertyFunctionQueryNode.UPPER_CASE))
                                && rqn.getValueType() != QueryConstants.TYPE_STRING) {
                        String msg = "Upper and lower case function are only supported with String literals";
                        exceptions.add(new InvalidQueryException(msg));
                    }
                    return data;
                }
            }, null);
        }
        catch (RepositoryException e) {
            exceptions.add(e);
        }

        queryNode.addOperand(rqn);
    }

    /**
     * Creates the primary path query node.
     *
     * @param node xpath node representing the root of the parsed tree.
     * @return the path qurey node
     */
    private PathQueryNode createPathQueryNode(SimpleNode node) {
        root.setLocationNode(factory.createPathQueryNode(root));
        node.childrenAccept(this, root.getLocationNode());
        return root.getLocationNode();
    }

    /**
     * Assigns a value to the <code>queryNode</code>.
     *
     * @param node      must be of type string, decimal, double or integer; otherwise
     *                  an InvalidQueryException is added to {@link #exceptions}.
     * @param queryNode current node in the query tree.
     */
    private void assignValue(SimpleNode node, RelationQueryNode queryNode) {
        if (node.getId() == JJTSTRINGLITERAL) {
            queryNode.setStringValue(unescapeQuotes(node.getValue()));
        } else if (node.getId() == JJTDECIMALLITERAL) {
            queryNode.setDoubleValue(Double.parseDouble(node.getValue()));
        } else if (node.getId() == JJTDOUBLELITERAL) {
            queryNode.setDoubleValue(Double.parseDouble(node.getValue()));
        } else if (node.getId() == JJTINTEGERLITERAL) {
            // if this is an expression that contains position() do not change
            // the type.
            if (queryNode.getValueType() == QueryConstants.TYPE_POSITION) {
                queryNode.setPositionValue(Integer.parseInt(node.getValue()));
            } else {
                queryNode.setLongValue(Long.parseLong(node.getValue()));
            }
        } else {
            exceptions.add(new InvalidQueryException("Unsupported literal type:" + node.toString()));
        }
    }

    /**
     * Creates a function based on <code>node</code>.
     *
     * @param node      the function node from the xpath tree.
     * @param queryNode the current query node.
     * @return the function node
     */
    private QueryNode createFunction(SimpleNode node, QueryNode queryNode) {
        // find out function name
        String tmp = ((SimpleNode) node.jjtGetChild(0)).getValue();
        String fName = tmp.substring(0, tmp.length() - 1);
        try {
            QName funName = resolver.toQName(fName);

            if (FN_NOT.equals(funName) || FN_NOT_10.equals(funName)) {
                if (queryNode instanceof NAryQueryNode) {
                    QueryNode not = factory.createNotQueryNode(queryNode);
                    ((NAryQueryNode) queryNode).addOperand(not);
                    // @todo is this needed?
                    queryNode = not;
                    // traverse
                    if (node.jjtGetNumChildren() == 2) {
                        node.jjtGetChild(1).jjtAccept(this, queryNode);
                    } else {
                        exceptions.add(new InvalidQueryException("fn:not only supports one expression argument"));
                    }
                } else {
                    exceptions.add(new InvalidQueryException("Unsupported location for function fn:not"));
                }
            } else if (XS_DATETIME.equals(funName)) {
                // check arguments
                if (node.jjtGetNumChildren() == 2) {
                    if (queryNode instanceof RelationQueryNode) {
                        RelationQueryNode rel = (RelationQueryNode) queryNode;
                        SimpleNode literal = (SimpleNode) node.jjtGetChild(1).jjtGetChild(0);
                        if (literal.getId() == JJTSTRINGLITERAL) {
                            String value = literal.getValue();
                            // strip quotes
                            value = value.substring(1, value.length() - 1);
                            Calendar c = ISO8601.parse(value);
                            if (c == null) {
                                exceptions.add(new InvalidQueryException("Unable to parse string literal for xs:dateTime: " + value));
                            } else {
                                rel.setDateValue(c.getTime());
                            }
                        } else {
                            exceptions.add(new InvalidQueryException("Wrong argument type for xs:dateTime"));
                        }
                    } else {
                        exceptions.add(new InvalidQueryException("Unsupported location for function xs:dateTime"));
                    }
                } else {
                    // wrong number of arguments
                    exceptions.add(new InvalidQueryException("Wrong number of arguments for xs:dateTime"));
                }
            } else if (JCR_CONTAINS.equals(funName)) {
                // check number of arguments
                if (node.jjtGetNumChildren() == 3) {
                    if (queryNode instanceof NAryQueryNode) {
                        SimpleNode literal = (SimpleNode) node.jjtGetChild(2).jjtGetChild(0);
                        if (literal.getId() == JJTSTRINGLITERAL) {
                            TextsearchQueryNode contains = factory.createTextsearchQueryNode(
                                    queryNode, unescapeQuotes(literal.getValue()));
                            // assign property name
                            SimpleNode path = (SimpleNode) node.jjtGetChild(1);
                            path.jjtAccept(this, contains);
                            ((NAryQueryNode) queryNode).addOperand(contains);
                        } else {
                            exceptions.add(new InvalidQueryException("Wrong argument type for jcr:contains"));
                        }
                    }
                } else {
                    // wrong number of arguments
                    exceptions.add(new InvalidQueryException("Wrong number of arguments for jcr:contains"));
                }
            } else if (JCR_LIKE.equals(funName)) {
                // check number of arguments
                if (node.jjtGetNumChildren() == 3) {
                    if (queryNode instanceof NAryQueryNode) {
                        RelationQueryNode like = factory.createRelationQueryNode(
                                queryNode, RelationQueryNode.OPERATION_LIKE);
                        ((NAryQueryNode) queryNode).addOperand(like);

                        // assign property name
                        node.jjtGetChild(1).jjtAccept(this, like);
                        // check property name
                        if (like.getRelativePath() == null) {
                            exceptions.add(new InvalidQueryException("Wrong first argument type for jcr:like"));
                        }

                        SimpleNode literal = (SimpleNode) node.jjtGetChild(2).jjtGetChild(0);
                        if (literal.getId() == JJTSTRINGLITERAL) {
                            like.setStringValue(unescapeQuotes(literal.getValue()));
                        } else {
                            exceptions.add(new InvalidQueryException("Wrong second argument type for jcr:like"));
                        }
                    } else {
                        exceptions.add(new InvalidQueryException("Unsupported location for function jcr:like"));
                    }
                } else {
                    // wrong number of arguments
                    exceptions.add(new InvalidQueryException("Wrong number of arguments for jcr:like"));
                }
            } else if (FN_TRUE.equals(funName)) {
                if (queryNode.getType() == QueryNode.TYPE_RELATION) {
                    RelationQueryNode rel = (RelationQueryNode) queryNode;
                    rel.setStringValue("true");
                } else {
                    exceptions.add(new InvalidQueryException("Unsupported location for true()"));
                }
            } else if (FN_FALSE.equals(funName)) {
                if (queryNode.getType() == QueryNode.TYPE_RELATION) {
                    RelationQueryNode rel = (RelationQueryNode) queryNode;
                    rel.setStringValue("false");
                } else {
                    exceptions.add(new InvalidQueryException("Unsupported location for false()"));
                }
            } else if (FN_POSITION.equals(funName)) {
                if (queryNode.getType() == QueryNode.TYPE_RELATION) {
                    RelationQueryNode rel = (RelationQueryNode) queryNode;
                    if (rel.getOperation() == RelationQueryNode.OPERATION_EQ_GENERAL) {
                        // set dummy value to set type of relation query node
                        // will be overwritten when the tree is furhter parsed.
                        rel.setPositionValue(1);
                        rel.addPathElement(FN_POSITION_FULL);
                    } else {
                        exceptions.add(new InvalidQueryException("Unsupported expression with position(). Only = is supported."));
                    }
                } else {
                    exceptions.add(new InvalidQueryException("Unsupported location for position()"));
                }
            } else if (FN_FIRST.equals(funName)) {
                if (queryNode.getType() == QueryNode.TYPE_RELATION) {
                    ((RelationQueryNode) queryNode).setPositionValue(1);
                } else if (queryNode.getType() == QueryNode.TYPE_LOCATION) {
                    ((LocationStepQueryNode) queryNode).setIndex(1);
                } else {
                    exceptions.add(new InvalidQueryException("Unsupported location for first()"));
                }
            } else if (FN_LAST.equals(funName)) {
                if (queryNode.getType() == QueryNode.TYPE_RELATION) {
                    ((RelationQueryNode) queryNode).setPositionValue(LocationStepQueryNode.LAST);
                } else if (queryNode.getType() == QueryNode.TYPE_LOCATION) {
                    ((LocationStepQueryNode) queryNode).setIndex(LocationStepQueryNode.LAST);
                } else {
                    exceptions.add(new InvalidQueryException("Unsupported location for last()"));
                }
            } else if (JCR_DEREF.equals(funName)) {
                // check number of arguments
                if (node.jjtGetNumChildren() == 3) {
                    boolean descendant = false;
                    if (queryNode.getType() == QueryNode.TYPE_LOCATION) {
                        LocationStepQueryNode loc = (LocationStepQueryNode) queryNode;
                        // remember if descendant axis
                        descendant = loc.getIncludeDescendants();
                        queryNode = loc.getParent();
                        ((NAryQueryNode) queryNode).removeOperand(loc);
                    }
                    if (queryNode.getType() == QueryNode.TYPE_PATH) {
                        PathQueryNode pathNode = (PathQueryNode) queryNode;
                        DerefQueryNode derefNode = factory.createDerefQueryNode(pathNode, null, false);

                        // assign property name
                        node.jjtGetChild(1).jjtAccept(this, derefNode);
                        // check property name
                        if (derefNode.getRefProperty() == null) {
                            exceptions.add(new InvalidQueryException("Wrong first argument type for jcr:deref"));
                        }

                        SimpleNode literal = (SimpleNode) node.jjtGetChild(2).jjtGetChild(0);
                        if (literal.getId() == JJTSTRINGLITERAL) {
                            String value = literal.getValue();
                            // strip quotes
                            value = value.substring(1, value.length() - 1);
                            if (!value.equals("*")) {
                                QName name = null;
                                try {
                                    name = decode(resolver.toQName(value));
                                } catch (NamespaceException e) {
                                    exceptions.add(new InvalidQueryException("Illegal name: " + value));
                                }
                                derefNode.setNameTest(name);
                            }
                        } else {
                            exceptions.add(new InvalidQueryException("Second argument for jcr:deref must be a String"));
                        }

                        // check if descendant
                        if (!descendant) {
                            Node p = node.jjtGetParent();
                            for (int i = 0; i < p.jjtGetNumChildren(); i++) {
                                SimpleNode c = (SimpleNode) p.jjtGetChild(i);
                                if (c == node) {
                                    break;
                                }
                                descendant = (c.getId() == JJTSLASHSLASH
                                        || c.getId() == JJTROOTDESCENDANTS);
                            }
                        }
                        derefNode.setIncludeDescendants(descendant);
                        pathNode.addPathStep(derefNode);
                    } else {
                        exceptions.add(new InvalidQueryException("Unsupported location for jcr:deref()"));
                    }
                }
            } else if (JCR_SCORE.equals(funName)) {
                if (queryNode.getType() == QueryNode.TYPE_ORDER) {
                    createOrderSpec(node, (OrderQueryNode) queryNode);
                } else {
                    exceptions.add(new InvalidQueryException("Unsupported location for jcr:score()"));
                }
            } else if (FN_LOWER_CASE.equals(funName)) {
                if (node.jjtGetNumChildren() == 2) {
                    if (queryNode.getType() == QueryNode.TYPE_RELATION) {
                        RelationQueryNode relNode = (RelationQueryNode) queryNode;
                        relNode.addOperand(factory.createPropertyFunctionQueryNode(
                                relNode, PropertyFunctionQueryNode.LOWER_CASE));
                        // get property name
                        node.jjtGetChild(1).jjtAccept(this, relNode);
                    } else {
                        exceptions.add(new InvalidQueryException("Unsupported location for fn:lower-case()"));
                    }
                } else {
                    exceptions.add(new InvalidQueryException("Wrong number of argument for fn:lower-case()"));
                }
            } else if (FN_UPPER_CASE.equals(funName)) {
                if (node.jjtGetNumChildren() == 2) {
                    if (queryNode.getType() == QueryNode.TYPE_RELATION) {
                        RelationQueryNode relNode = (RelationQueryNode) queryNode;
                        relNode.addOperand(factory.createPropertyFunctionQueryNode(
                                relNode, PropertyFunctionQueryNode.UPPER_CASE));
                        // get property name
                        node.jjtGetChild(1).jjtAccept(this, relNode);
                    } else {
                        exceptions.add(new InvalidQueryException("Unsupported location for fn:upper-case()"));
                    }
                } else {
                    exceptions.add(new InvalidQueryException("Unsupported location for fn:upper-case()"));
                }
            } else if (REP_SIMILAR.equals(funName)) {
                if (node.jjtGetNumChildren() == 3) {
                    if (queryNode instanceof NAryQueryNode) {
                        NAryQueryNode parent = (NAryQueryNode) queryNode;
                        RelationQueryNode rel = factory.createRelationQueryNode(
                                parent, RelationQueryNode.OPERATION_SIMILAR);
                        parent.addOperand(rel);
                        // assign path
                        node.jjtGetChild(1).jjtAccept(this, rel);

                        // get path string
                        node.jjtGetChild(2).jjtAccept(this, rel);
                        // check if string is set
                        if (rel.getStringValue() == null) {
                            exceptions.add(new InvalidQueryException(
                                    "Second argument for rep:similar() must be of type string"));
                        }
                    } else {
                        exceptions.add(new InvalidQueryException(
                                "Unsupported location for rep:similar()"));
                    }
                } else {
                    exceptions.add(new InvalidQueryException(
                            "Wrong number of arguments for rep:similar()"));
                }
            } else if (REP_SPELLCHECK.equals(funName)
                    && queryNode.getType() != QueryNode.TYPE_PATH) {
                if (node.jjtGetNumChildren() == 2) {
                    if (queryNode instanceof NAryQueryNode) {
                        NAryQueryNode parent = (NAryQueryNode) queryNode;
                        RelationQueryNode rel = factory.createRelationQueryNode(
                                parent, RelationQueryNode.OPERATION_SPELLCHECK);
                        parent.addOperand(rel);

                        // get string to check
                        node.jjtGetChild(1).jjtAccept(this, rel);
                        // check if string is set
                        if (rel.getStringValue() == null) {
                            exceptions.add(new InvalidQueryException(
                                    "Argument for rep:spellcheck() must be of type string"));
                        }

                        // set a dummy property name
                        rel.addPathElement(JCRConstants.Q_JCR_PRIMARYTYPE);
                    } else {
                        exceptions.add(new InvalidQueryException(
                                "Unsupported location for rep:spellcheck()"));
                    }
                } else {
                    exceptions.add(new InvalidQueryException(
                            "Wrong number of arguments for rep:spellcheck()"));
                }
            } else if (queryNode.getType() == QueryNode.TYPE_RELATION) {
                // use function name as name of a pseudo property in a relation
                try {
                    QName name = resolver.toQName(fName + "()");
//                    Path.Element element = PATH_FACTORY.createElement(name);
                    RelationQueryNode relNode = (RelationQueryNode) queryNode;
                    relNode.addPathElement(name);
                } catch (NamespaceException e) {
                    exceptions.add(e);
                }
            } else if (queryNode.getType() == QueryNode.TYPE_PATH) {
                // use function name as name of a pseudo property in select clause
                try {
                    QName name = resolver.toQName(fName + "()");
                    root.addSelectProperty(name);
                } catch (NamespaceException e) {
                    exceptions.add(e);
                }
            } else {
                exceptions.add(new InvalidQueryException("Unsupported function: " + fName));
            }
        } catch (NamespaceException e) {
            exceptions.add(e);
        }
        catch (RepositoryException e)
        {
            exceptions.add(e);
        }
        return queryNode;
    }

    private OrderQueryNode.OrderSpec createOrderSpec(SimpleNode node,
                                                     OrderQueryNode queryNode) {
        SimpleNode child = (SimpleNode) node.jjtGetChild(0);
        OrderQueryNode.OrderSpec spec = null;
        try {
            String propName = child.getValue();
            if (child.getId() == JJTQNAMELPAR) {
                // function name
                // cut off left parenthesis at end
                propName = propName.substring(0, propName.length() - 1);
            }
            QName name = decode(resolver.toQName(propName));
            spec = new OrderQueryNode.OrderSpec(name, true);
            queryNode.addOrderSpec(spec);
        } catch (NamespaceException e) {
            exceptions.add(new InvalidQueryException("Illegal name: " + child.getValue()));
        }
        catch (RepositoryException e)
        {
            exceptions.add(new InvalidQueryException("Illegal name: " + child.getValue()));
        }
        return spec;
    }

    /**
     * Returns true if <code>node</code> has a child node which is the attribute
     * axis.
     *
     * @param node a node with type {@link #JJTSTEPEXPR}.
     * @return <code>true</code> if this step expression uses the attribute axis.
     */
    private boolean isAttributeAxis(SimpleNode node) {
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            if (((SimpleNode) node.jjtGetChild(i)).getId() == JJTAT) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns <code>true</code> if the NodeTest <code>node</code> is an
     * attribute name test.
     * Example:
     * <pre>
     * StepExpr
     *     At @
     *     NodeTest
     *         NameTest
     *             Name foo
     * </pre>
     * @param node a node with type {@link #JJTNAMETEST}.
     * @return <code>true</code> if the name test <code>node</code> is on the
     * attribute axis.
     */
    private boolean isAttributeNameTest(SimpleNode node) {
        SimpleNode stepExpr = (SimpleNode) node.jjtGetParent().jjtGetParent();
        if (stepExpr.getId() == JJTSTEPEXPR) {
            return ((SimpleNode) stepExpr.jjtGetChild(0)).getId() == JJTAT;
        }
        return false;
    }

    /**
     * Unescapes single or double quotes depending on how <code>literal</code>
     * is enclosed and strips enclosing quotes.
     *
     * </p>
     * Examples:</br>
     * <code>"foo""bar"</code> -&gt; <code>foo"bar</code></br>
     * <code>'foo''bar'</code> -&gt; <code>foo'bar</code></br>
     * but:</br>
     * <code>'foo""bar'</code> -&gt; <code>foo""bar</code>
     *
     * @param literal the string literal to unescape
     * @return the unescaped and stripped literal.
     */
    private String unescapeQuotes(String literal) {
        String value = literal.substring(1, literal.length() - 1);
        if (value.length() == 0) {
            // empty string
            return value;
        }
        if (literal.charAt(0) == '"') {
            value = value.replaceAll("\"\"", "\"");
        } else {
            value = value.replaceAll("''", "'");
        }
        return value;
    }

    private static QName decode(QName name) {
        String decodedLN = XMLUtils.decode(name.getLocalPart());
        if (decodedLN.equals(name.getLocalPart())) {
            return name;
        } else {
            return new QName(name.getNamespaceURI(), decodedLN);
        }
    }
}
