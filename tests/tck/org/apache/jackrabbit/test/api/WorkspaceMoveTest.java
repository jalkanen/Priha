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

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * <code>WorkspaceMoveTest</code> contains tests for copying nodes in one
 * workspace.
 *
 * @test
 * @sources WorkspaceMoveTest.java
 * @executeClass org.apache.jackrabbit.test.api.WorkspaceMoveTest
 * @keywords level2
 */
public class WorkspaceMoveTest extends AbstractWorkspaceCopyTest {

    /**
     * Operation is performed entirely within the persistent workspace, it does
     * not involve transient storage and therefore does not require a save
     */
    public void testMoveNodes() throws RepositoryException {
        String dstAbsPath = node2.getPath() + "/" + node1.getName();
        workspace.move(node1.getPath(), dstAbsPath);

        // there should not be any pending changes after copy
        assertFalse(superuser.hasPendingChanges());
    }

    /**
     * The destAbsPath provided must not have an index on its final element. If
     * it does, then a RepositoryException is thrown. Strictly speaking, the
     * destAbsPath parameter is actually an absolute path to the parent node of
     * the new location, appended with the new name desired for the copied node.
     * It does not specify a position within the child node ordering.
     */
    public void testMoveNodesAbsolutePath() {
        try {
            // copy referenceable node to an absolute path containing index
            String dstAbsPath = node2.getPath() + "/" + node1.getName() + "[2]";
            workspace.move(node1.getPath(), dstAbsPath);
            fail("Moving a node to an absolute path containing index should not be possible.");
        } catch (RepositoryException e) {
            // successful

        }
    }

    /**
     * A ConstraintViolationException is thrown if the operation would violate a
     * node-type or other implementation-specific constraint.
     */
    public void testMoveNodesConstraintViolationException() throws RepositoryException {
        // if parent node is nt:base then no sub nodes can be created
        Node subNodesNotAllowedNode = testRootNode.addNode(nodeName3, ntBase);
        testRootNode.save();
        try {
            String dstAbsPath = subNodesNotAllowedNode.getPath() + "/" + node2.getName();
            workspace.move(node2.getPath(), dstAbsPath);
            fail("Moving a node below a node which can not have any sub nodes should throw a ConstraintViolationException.");
        } catch (ConstraintViolationException e) {
            // successful
        }
    }


    /**
     * An AccessDeniedException is thrown if the current session (i.e., the
     * session that was used to acquire this Workspace object) does not have
     * sufficient access permissions to complete the operation.
     */
    public void testMoveNodesAccessDenied() throws RepositoryException {
        Session readOnlySuperuser = helper.getReadOnlySession();
        try {
            String dstAbsPath = node2.getPath() + "/" + node1.getName();
            try {
                readOnlySuperuser.getWorkspace().move(node1.getPath(), dstAbsPath);
                fail("Copy in a read-only session should throw an AccessDeniedException.");
            } catch (AccessDeniedException e) {
                // successful
            }
        } finally {
            readOnlySuperuser.logout();
        }
    }

    /**
     * A PathNotFoundException is thrown if the node at srcAbsPath or the parent
     * of the new node at destAbsPath does not exist.
     */
    public void testMoveNodesPathNotExisting() throws RepositoryException {

        String srcAbsPath = node1.getPath();
        String dstAbsPath = node2.getPath() + "/" + node1.getName();

        // srcAbsPath is not existing
        String invalidSrcPath = srcAbsPath + "invalid";
        try {
            workspace.move(invalidSrcPath, dstAbsPath);
            fail("Not existing source path '" + invalidSrcPath + "' should throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // successful
        }

        // dstAbsPath parent is not existing
        String invalidDstParentPath = node2.getPath() + "invalid/" + node1.getName();
        try {
            workspace.move(srcAbsPath, invalidDstParentPath);
            fail("Not existing destination parent path '" + invalidDstParentPath + "' should throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // successful
        }
    }


    /**
     * A LockException is thrown if a lock prevents the copy.
     */
    public void testMoveNodesLocked() throws RepositoryException {
        // we assume repository supports locking
        String dstAbsPath = node2.getPath() + "/" + node1.getName();

        // get other session
        Session otherSession = helper.getReadWriteSession();

        try {
            // get lock target node in destination wsp through other session
            Node lockTarget = (Node) otherSession.getItem(node2.getPath());

            // add mixin "lockable" to be able to lock the node
            if (!lockTarget.getPrimaryNodeType().isNodeType(mixLockable)) {
                lockTarget.addMixin(mixLockable);
                lockTarget.getParent().save();
            }

            // lock dst parent node using other session
            lockTarget.lock(true, true);

            try {
                workspace.move(node1.getPath(), dstAbsPath);
                fail("LockException was expected.");
            } catch (LockException e) {
                // successful
            } finally {
                lockTarget.unlock();
            }
        } finally {
            otherSession.logout();
        }
    }
}