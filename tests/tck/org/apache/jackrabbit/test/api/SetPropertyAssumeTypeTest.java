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
import org.apache.jackrabbit.test.api.nodetype.NodeTypeUtil;

import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Property;

/**
 * <code>SetPropertyAssumeTypeTest</code> tests if when setting a property
 * of type <code>PropertyType.UNDEFINED</code> the type is assumed correctly.
 * The signatures <code>Node.setProperty(String, Value, int)</code>,
 * <code>Node.setProperty(String, String, int)</code>,
 * <code>Node.setProperty(String, Value[], int)</code> and
 * <code>Node.setProperty(String, Node)</code> are tested.
 *
 * @test
 * @sources SetPropertyAssumeTypeTest.java
 * @executeClass org.apache.jackrabbit.test.api.SetPropertyAssumeTypeTest
 * @keywords level2
 */
public class SetPropertyAssumeTypeTest extends AbstractJCRTest {

    private Node testNode;
    private String testPropName;
    private Value binaryValue;
    private Value booleanValue;
    private Value dateValue;
    private Value doubleValue;
    private Value longValue;
    private Value nameValue;
    private Value pathValue;
    private Value stringValue;
    private Value binaryValues[];
    private Value booleanValues[];
    private Value dateValues[];
    private Value doubleValues[];
    private Value longValues[];
    private Value nameValues[];
    private Value pathValues[];
    private Value stringValues[];

