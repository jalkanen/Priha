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
package org.apache.jackrabbit.test.api;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

/**
 * <code>AbstractWorkspaceCopyCloneBetweenWorkspacesTest</code> is the abstract
 * base class for all copying and cloning related test classes between
 * workspaces.
 */
abstract class AbstractWorkspaceCopyBetweenTest extends AbstractWorkspaceCopyTest {

    /**
     * The superuser session for the non default workspace
     */
    protected Session superuserW2;

    /**
     * A read-write session for the non default workspace
     */
    protected Session rwSessionW2;

    /**
     * The workspace in the non default session.
     */
    Workspace workspaceW2;

    /**
     * The testroot node in the non default session
     */
    Node testRootNodeW2;

    /**
     * A referenceable node in default workspace
     */
    protected Node node1W2;

    /**
     * A non-referenceable node in default workspace
     */
    protected Node node2W2;

    protected void setUp() throws Exception {
        super.setUp();

        // init second workspace
        superuserW2 = helper.getSuperuserSession(workspaceName);
        rwSessionW2 = helper.getReadWriteSession(workspaceName);
        workspaceW2 = superuserW2.getWorkspace();

        initNodesW2();
    }

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
                            Node n = children.nextNode();
                            n.remove();
                        }
                        superuserW2.save();
                    }
                }
            } finally {
                superuserW2.logout();
            }
        }
        if (rwSessionW2 != null) {
            rwSessionW2.logout();
        }
        super.tearDown();
    }


    private void initNodesW2() throws RepositoryException {

        // testroot
        if (superuserW2.getRootNode().hasNode(testPath)) {
            testRootNodeW2 = superuserW2.getRootNode().getNode(testPath);
        } else {
            testRootNodeW2 = superuserW2.getRootNode().addNode(testPath, testNodeType);
            superuserW2.save();
        }

        // some test nodes
        superuserW2.getWorkspace().copy(workspace.getName(), node1.getPath(), node1.getPath());
        node1W2 = testRootNodeW2.getNode(node1.getName());

        superuserW2.getWorkspace().copy(workspace.getName(), node2.getPath(), node2.getPath());
        node2W2 = testRootNodeW2.getNode(node2.getName());

        testRootNodeW2.save();
    }
}
