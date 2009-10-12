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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;

import org.priha.core.namespace.NamespaceMapper;
import org.priha.path.Path;
import org.priha.query.aqt.*;
import org.priha.util.ISO8601;
import org.priha.util.QName;
import org.priha.xml.XMLUtils;


/**
 * Implements the query node tree serialization into a String.
 */
class QueryFormat implements QueryNodeVisitor, QueryConstants {

    /**
     * Will be used to resolve QNames
     */
    private final NamespaceMapper resolver;

    /**
     * The String representation of the query node tree
     */
    private final String statement;

    /**
     * List of exception objects created while creating the XPath string
     */
    private final List<RepositoryException> exceptions = new ArrayList<RepositoryException>();

    private QueryFormat(QueryRootNode root, NamespaceMapper resolver)
            throws RepositoryException {
        this.resolver = resolver;
        statement = root.accept(this, new StringBuffer()).toString();
        if (exceptions.size() > 0) {
            Exception e = (Exception) exceptions.get(0);
            throw new InvalidQueryException(e.getMessage(), e);
        }
    }

    /**
     * Creates a XPath <code>String</code> representation of the QueryNode tree
     * argument <code>root</code>.
     *
     * @param root     the query node tree.
     * @param resolver to resolve QNames.
     * @return the XPath string representation of the QueryNode tree.
     * @throws InvalidQueryException the query node tree cannot be represented
     *                               as a XPath <code>String</code>.
     */
    public static String toString(QueryRootNode root, NamespaceMapper resolver)
            throws InvalidQueryException {
        try {
            return new QueryFormat(root, resolver).toString();
        }
        catch (RepositoryException e) {
            throw new InvalidQueryException(e);
        }
    }

    /**
     * Returns the string representation.
     *
     * @return the string representation.
     */
    public String toString() {
        return statement;
    }

    //-------------< QueryNodeVisitor interface >-------------------------------

    public Object visit(QueryRootNode node, Object data) throws RepositoryException {
        StringBuffer sb = (StringBuffer) data;
        node.getLocationNode().accept(this, data);
        if (node.getOrderNode() != null) {
            node.getOrderNode().accept(this, data);
        }
        QName[] selectProps = node.getSelectProperties();
        if (selectProps.length > 0) {
            sb.append('/');
            boolean union = selectProps.length > 1;
            if (union) {
                sb.append('(');
            }
            String pipe = "";
            for (int i = 0; i < selectProps.length; i++) {
                try {
                    sb.append(pipe);
                    sb.append('@');
                    sb.append(resolver.fromQName(encode(selectProps[i])));
                    pipe = "|";
                } catch (NamespaceException e) {
                    exceptions.add(e);
                }
            }
            if (union) {
                sb.append(')');
            }
        }
        return data;
    }

    public Object visit(OrQueryNode node, Object data) throws RepositoryException {
        StringBuffer sb = (StringBuffer) data;
        boolean bracket = false;
        if (node.getParent() instanceof AndQueryNode) {
            bracket = true;
        }
        if (bracket) {
            sb.append("(");
        }
        String or = "";
        QueryNode[] operands = node.getOperands();
        for (int i = 0; i < operands.length; i++) {
            sb.append(or);
            operands[i].accept(this, sb);
            or = " or ";
        }
        if (bracket) {
            sb.append(")");
        }
        return sb;
    }

    public Object visit(AndQueryNode node, Object data) throws RepositoryException {
        StringBuffer sb = (StringBuffer) data;
        String and = "";
        QueryNode[] operands = node.getOperands();
        for (int i = 0; i < operands.length; i++) {
            sb.append(and);
            operands[i].accept(this, sb);
            and = " and ";
        }
        return sb;
    }

    public Object visit(NotQueryNode node, Object data) throws RepositoryException {
        StringBuffer sb = (StringBuffer) data;
        QueryNode[] operands = node.getOperands();
        if (operands.length > 0) {
            try {
                sb.append(resolver.fromQName(XPathQueryBuilder.FN_NOT_10));
                sb.append("(");
                operands[0].accept(this, sb);
                sb.append(")");
            } catch (NamespaceException e) {
                exceptions.add(e);
            }
        }
        return sb;
    }

    public Object visit(ExactQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        sb.append("@");
        try {
            QName name = encode(node.getPropertyName());
            sb.append(resolver.fromQName(name));
            sb.append("='");
            sb.append(resolver.fromQName(node.getValue()));
        } catch (NamespaceException e) {
            exceptions.add(e);
        }
        sb.append("'");
        return sb;
    }

    public Object visit(NodeTypeQueryNode node, Object data) {
        // handled in location step visit
        return data;
    }

