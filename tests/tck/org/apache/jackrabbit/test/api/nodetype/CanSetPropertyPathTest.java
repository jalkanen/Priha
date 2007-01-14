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
package org.apache.jackrabbit.test.api.nodetype;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.Session;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.text.ParseException;

/**
 * Test of <code>NodeType.canSetProperty(String propertyName, Value
 * value)</code> and <code>NodeType.canSetProperty(String propertyNa  me,
 * Value[] values)</code> where property is of type Path.
 *
 * @test
 * @sources CanSetPropertyPathPath.java
 * @executeClass org.apache.jackrabbit.test.api.nodetype.CanSetPropertyPathTest
 * @keywords level1
 */
public class CanSetPropertyPathTest extends AbstractJCRTest {
    /**
     * The session we use for the tests
     */
    private Session session;

    /**
     * Sets up the fixture for the test cases.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();

        session = helper.getReadOnlySession();
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


    /**
     * Tests if NodeType.canSetProperty(String propertyName, Value value)
     * returns true if value and its type are convertible to PathValue.
     */
    public void testConversions()
            throws NotExecutableException, RepositoryException {

        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.PATH, false, false, false, false);

        if (propDef == null) {
            throw new NotExecutableException("No path property def that meets the " +
                    "requirements of the test has been found");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();


        Value pathStringValue = superuser.getValueFactory().createValue("abc");
        assertTrue("canSetProperty(String propertyName, Value value) must return " +
                "true if the property is of type Path and value is a StringValue " +
                "that is convertible to a PathValue",
                nodeType.canSetProperty(propDef.getName(), pathStringValue));

        Value noPathStringValue = superuser.getValueFactory().createValue("a:b:c");
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Path and value is a StringValue " +
                "that is not convertible to a PathValue",
                nodeType.canSetProperty(propDef.getName(), noPathStringValue));

        Value pathBinaryValue = superuser.getValueFactory().createValue("abc", PropertyType.BINARY);
        assertTrue("canSetProperty(String propertyName, Value value) must return " +
                "true if the property is of type Path and value is a UTF-8 " +
                "BinaryValue that is convertible to a PathValue",
                nodeType.canSetProperty(propDef.getName(), pathBinaryValue));

