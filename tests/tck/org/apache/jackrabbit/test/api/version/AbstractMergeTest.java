/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.test.api.version;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * <code>AbstractMergeTest</code> is the abstract base class for all merge
 * related test classes.
 */
public abstract class AbstractMergeTest extends AbstractJCRTest {

    private static final String PROP_VERSIONABLE_NODE_TYPE = "versionableNodeType";

    // global variable used in different tests

    protected String versionableNodeType;
    protected String nonVersionableNodeType;

    /**
     * The superuser session for the second workspace
     */
    protected Session superuserW2;

    /**
     * The default workspace
     */
    protected Workspace workspace;

    /**
     * The second workspace
     */
    protected Workspace workspaceW2;

    /**
     * The test root node in second workspace to test
     */
    protected Node testRootNodeW2;

    /**
     * The modified string to check
     */
    protected static final String CHANGED_STRING = "changed";

    /**
     * Initialising used variables coming from the properties file.<br> Setup
     * some nodes on the 2 workspaces.<br>
     */
    protected void setUp() throws Exception {
        super.setUp();

        NodeTypeManager ntm = superuser.getWorkspace().getNodeTypeManager();

        // versionable node type
        versionableNodeType = getProperty(PROP_VERSIONABLE_NODE_TYPE);
        if (versionableNodeType == null) {
            fail("Property '" + PROP_VERSIONABLE_NODE_TYPE + "' is not defined.");
        }

        NodeType vNt = ntm.getNodeType(versionableNodeType);
        if (!vNt.isNodeType(mixVersionable)) {
            fail("Property '" + PROP_VERSIONABLE_NODE_TYPE + "' does not define a versionable nodetype.");
        }

        // non versionable node type
        // test node type defines always a non versionable node type
        nonVersionableNodeType = testNodeType;
        if (nonVersionableNodeType == null) {
            fail("Property '" + testNodeType + "' is not defined.");
        }

        NodeType nvNt = ntm.getNodeType(nonVersionableNodeType);
        if (nvNt.isNodeType(mixVersionable)) {
            fail("Property '" + testNodeType + "' does define a versionable nodetype.");
        }

        // initialise a new session on second workspace as superuser
        superuserW2 = helper.getSuperuserSession(workspaceName);

        workspace = superuser.getWorkspace();
        workspaceW2 = superuserW2.getWorkspace();

        // get/create test root node on second workspace
        if (testPath.length() == 0) {
            // test root is the root node
            testRootNodeW2 = superuserW2.getRootNode();
        } else if (!superuserW2.getRootNode().hasNode(testPath)) {
            testRootNodeW2 = superuserW2.getRootNode().addNode(testPath, testNodeType);
        } else {
            testRootNodeW2 = superuserW2.getRootNode().getNode(testPath);
        }

        // initialize test nodes
        initNodes();
    }

    /**
     * Tidy the testRootNodes of both workspaces, then logout sessions
     *
     * @throws Exception
     */
    protected void tearDown() throws Exception {
        // remove all test nodes in second workspace
        if (superuserW2 != null) {
            try {
                if (!isReadOnly) {
                    // do a 'rollback'
                    superuserW2.refresh(false);
                    Node rootW2 = superuserW2.getRootNode();
                    if (rootW2.hasNode(testPath)) {
                        // clean test root
                        testRootNodeW2 = rootW2.getNode(testPath);
                        for (NodeIterator children = testRootNodeW2.getNodes(); children.hasNext();) {
                            children.nextNode().remove();
                        }
                        rootW2.save();
                    }
                }
            } finally {
                superuserW2.logout();
            }
        }

        super.tearDown();
    }

    // initialize nodes
    abstract void initNodes() throws RepositoryException;
}