    public Object visit(TextsearchQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        try {
            sb.append(resolver.fromQName(XPathQueryBuilder.JCR_CONTAINS));
            sb.append("(");
            Path relPath = node.getRelativePath();
            if (relPath == null) {
                sb.append(".");
            } else {
                QName[] elements = relPath.getElements();
                String slash = "";
                for (int i = 0; i < elements.length; i++) {
                    sb.append(slash);
                    slash = "/";
                    if (node.getReferencesProperty() && i == elements.length - 1) {
                        sb.append("@");
                    }
                    if (elements[i].equals(RelationQueryNode.STAR_NAME_TEST)) {
                        sb.append("*");
                    } else {
                        QName n = encode(elements[i]);
                        sb.append(resolver.fromQName(n));
                    }
//                    if (elements[i].getIndex() != 0) {
//                        sb.append("[").append(elements[i].getIndex()).append("]");
//                    }
                }
            }
            sb.append(", '");
            sb.append(node.getQuery().replaceAll("'", "''"));
            sb.append("')");
        } catch (NamespaceException e) {
            exceptions.add(e);
        }
        return sb;
    }

    public Object visit(PathQueryNode node, Object data) throws RepositoryException {
        StringBuffer sb = (StringBuffer) data;
        if (node.isAbsolute()) {
            sb.append("/");
        }
        LocationStepQueryNode[] steps = node.getPathSteps();
        String slash = "";
        for (int i = 0; i < steps.length; i++) {
            sb.append(slash);
            steps[i].accept(this, sb);
            slash = "/";
        }
        return sb;
    }

    public Object visit(LocationStepQueryNode node, Object data) throws RepositoryException {
        StringBuffer sb = (StringBuffer) data;
        if (node.getIncludeDescendants()) {
            sb.append('/');
        }
        final QName[] nodeType = new QName[1];
        node.acceptOperands(new DefaultQueryNodeVisitor() {
            public Object visit(NodeTypeQueryNode node, Object data) {
                nodeType[0] = node.getValue();
                return data;
            }
        }, null);

        if (nodeType[0] != null) {
            sb.append("element(");
        }

        if (node.getNameTest() == null) {
            sb.append("*");
        } else {
            try {
                if (node.getNameTest().getLocalPart().length() == 0) {
                    sb.append(resolver.fromQName(XPathQueryBuilder.JCR_ROOT));
                } else {
                    sb.append(resolver.fromQName(encode(node.getNameTest())));
                }
            } catch (NamespaceException e) {
                exceptions.add(e);
            }
        }

        if (nodeType[0] != null) {
            sb.append(", ");
            try {
                sb.append(resolver.fromQName(encode(nodeType[0])));
            } catch (NamespaceException e) {
                exceptions.add(e);
            }
            sb.append(")");
        }

        if (node.getIndex() != LocationStepQueryNode.NONE) {
            sb.append('[').append(node.getIndex()).append(']');
        }
        QueryNode[] predicates = node.getPredicates();
        for (int i = 0; i < predicates.length; i++) {
            // ignore node type query nodes
            if (predicates[i].getType() == QueryNode.TYPE_NODETYPE) {
                continue;
            }
            sb.append('[');
            predicates[i].accept(this, sb);
            sb.append(']');
        }
        return sb;
    }

    public Object visit(DerefQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        try {
            sb.append(resolver.fromQName(XPathQueryBuilder.JCR_DEREF));
            sb.append("(@");
            sb.append(resolver.fromQName(encode(node.getRefProperty())));
            sb.append(", '");
            if (node.getNameTest() == null) {
                sb.append("*");
            } else {
                sb.append(resolver.fromQName(encode(node.getNameTest())));
            }
            sb.append("')");
        } catch (NamespaceException e) {
            exceptions.add(e);
        }
        return sb;
    }

