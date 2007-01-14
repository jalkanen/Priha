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

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

/**
 * <code>WorkspaceCopyBetweenWorkspacesSameNameSibsTest</code> contains tests
 * for copying nodes as same name siblings between workspace.
 *
 * @test
 * @sources WorkspaceCopyBetweenWorkspacesSameNameSibsTest.java
 * @executeClass org.apache.jackrabbit.test.api.WorkspaceCopyBetweenWorkspacesSameNameSibsTest
 * @keywords level2
 */
public class WorkspaceCopyBetweenWorkspacesSameNameSibsTest extends AbstractWorkspaceSameNameSibsTest {

    /**
     * If ordering is supported by the node type of the parent node of the new
     * location, then the newly moved node is appended to the end of the child
     * node list.
     */
    public void testCopyNodesOrderingSupportedByParent() throws RepositoryException {
        // test assumes that repositry supports Orderable Child Node Support (optional)
        String[] orderList = {nodeName1, nodeName2, nodeName3};

        // copy node three times below a node and check the order
        for (int i = 0; i < orderList.length; i++) {
            workspaceW2.copy(workspace.getName(), node1.getPath(), node2.getPath() + "/" + orderList[i]);
        }

        // check regarding orderList if nodes are added at the end
        int cnt = 0;
        NodeIterator iter = node2.getNodes();
        while (iter.hasNext()) {
            Node n = (Node) iter.nextNode();

            assertTrue(n.getName().equals(orderList[cnt]));
            cnt++;
        }
    }

    /**
     * An ItemExistsException is thrown if a node or property already exists at
     * destAbsPath.
     * @tck.config sameNameSibsFalseNodeType name of a node type that does not
     * allows same name siblings.
     * @tck.config nodeName3 name of a child node that does not allow same name
     * siblings..
     */
    public void testCopyNodesNodeExistsAtDestPath() throws RepositoryException {
        // create a parent node where allowSameNameSiblings are set to false
        Node snsfNode = testRootNodeW2.addNode(nodeName3, sameNameSibsFalseNodeType.getName());
        testRootNodeW2.save();

        String dstAbsPath = snsfNode.getPath() + "/" + node1.getName();
        workspaceW2.copy(node1.getPath(), dstAbsPath);

        // try to copy again the node to same destAbsPath
        // property already exist
        try {
            workspaceW2.copy(workspace.getName(), node1.getPath(), dstAbsPath);
            fail("Node exists below '" + dstAbsPath + "'. Test should fail.");
        } catch (ItemExistsException e) {
            // successful
        }
    }
}