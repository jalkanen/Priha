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
package org.priha.query.aqt;

import java.util.List;

import javax.jcr.RepositoryException;

import org.priha.core.JCRConstants;
import org.priha.util.QName;


/**
 * Implements a query node that defines a path restriction.
 */
public class PathQueryNode extends NAryQueryNode {

    /**
     * Flag indicating whether this path is absolute.
     */
    private boolean absolute = false;

    /**
     * List of valid node type names under /jcr:system
     */
    private final List<QName> validJcrSystemNodeTypeNames;

    /**
     * Empty step node array.
     */
    private static final LocationStepQueryNode[] EMPTY = new LocationStepQueryNode[0];

    /**
     * Creates a relative <code>PathQueryNode</code> with no location steps and
     * the list of node types under /jcr:system.
     *
     * @param parent the parent query node.
     */
    protected PathQueryNode(QueryNode parent, List<QName> validJcrSystemNodeTypeNames) {
        super(parent);
        this.validJcrSystemNodeTypeNames = validJcrSystemNodeTypeNames;
    }

    /**
     * Returns a list of valid node types under /jcr:system. List&lt;Name>.
     *
     * @return a list of valid node types under /jcr:system.
     */
    public List<QName> getValidJcrSystemNodeTypeNames() {
        return validJcrSystemNodeTypeNames;
    }

    /**
     * {@inheritDoc}
     * @throws RepositoryException
     */
    public Object accept(QueryNodeVisitor visitor, Object data) throws RepositoryException {
        return visitor.visit(this, data);
    }

    /**
     * Returns the type of this node.
     *
     * @return the type of this node.
     */
    public int getType() {
        return QueryNode.TYPE_PATH;
    }

    /**
     * Adds a path step to this <code>PathQueryNode</code>.
     *
     * @param step the step to add.
     */
    public void addPathStep(LocationStepQueryNode step) {
        addOperand(step);
    }

    /**
     * Returns an array of all currently set location step nodes.
     *
     * @return an array of all currently set location step nodes.
     */
    public LocationStepQueryNode[] getPathSteps() {
        if (operands == null) {
            return EMPTY;
        } else {
            return (LocationStepQueryNode[]) operands.toArray(new LocationStepQueryNode[operands.size()]);
        }
    }

    /**
     * If <code>absolute</code> is <code>true</code> sets this
     * <code>PathQueryNode</code> to an absolute path. If <code>absolute</code>
     * is <code>false</code> this path is considered relative.
     *
     * @param absolute sets the absolute property to this new value.
     */
    public void setAbsolute(boolean absolute) {
        this.absolute = absolute;
    }

    /**
     * Returns <code>true</code> if this is an absolute path; <code>false</code>
     * otherwise.
     *
     * @return <code>true</code> if this is an absolute path; <code>false</code>
     *         otherwise.
     */
    public boolean isAbsolute() {
        return absolute;
    }

    /**
     * @inheritDoc
     */
    public boolean equals(Object obj) {
        if (obj instanceof PathQueryNode) {
            PathQueryNode other = (PathQueryNode) obj;
            return super.equals(obj) && absolute == other.absolute;
        }
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean needsSystemTree() {

        LocationStepQueryNode[] pathSteps = getPathSteps();
        if (pathSteps == null || pathSteps.length == 0) {
            return true;
        }

        QName firstPathStepName = pathSteps[0].getNameTest();
        if (firstPathStepName == null) {
            // If the first operand of the path steps is a node type query
            // we do not need to include the system index if the node type is
            // none of the node types that may occur in the system index.
            QueryNode[] pathStepOperands = pathSteps[0].getOperands();
            if (pathStepOperands.length > 0) {
                if (pathStepOperands[0] instanceof NodeTypeQueryNode) {
                    NodeTypeQueryNode nodeTypeQueryNode = (NodeTypeQueryNode) pathStepOperands[0];
                    if (!validJcrSystemNodeTypeNames.contains(nodeTypeQueryNode.getValue())) {
                        return false;
                    }
                }
            }
            // If the first location step has a null name test we need to include
            // the system tree ("*")
            return true;
        }

        // Calculate the first workspace relative location step
        LocationStepQueryNode firstWorkspaceRelativeStep = pathSteps[0];
        if (firstPathStepName.equals(JCRConstants.Q_JCR_ROOT)) {
            // path starts with "/jcr:root"
            if (pathSteps.length > 1) {
                firstWorkspaceRelativeStep = pathSteps[1];
            }
        }

        // First path step starts with "//"
        if (firstWorkspaceRelativeStep.getIncludeDescendants()) {
            return true;
        }

        // If the first workspace relative location step is jcr:system we need
        // to include the system tree
        QName firstWorkspaceRelativeName = firstWorkspaceRelativeStep.getNameTest();
        if (firstWorkspaceRelativeName == null
                || firstWorkspaceRelativeName.equals(JCRConstants.Q_JCR_SYSTEM)) {
            return true;
        }

        return super.needsSystemTree();
    }
}