    public Object visit(RelationQueryNode node, Object data) throws RepositoryException {
        StringBuffer sb = (StringBuffer) data;
        try {

            StringBuffer propPath = new StringBuffer();
            // only encode if not position function
            Path relPath = node.getRelativePath();
            if (relPath == null) {
                propPath.append(".");
            } else if (relPath.getLastComponent().equals(XPathQueryBuilder.FN_POSITION_FULL)) {
                propPath.append(resolver.fromQName(XPathQueryBuilder.FN_POSITION_FULL));
            } else {
                QName[] elements = relPath.getElements();
                String slash = "";
                for (int i = 0; i < elements.length; i++) {
                    propPath.append(slash);
                    slash = "/";
                    if (i == elements.length - 1 && node.getOperation() != OPERATION_SIMILAR) {
                        propPath.append("@");
                    }
                    if (elements[i].equals(RelationQueryNode.STAR_NAME_TEST)) {
                        propPath.append("*");
                    } else {
                        propPath.append(resolver.fromQName(encode(elements[i])));
                    }
//                    if (elements[i].getIndex() != 0) {
//                        propPath.append("[").append(elements[i].getIndex()).append("]");
//                    }
                }
            }

            // surround name with property function
            node.acceptOperands(this, propPath);

            if (node.getOperation() == OPERATION_EQ_VALUE) {
                sb.append(propPath).append(" eq ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_EQ_GENERAL) {
                sb.append(propPath).append(" = ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_GE_GENERAL) {
                sb.append(propPath).append(" >= ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_GE_VALUE) {
                sb.append(propPath).append(" ge ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_GT_GENERAL) {
                sb.append(propPath).append(" > ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_GT_VALUE) {
                sb.append(propPath).append(" gt ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_LE_GENERAL) {
                sb.append(propPath).append(" <= ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_LE_VALUE) {
                sb.append(propPath).append(" le ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_LIKE) {
                sb.append(resolver.fromQName(XPathQueryBuilder.JCR_LIKE));
                sb.append("(").append(propPath).append(", ");
                appendValue(node, sb);
                sb.append(")");
            } else if (node.getOperation() == OPERATION_LT_GENERAL) {
                sb.append(propPath).append(" < ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_LT_VALUE) {
                sb.append(propPath).append(" lt ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_NE_GENERAL) {
                sb.append(propPath).append(" != ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_NE_VALUE) {
                sb.append(propPath).append(" ne ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_NULL) {
                sb.append(resolver.fromQName(XPathQueryBuilder.FN_NOT));
                sb.append("(").append(propPath).append(")");
            } else if (node.getOperation() == OPERATION_NOT_NULL) {
                sb.append(propPath);
            } else if (node.getOperation() == OPERATION_SIMILAR) {
                sb.append(resolver.fromQName(XPathQueryBuilder.REP_SIMILAR));
                sb.append("(").append(propPath).append(", ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_SPELLCHECK) {
                sb.append(resolver.fromQName(XPathQueryBuilder.REP_SPELLCHECK));
                sb.append("(");
                appendValue(node, sb);
                sb.append(")");
            } else {
                exceptions.add(new InvalidQueryException("Invalid operation: " + node.getOperation()));
            }
        } catch (NamespaceException e) {
            exceptions.add(e);
        }
        return sb;
    }

    public Object visit(OrderQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        sb.append(" order by");
        OrderQueryNode.OrderSpec[] specs = node.getOrderSpecs();
        String comma = "";
        try {
            for (int i = 0; i < specs.length; i++) {
                sb.append(comma);
                QName prop = encode(specs[i].getProperty());
                sb.append(" @");
                sb.append(resolver.fromQName(prop));
                if (!specs[i].isAscending()) {
                    sb.append(" descending");
                }
                comma = ",";
            }
        } catch (NamespaceException e) {
            exceptions.add(e);
        }
        return data;
    }

    public Object visit(PropertyFunctionQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        String functionName = node.getFunctionName();
        try {
            if (functionName.equals(PropertyFunctionQueryNode.LOWER_CASE)) {
                sb.insert(0, resolver.fromQName(XPathQueryBuilder.FN_LOWER_CASE) + "(");
                sb.append(")");
            } else if (functionName.equals(PropertyFunctionQueryNode.UPPER_CASE)) {
                sb.insert(0, resolver.fromQName(XPathQueryBuilder.FN_UPPER_CASE) + "(");
                sb.append(")");
            } else {
                exceptions.add(new InvalidQueryException("Unsupported function: " + functionName));
            }
        } catch (NamespaceException e) {
            exceptions.add(e);
        }
        return sb;
    }

    //----------------------------< internal >----------------------------------

    /**
     * Appends the value of a relation node to the <code>StringBuffer</code>
     * <code>sb</code>.
     *
     * @param node the relation node.
     * @param b    where to append the value.
     * @throws NamespaceException if a prefix declaration is missing for
     *                                   a namespace URI.
     */
    private void appendValue(RelationQueryNode node, StringBuffer b)
            throws NamespaceException {
        if (node.getValueType() == TYPE_LONG) {
            b.append(node.getLongValue());
        } else if (node.getValueType() == TYPE_DOUBLE) {
            b.append(node.getDoubleValue());
        } else if (node.getValueType() == TYPE_STRING) {
            b.append("'").append(node.getStringValue().replaceAll("'", "''")).append("'");
        } else if (node.getValueType() == TYPE_DATE || node.getValueType() == TYPE_TIMESTAMP) {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.setTime(node.getDateValue());
            b.append(resolver.fromQName(XPathQueryBuilder.XS_DATETIME));
            b.append("('").append(ISO8601.format(cal)).append("')");
        } else if (node.getValueType() == TYPE_POSITION) {
            if (node.getPositionValue() == LocationStepQueryNode.LAST) {
                b.append("last()");
            } else {
                b.append(node.getPositionValue());
            }
        } else {
            exceptions.add(new InvalidQueryException("Invalid type: " + node.getValueType()));
        }
    }

    private static QName encode(QName name) {
        String encoded = XMLUtils.encode(name.getLocalPart());
        if (encoded.equals(name.getLocalPart())) {
            return name;
        } else {
            return new QName(name.getNamespaceURI(), encoded);
        }
    }
}
