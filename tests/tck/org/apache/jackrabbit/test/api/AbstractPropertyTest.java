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

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.Session;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Value;

/**
 * Provides the common setup method for all level 1 property tests.
 */
abstract class AbstractPropertyTest extends AbstractJCRTest {

    /** String encoding in a stream */
    protected static String UTF8 = "UTF-8";

    /** A read only session */
    protected Session session;

    /* The property under test */
    protected Property prop;

    /** <code>true</code> if the property is multi valued */
    protected boolean multiple;

    /**
     * Concrete subclasses return the type of property they test. One of the
     * values defined in {@link javax.jcr.PropertyType}.
     */
    protected abstract int getPropertyType();

    /**
     * Sets up the fixture for the tests.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();
        session = helper.getReadOnlySession();

        prop = PropertyUtil.searchProp(session, session.getRootNode().getNode(testPath), getPropertyType());
        if (prop == null) {
            String msg = "Workspace does not contain a node with a " +
                    PropertyType.nameFromValue(getPropertyType()) + " property.";
            throw new NotExecutableException(msg);
        }
        multiple = prop.getDefinition().isMultiple();
        Value val = PropertyUtil.getValue(prop);
        if (val == null) {
            String msg = PropertyType.nameFromValue(getPropertyType()) +
                    " property does not contain a value";
            throw new NotExecutableException(msg);
        }
    }

    /**
     * Releases the session aquired in {@link #setUp()}.
     */
    protected void tearDown() throws Exception {
        if (session != null) {
            session.logout();
        }
        super.tearDown();
    }
}
