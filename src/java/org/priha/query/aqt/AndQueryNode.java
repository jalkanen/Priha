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

import javax.jcr.RepositoryException;

/**
 * Implements a query node that defines an AND operation between arbitrary
 * other {@link QueryNode}s.
 */
public class AndQueryNode extends NAryQueryNode {

    /**
     * Creates a new <code>AndQueryNode</code> with a <code>parent</code>
     * query node.
     *
     * @param parent the parent of <code>this</code> <code>AndQueryNode</code>.
     */
    protected AndQueryNode(QueryNode parent) {
        super(parent);
    }

    /**
     * This method can return <code>null</code> to indicate that this
     * <code>AndQueryNode</code> does not contain any operands.
     * {@inheritDoc}
     * @throws RepositoryException
     */
    public Object accept(QueryNodeVisitor visitor, Object data) throws RepositoryException {
        return visitor.visit(this, data);
    }

    /**
     * Returns the type of this node.
     * @return the type of this node.
     */
    public int getType() {
        return QueryNode.TYPE_AND;
    }

    /**
     * @inheritDoc
     */
    public boolean equals(Object obj) {
        if (obj instanceof AndQueryNode) {
            return super.equals(obj);
        }
        return false;
    }
}