        Value noPathBinaryValue = superuser.getValueFactory().createValue("a:b:c", PropertyType.BINARY);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Path and value is a BinaryValue" +
                "that is not convertible to a PathValue",
                nodeType.canSetProperty(propDef.getName(), noPathBinaryValue));

        Value dateValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.DATE);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Path and value is a DateValue",
                nodeType.canSetProperty(propDef.getName(), dateValue));

        Value doubleValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.DOUBLE);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Path and value is a DoubleValue",
                nodeType.canSetProperty(propDef.getName(), doubleValue));

        Value longValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.LONG);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Path and value is a LongValue",
                nodeType.canSetProperty(propDef.getName(), longValue));

        Value booleanValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.BOOLEAN);
        assertFalse("canSetProperty(String propertyName, Value value) must return " +
                "false if the property is of type Path and value is a BooleanValue",
                nodeType.canSetProperty(propDef.getName(), booleanValue));

        Value pathValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.NAME);
        assertTrue("canSetProperty(String propertyName, Value value) must return " +
                "true if the property is of type Path and value is a NameValue",
                nodeType.canSetProperty(propDef.getName(), pathValue));

        Value relPathValue = superuser.getValueFactory().createValue("abc", PropertyType.PATH);
        assertTrue("canSetProperty(String propertyName, Value value) must return " +
                "true if the property is of type Path and value is a PathValue",
                nodeType.canSetProperty(propDef.getName(), relPathValue));

        Value absPathValue = superuser.getValueFactory().createValue("/abc", PropertyType.PATH);
        assertTrue("canSetProperty(String propertyName, Value value) must return " +
                "true if the property is of type Path and value is a PathValue",
                nodeType.canSetProperty(propDef.getName(), absPathValue));
    }

    /**
     * Tests if NodeType.canSetProperty(String propertyName, Value[] values)
     * returns true if all values and its types are convertible to PathValue.
     */
    public void testConversionsMultiple()
            throws NotExecutableException, RepositoryException {

        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.PATH, true, false, false, false);

        if (propDef == null) {
            throw new NotExecutableException("No multiple path property def that meets the " +
                    "requirements of the test has been found");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();


        Value pathValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.PATH);

        Value pathStringValue = superuser.getValueFactory().createValue("abc");
        Value pathStringValues[] = new Value[] {pathStringValue};
        assertTrue("canSetProperty(String propertyName, Value[] values) must return " +
                "true if the property is of type Path and values are of type StringValue " +
                "that are convertible to PathValues",
                nodeType.canSetProperty(propDef.getName(), pathStringValues));

        Value notPathStringValue = superuser.getValueFactory().createValue("a:b:c");
        Value notPathStringValues[] = new Value[] {pathValue, notPathStringValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Path and values are of type StringValue " +
                "that are not convertible to PathValues ",
                nodeType.canSetProperty(propDef.getName(), notPathStringValues));

        Value pathBinaryValue = superuser.getValueFactory().createValue("abc", PropertyType.BINARY);
        Value pathBinaryValues[] = new Value[] {pathBinaryValue};
        assertTrue("canSetProperty(String propertyName, Value[] values) must return " +
                "true if the property is of type Path and values are of type BinaryValue " +
                "that are convertible to PathValues",
                nodeType.canSetProperty(propDef.getName(), pathBinaryValues));

        Value notPathBinaryValue = superuser.getValueFactory().createValue("a:b:c", PropertyType.BINARY);
        Value notPathBinaryValues[] = new Value[] {pathValue, notPathBinaryValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Path and values are of type BinaryValue " +
                "that are not convertible to PathValues",
                nodeType.canSetProperty(propDef.getName(), notPathBinaryValues));

        Value dateValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.DATE);
        Value dateValues[] = new Value[] {pathValue, dateValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Path and values are of type DateValue",
                nodeType.canSetProperty(propDef.getName(), dateValues));

        Value doubleValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.DOUBLE);
        Value doubleValues[] = new Value[] {pathValue, doubleValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Path and values are of type DoubleValue",
                nodeType.canSetProperty(propDef.getName(), doubleValues));

        Value longValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.LONG);
        Value longValues[] = new Value[] {pathValue, longValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Path and values are of type LongValue",
                nodeType.canSetProperty(propDef.getName(), longValues));

        Value booleanValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.BOOLEAN);
        Value booleanValues[] = new Value[] {booleanValue};
        assertFalse("canSetProperty(String propertyName, Value[] values) must return " +
                "false if the property is of type Path and values are of type BooleanValue",
                nodeType.canSetProperty(propDef.getName(), booleanValues));

        Value nameValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.NAME);
        Value nameValues[] = new Value[] {nameValue};
        assertTrue("canSetProperty(String propertyName, Value[] values) must return " +
                "true if the property is of type Path and values are of type NameValue",
                nodeType.canSetProperty(propDef.getName(), nameValues));

        Value pathValues[] = new Value[] {pathValue, pathValue};
        assertTrue("canSetProperty(String propertyName, Value[] values) must return " +
                "true if the property is of type Path and values are of type PathValue",
                nodeType.canSetProperty(propDef.getName(), pathValues));
    }

    /**
     * Tests if canSetProperty(String propertyName, Value value) returns false
     * if value does not satisfy the value constraints of the property def
     */
    public void testValueConstraintNotSatisfied()
            throws NotExecutableException, ParseException, RepositoryException {

        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.PATH, false, false, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No path property def with " +
                    "testable value constraints has been found");
        }

        Value value = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, false);
        if (value == null) {
            throw new NotExecutableException("No path property def with " +
                    "testable value constraints has been found");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();

        assertFalse("canSetProperty(String propertyName, Value value) must " +
                "return false if value does not match the value constraints.",
                nodeType.canSetProperty(propDef.getName(), value));
    }

    /**
     * Tests if canSetProperty(String propertyName, Value[] values) returns
     * false if values do not satisfy the value constraints of the property def
     */
    public void testValueConstraintNotSatisfiedMultiple()
            throws NotExecutableException, ParseException, RepositoryException {

        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.PATH, true, false, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No multiple path property def with " +
                    "testable value constraints has been found");
        }

        Value value = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, false);
        if (value == null) {
            throw new NotExecutableException("No multiple path property def with " +
                    "testable value constraints has been found");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();
        Value values[] = new Value[] {value};

        assertFalse("canSetProperty(String propertyName, Value[] values) must " +
                "return false if values do not match the value constraints.",
                nodeType.canSetProperty(propDef.getName(), values));
    }
}