    public void setUp() throws Exception {
        super.setUp();

        binaryValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.BINARY);
        booleanValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.BOOLEAN);
        dateValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.DATE);
        doubleValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.DOUBLE);
        longValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.LONG);
        nameValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.NAME);
        pathValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.PATH);
        stringValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.STRING);

        binaryValues = new Value[] {binaryValue};
        booleanValues = new Value[] {booleanValue};
        dateValues = new Value[] {dateValue};
        doubleValues = new Value[] {doubleValue};
        longValues = new Value[] {longValue};
        nameValues = new Value[] {nameValue};
        pathValues = new Value[] {pathValue};
        stringValues = new Value[] {stringValue};
    }

    /**
     * Tests if <code>Node.setProperty(String, Value, int)</code> if the node
     * type of this node does not indicate a specific property type, then the
     * type parameter is used.
     */
    public void testValue() throws NotExecutableException, RepositoryException {

        setUpNodeWithUndefinedProperty(false);

        Property prop;

        // create an extra value for BINARY property to avoid IllegalStateException
        Value stringValueForBinary = NodeTypeUtil.getValueOfType(superuser, PropertyType.STRING);
        prop = testNode.setProperty(testPropName, stringValueForBinary, PropertyType.BINARY);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.BINARY,
                     prop.getType());

        prop = testNode.setProperty(testPropName, stringValue, PropertyType.BOOLEAN);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.BOOLEAN,
                     prop.getType());

        prop = testNode.setProperty(testPropName, doubleValue, PropertyType.DATE);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.DATE,
                     prop.getType());

        prop = testNode.setProperty(testPropName, dateValue, PropertyType.DOUBLE);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.DOUBLE,
                     prop.getType());

        prop = testNode.setProperty(testPropName, dateValue, PropertyType.LONG);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.LONG,
                     prop.getType());

        // create a PathValue that is convertible to the value of name property
        Value valueConvertibleToName = superuser.getValueFactory().createValue(nameValue.getString(), PropertyType.PATH);
        prop = testNode.setProperty(testPropName, valueConvertibleToName, PropertyType.NAME);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.NAME,
                     prop.getType());

        prop = testNode.setProperty(testPropName, nameValue, PropertyType.PATH);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.PATH,
                     prop.getType());

        prop = testNode.setProperty(testPropName, dateValue, PropertyType.STRING);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.STRING,
                     prop.getType());
    }

    /**
     * Tests if <code>Node.setProperty(String, Value[], int)</code> if the node
     * type of this node does not indicate a specific property type, then the
     * type parameter is used.
     */
    public void testValues() throws NotExecutableException, RepositoryException {

        setUpNodeWithUndefinedProperty(true);

        Property prop;

        // create an extra value for BINARY property to avoid IllegalStateException
        Value stringValuesForBinary[] =
            new Value[] {NodeTypeUtil.getValueOfType(superuser, PropertyType.STRING)};
        prop = testNode.setProperty(testPropName, stringValuesForBinary, PropertyType.BINARY);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.BINARY,
                     prop.getType());

        prop = testNode.setProperty(testPropName, stringValues, PropertyType.BOOLEAN);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.BOOLEAN,
                     prop.getType());

        prop = testNode.setProperty(testPropName, doubleValues, PropertyType.DATE);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.DATE,
                     prop.getType());

        prop = testNode.setProperty(testPropName, dateValues, PropertyType.DOUBLE);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.DOUBLE,
                     prop.getType());

        prop = testNode.setProperty(testPropName, dateValues, PropertyType.LONG);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.LONG,
                     prop.getType());

        // create a PathValue that is convertible to the value of name property
        Value valuesConvertibleToName[] =
            new Value[] {superuser.getValueFactory().createValue(nameValue.getString(), PropertyType.PATH)};
        prop = testNode.setProperty(testPropName, valuesConvertibleToName, PropertyType.NAME);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.NAME,
                     prop.getType());

        prop = testNode.setProperty(testPropName, nameValues, PropertyType.PATH);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.PATH,
                     prop.getType());

        prop = testNode.setProperty(testPropName, dateValues, PropertyType.STRING);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.STRING,
                     prop.getType());
    }

    /**
     * Tests if <code>Node.setProperty(String, String, int)</code> if the node
     * type of this node does not indicate a specific property type, then the
     * type parameter is used.
     */
    public void testString() throws NotExecutableException, RepositoryException {

        setUpNodeWithUndefinedProperty(false);

        Property prop;

        prop = testNode.setProperty(testPropName, binaryValue.getString(), PropertyType.BINARY);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.BINARY,
                     prop.getType());

        prop = testNode.setProperty(testPropName, booleanValue.getString(), PropertyType.BOOLEAN);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.BOOLEAN,
                     prop.getType());

        prop = testNode.setProperty(testPropName, dateValue.getString(), PropertyType.DATE);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.DATE,
                     prop.getType());

        prop = testNode.setProperty(testPropName, doubleValue.getString(), PropertyType.DOUBLE);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.DOUBLE,
                     prop.getType());

        prop = testNode.setProperty(testPropName, longValue.getString(), PropertyType.LONG);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.LONG,
                     prop.getType());

        prop = testNode.setProperty(testPropName, nameValue.getString(), PropertyType.NAME);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.NAME,
                     prop.getType());

        prop = testNode.setProperty(testPropName, pathValue.getString(), PropertyType.PATH);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.PATH,
                     prop.getType());

        prop = testNode.setProperty(testPropName, stringValue.getString(), PropertyType.STRING);
        assertEquals("setProperty(String, Value, int) of a property of type undefined " +
                     "must assume the property type of the type parameter.",
                     PropertyType.STRING,
                     prop.getType());
    }

    /**
     * Tests if <code>Node.setProperty(String, Value)</code> if the node type of
     * this node does not indicate a specific property type, then the property
     * type of the supplied Value object is used and if the property already
     * exists (has previously been set) it assumes the new property type.
     */
    public void testValueAssumeTypeOfValue() throws NotExecutableException, RepositoryException {

        setUpNodeWithUndefinedProperty(false);

        Property prop;

        prop = testNode.setProperty(testPropName, binaryValue);
        assertEquals("setProperty(String, Value) of a property of type undefined " +
                     "must assume the property type of the supplied value object.",
                     PropertyType.BINARY,
                     prop.getType());

        prop = testNode.setProperty(testPropName, booleanValue);
        assertEquals("setProperty(String, Value) of a property of type undefined " +
                     "must assume the property type of the supplied value object.",
                     PropertyType.BOOLEAN,
                     prop.getType());

        prop = testNode.setProperty(testPropName, dateValue);
        assertEquals("setProperty(String, Value) of a property of type undefined " +
                     "must assume the property type of the supplied value object.",
                     PropertyType.DATE,
                     prop.getType());

        prop = testNode.setProperty(testPropName, doubleValue);
        assertEquals("setProperty(String, Value) of a property of type undefined " +
                     "must assume the property type of the supplied value object.",
                     PropertyType.DOUBLE,
                     prop.getType());

        prop = testNode.setProperty(testPropName, longValue);
        assertEquals("setProperty(String, Value) of a property of type undefined " +
                     "must assume the property type of the supplied value object.",
                     PropertyType.LONG,
                     prop.getType());

        prop = testNode.setProperty(testPropName, nameValue);
        assertEquals("setProperty(String, Value) of a property of type undefined " +
                     "must assume the property type of the supplied value object.",
                     PropertyType.NAME,
                     prop.getType());

        prop = testNode.setProperty(testPropName, pathValue);
        assertEquals("setProperty(String, Value) of a property of type undefined " +
                     "must assume the property type of the supplied value object.",
                     PropertyType.PATH,
                     prop.getType());

        prop = testNode.setProperty(testPropName, stringValue);
        assertEquals("setProperty(String, Value) of a property of type undefined " +
                     "must assume the property type of the supplied value object.",
                     PropertyType.STRING,
                     prop.getType());
    }

    /**
     * Tests if <code>Node.setProperty(String, Node)</code> if the node type of
     * this node does not indicate a specific property type, then the property
     * type of the supplied Value object is used and if the property already
     * exists (has previously been set) it assumes the new property type.
     */
    public void testNodeAssumeTypeOfValue()
        throws NotExecutableException, RepositoryException {

        setUpNodeWithUndefinedProperty(false);

        Node referenceableNode = testRootNode.addNode(nodeName2);
        referenceableNode.addMixin(mixReferenceable);

        Property prop = testNode.setProperty(testPropName, referenceableNode);
        assertEquals("setProperty(String, Value) of a property of type undefined " +
                     "must assume the property type of the supplied value object.",
                     PropertyType.REFERENCE,
                     prop.getType());
    }

    /**
     * Tests if <code>Node.setProperty(String, Value[])</code> if the node type of
     * this node does not indicate a specific property type, then the property
     * type of the supplied Value object is used and if the property already
     * exists (has previously been set) it assumes the new property type.
     */
    public void testValuesAssumeTypeOfValue() throws NotExecutableException, RepositoryException {

        setUpNodeWithUndefinedProperty(true);

        Property prop;

        prop = testNode.setProperty(testPropName, binaryValues);
        assertEquals("setProperty(String, Value) of a property of type undefined " +
                     "must assume the property type of the supplied value object.",
                     PropertyType.BINARY,
                     prop.getType());

        prop = testNode.setProperty(testPropName, booleanValues);
        assertEquals("setProperty(String, Value) of a property of type undefined " +
                     "must assume the property type of the supplied value object.",
                     PropertyType.BOOLEAN,
                     prop.getType());

        prop = testNode.setProperty(testPropName, dateValues);
        assertEquals("setProperty(String, Value) of a property of type undefined " +
                     "must assume the property type of the supplied value object.",
                     PropertyType.DATE,
                     prop.getType());

        prop = testNode.setProperty(testPropName, doubleValues);
        assertEquals("setProperty(String, Value) of a property of type undefined " +
                     "must assume the property type of the supplied value object.",
                     PropertyType.DOUBLE,
                     prop.getType());

        prop = testNode.setProperty(testPropName, longValues);
        assertEquals("setProperty(String, Value) of a property of type undefined " +
                     "must assume the property type of the supplied value object.",
                     PropertyType.LONG,
                     prop.getType());

        prop = testNode.setProperty(testPropName, nameValues);
        assertEquals("setProperty(String, Value) of a property of type undefined " +
                     "must assume the property type of the supplied value object.",
                     PropertyType.NAME,
                     prop.getType());

        prop = testNode.setProperty(testPropName, pathValues);
        assertEquals("setProperty(String, Value) of a property of type undefined " +
                     "must assume the property type of the supplied value object.",
                     PropertyType.PATH,
                     prop.getType());

        prop = testNode.setProperty(testPropName, stringValues);
        assertEquals("setProperty(String, Value) of a property of type undefined " +
                     "must assume the property type of the supplied value object.",
                     PropertyType.STRING,
                     prop.getType());
    }

    /**
     * Tests if <code>Node.setProperty(String, Value, int)</code> throws a
     * ConstraintViolationException if the type parameter and the type of the
     * property do not match. The exception has to be thrown either immediately
     * (by this method) or on save.
     */
    public void testValueConstraintVioloationExceptionBecauseOfInvalidTypeParameter()
        throws NotExecutableException, RepositoryException {

        // locate a property definition of type string
        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(superuser, PropertyType.STRING, false, false, false, false);

        if (propDef == null) {
            throw new NotExecutableException("No testable property has been found.");
        }

        // create a node of type propDef.getDeclaringNodeType()
        String nodeType = propDef.getDeclaringNodeType().getName();
        Node testNode = testRootNode.addNode(nodeName1, nodeType);
        String testPropName = propDef.getName();

        try {
            testNode.setProperty(testPropName, stringValue, PropertyType.DATE);
            testRootNode.save();
            fail("Node.setProperty(String, Value, int) must throw a " +
                 "ConstraintViolationExcpetion if the type parameter and the " +
                 "type of the property do not match." );
        }
        catch (ConstraintViolationException e) {
            // success
        }
    }

    /**
     * Tests if <code>Node.setProperty(String, String, int)</code> throws a
     * ConstraintViolationException if the type parameter and the type of the
     * property do not match. The exception has to be thrown either immediately
     * (by this method) or on save.
     */
    public void testStringConstraintVioloationExceptionBecauseOfInvalidTypeParameter()
        throws NotExecutableException, RepositoryException {

        // locate a property definition of type string
        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(superuser, PropertyType.STRING, false, false, false, false);

        if (propDef == null) {
            throw new NotExecutableException("No testable property has been found.");
        }

        // create a node of type propDef.getDeclaringNodeType()
        String nodeType = propDef.getDeclaringNodeType().getName();
        Node testNode = testRootNode.addNode(nodeName1, nodeType);
        String testPropName = propDef.getName();

        try {
            testNode.setProperty(testPropName, "abc", PropertyType.DATE);
            testRootNode.save();
            fail("Node.setProperty(String, Value, int) must throw a " +
                 "ConstraintViolationExcpetion if the type parameter and the " +
                 "type of the property do not match." );
        }
        catch (ConstraintViolationException e) {
            // success
        }
    }

    /**
     * Tests if <code>Node.setProperty(String, Value[], int)</code> throws a
     * ConstraintViolationException if the type parameter and the type of the
     * property do not match. The exception has to be thrown either immediately
     * (by this method) or on save.
     */
    public void testValuesConstraintVioloationExceptionBecauseOfInvalidTypeParameter()
        throws NotExecutableException, RepositoryException {

        // locate a property definition of type string
        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(superuser, PropertyType.STRING, true, false, false, false);

        if (propDef == null) {
            throw new NotExecutableException("No testable property has been found.");
        }

        // create a node of type propDef.getDeclaringNodeType()
        String nodeType = propDef.getDeclaringNodeType().getName();
        Node testNode = testRootNode.addNode(nodeName1, nodeType);
        String testPropName = propDef.getName();

        try {
            testNode.setProperty(testPropName, stringValues, PropertyType.DATE);
            testRootNode.save();
            fail("Node.setProperty(String, Value, int) must throw a " +
                 "ConstraintViolationExcpetion if the type parameter and the " +
                 "type of the property do not match." );
        }
        catch (ConstraintViolationException e) {
            // success
        }
    }

    //--------------------------< internal >------------------------------------

    private void setUpNodeWithUndefinedProperty(boolean multiple)
        throws NotExecutableException {

        try {
            // locate a property definition of type undefined
            PropertyDefinition propDef =
                    NodeTypeUtil.locatePropertyDef(superuser, PropertyType.UNDEFINED, multiple, false, false, false);

            if (propDef == null) {
                throw new NotExecutableException("No testable property of type " +
                                                 "UNDEFINED has been found.");
            }

            // create a node of type propDef.getDeclaringNodeType()
            String nodeType = propDef.getDeclaringNodeType().getName();
            testNode = testRootNode.addNode(nodeName1, nodeType);
            testPropName = propDef.getName();
        }
        catch (RepositoryException e) {
            throw new NotExecutableException("Not able to set up test items.");
        }
    }

}