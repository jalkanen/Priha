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
package org.apache.jackrabbit.test.api.observation;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test suite that includes all testcases for the Observation module.
 */
public class TestAll extends TestCase {

    /**
     * Returns a <code>Test</code> suite that executes all tests inside this
     * package.
     *
     * @return a <code>Test</code> suite that executes all tests inside this
     *         package.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite("Observation tests");

        suite.addTestSuite(EventIteratorTest.class);
        suite.addTestSuite(EventTest.class);
        suite.addTestSuite(GetRegisteredEventListenersTest.class);
        suite.addTestSuite(LockingTest.class);
        suite.addTestSuite(NodeAddedTest.class);
        suite.addTestSuite(NodeRemovedTest.class);
        suite.addTestSuite(NodeMovedTest.class);
        suite.addTestSuite(NodeReorderTest.class);
        suite.addTestSuite(PropertyAddedTest.class);
        suite.addTestSuite(PropertyChangedTest.class);
        suite.addTestSuite(PropertyRemovedTest.class);
        suite.addTestSuite(AddEventListenerTest.class);
        suite.addTestSuite(WorkspaceOperationTest.class);

        return suite;
    }
}
