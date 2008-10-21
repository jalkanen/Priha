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
package org.apache.jackrabbit.test;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite that includes all the JCR API tests
 */
public class JCRTestSuite extends TestSuite {

    public JCRTestSuite() {
        super("JCR API tests");
        addTest(org.apache.jackrabbit.test.api.TestAll.suite());
        addTest(org.apache.jackrabbit.test.api.query.TestAll.suite());
        addTest(org.apache.jackrabbit.test.api.nodetype.TestAll.suite());
        addTest(org.apache.jackrabbit.test.api.util.TestAll.suite());
        addTest(org.apache.jackrabbit.test.api.lock.TestAll.suite());
        addTest(org.apache.jackrabbit.test.api.version.TestAll.suite());
        addTest(org.apache.jackrabbit.test.api.observation.TestAll.suite());
    }

    public static Test suite() {
        return new JCRTestSuite();
    }
}
