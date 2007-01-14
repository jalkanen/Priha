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

import javax.jcr.RepositoryException;
import javax.jcr.version.VersionException;


/**
 * <code>WorkspaceMoveVersionableTest</code> contains tests for moving
 * versionable nodes in one workspace.
 *
 * @test
 * @sources WorkspaceMoveVersionableTest.java
 * @executeClass org.apache.jackrabbit.test.api.WorkspaceMoveVersionableTest
 * @keywords level2 versioning
 */
public class WorkspaceMoveVersionableTest extends AbstractWorkspaceVersionableTest {

    /**
     * A VersionException is thrown if the parent node of destAbsPath is
     * versionable and checked-in, or is non-versionable but its nearest
     * versionable ancestor is checked-in.
     */
    public void testMoveNodesVersionableAndCheckedIn() throws RepositoryException {
        // prepare the test data
        // create a non-versionable node below a versionable node
        // required for having a nearest versionable ancestor to a nonversionable sub node
        String dstAbsPath = node1.getPath() + "/" + node2.getName();
        workspace.copy(node2.getPath(), dstAbsPath);

        // make parent node versionable and check-in
        addMixinVersionableToNode(testRootNode, node1);
        node1.checkin();

        // 1. parent node of destAbsPath is non-versionable but its nearest versionable ancestor is checked-in
        try {
            workspace.move(node2.getPath(), dstAbsPath + "/" + node2.getName());
            fail("Copying a node to a node's versionable and checked-in nearest ancestor node of destAbsPath should throw VersionException.");
        } catch (VersionException e) {
            // successful
        }

        // 2. parent node of destAbsPath is versionable and checked-in
        try {
            workspace.move(node2.getPath(), node1.getPath() + "/" + node2.getName());
            fail("Copying a node to a versionable and checked-in parent node of destAbsPath should throw VersionException.");
        } catch (VersionException e) {
            // successful
        }
    }

}