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

import javax.jcr.Session;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.NodeType;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.Calendar;

/**
 * <code>SetValueVersionExceptionTest</code>...
 *
 * @test
 * @sources SetValueVersionExceptionTest.java
 * @executeClass org.apache.jackrabbit.test.api.SetValueVersionExceptionTest
 * @keywords level2 versionable
 */
public class SetValueVersionExceptionTest extends AbstractJCRTest {

    /**
     * The session we use for the tests
     */
    private Session session;

    private Node node;

    private Property property;
    private Property multiProperty;

    private Value value;
    private Value values[];

    /**
     * Sets up the fixture for the test cases.
     */
    protected void setUp() throws Exception {
        super.setUp();
        session = helper.getReadOnlySession();

        value = session.getValueFactory().createValue("abc");
        values = new Value[] {value};

        if (session.getRepository().getDescriptor(Repository.OPTION_LOCKING_SUPPORTED) == null) {
            throw new NotExecutableException("Versioning is not supported.");
        }

        // create a node that is versionable
        node = testRootNode.addNode(nodeName1, testNodeType);
        // or try to make it versionable if it is not
        if (!node.isNodeType(mixVersionable)) {
            if (node.canAddMixin(mixVersionable)) {
                node.addMixin(mixVersionable);
            } else {
                throw new NotExecutableException("Failed to set up required test items");
            }
        }

        property = node.setProperty(propertyName1, value);
        multiProperty = node.setProperty(propertyName2, values);

        testRootNode.save();

        node.checkin();
    }

    /**
     * Releases the session aquired in {@link #setUp()}.
     */
    protected void tearDown() throws Exception {
        node.checkout();

        if (session != null) {
            session.logout();
        }
        super.tearDown();
    }

    /**
     * Tests if setValue(Value) throws a VersionException immediately
     * or on save if the parent node of this property is checked-in.
     */
    public void testValue() throws RepositoryException {
        try {
            property.setValue(value);
            node.save();
            fail("Property.setValue(Value) must throw a VersionException " +
                 "immediately or on save if the parent node of this property " +
                 "is checked-in.");
        }
        catch (VersionException e) {
            // success
        }
    }

    /**
     * Tests if setValue(Value[]) throws a VersionException immediately
     * or on save if the parent node of this property is checked-in.
     */
    public void testValueArray() throws RepositoryException {
        try {
            multiProperty.setValue(values);
            node.save();
            fail("Property.setValue(Value[]) must throw a VersionException " +
                 "immediately or on save if the parent node of this property " +
                 "is checked-in.");
        }
        catch (VersionException e) {
            // success
        }
    }

    /**
     * Tests if setValue(String) throws a VersionException immediately
     * or on save if the parent node of this property is checked-in.
     */
    public void testString() throws RepositoryException {
        try {
            property.setValue("abc");
            node.save();
            fail("Property.setValue(String) must throw a VersionException " +
                 "immediately or on save if the parent node of this property " +
                 "is checked-in.");
        }
        catch (VersionException e) {
            // success
        }
    }

    /**
     * Tests if setValue(String[]) throws a VersionException immediately
     * or on save if the parent node of this property is checked-in.
     */
    public void testStringArray() throws RepositoryException {
        try {
            String values[] = new String[] {"abc"};
            multiProperty.setValue(values);
            node.save();
            fail("Property.setValue(String[]) must throw a VersionException " +
                 "immediately or on save if the parent node of this property " +
                 "is checked-in.");
        }
        catch (VersionException e) {
            // success
        }
    }

    /**
     * Tests if setValue(InputStream) throws a VersionException immediately
     * or on save if the parent node of this property is checked-in.
     */
    public void testInputStream() throws RepositoryException {
        try {
            byte[] bytes = {123};
            InputStream value = new ByteArrayInputStream(bytes);
            property.setValue(value);
            node.save();
            fail("Property.setValue(InputStream) must throw a VersionException " +
                 "immediately or on save if the parent node of this property " +
                 "is checked-in.");
        }
        catch (VersionException e) {
            // success
        }
    }

    /**
     * Tests if setValue(long) throws a VersionException immediately
     * or on save if the parent node of this property is checked-in.
     */
    public void testLong() throws RepositoryException {
        try {
            property.setValue(123);
            node.save();
            fail("Property.setValue(long) must throw a VersionException " +
                 "immediately or on save if the parent node of this property " +
                 "is checked-in.");
        }
        catch (VersionException e) {
            // success
        }
    }

    /**
     * Tests if setValue(double) throws a VersionException immediately
     * or on save if the parent node of this property is checked-in.
     */
    public void testDouble() throws RepositoryException {
        try {
            property.setValue(1.23);
            node.save();
            fail("Property.setValue(double) must throw a VersionException " +
                 "immediately or on save if the parent node of this property " +
                 "is checked-in.");
        }
        catch (VersionException e) {
            // success
        }
    }

    /**
     * Tests if setValue(Calendar) throws a VersionException immediately
     * or on save if the parent node of this property is checked-in.
     */
    public void testCalendar() throws RepositoryException {
        try {
            property.setValue(Calendar.getInstance());
            node.save();
            fail("Property.setValue(Calendar) must throw a VersionException " +
                 "immediately or on save if the parent node of this property " +
                 "is checked-in.");
        }
        catch (VersionException e) {
            // success
        }
    }

    /**
     * Tests if setValue(boolean) throws a VersionException immediately
     * or on save if the parent node of this property is checked-in.
     */
    public void testBoolean() throws RepositoryException {
        try {
            property.setValue(true);
            node.save();
            fail("Property.setValue(boolean) must throw a VersionException " +
                 "immediately or on save if the parent node of this property " +
                 "is checked-in.");
        }
        catch (VersionException e) {
            // success
        }
    }

    /**
     * Tests if setValue(Node) throws a VersionException immediately
     * or on save if the parent node of this property is checked-in.
     */
    public void testNode()
        throws NotExecutableException, RepositoryException {

        // create a referenceable node
        Node referenceableNode = testRootNode.addNode(nodeName3);
        referenceableNode.addMixin(mixReferenceable);

        // create a node with a reference property
        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.REFERENCE, false, false, false, false);
        if (propDef == null) {
            throw new NotExecutableException("Failed to set up required test items.");
        }
        NodeType nodeType = propDef.getDeclaringNodeType();
        Node node = testRootNode.addNode(nodeName4, nodeType.getName());

        // try to make it versionable if it is not
        if (!node.isNodeType(mixVersionable)) {
            if (node.canAddMixin(mixVersionable)) {
                node.addMixin(mixVersionable);
            } else {
                throw new NotExecutableException("Failed to set up required test items.");
            }
        }

        Property property = node.setProperty(propDef.getName(), referenceableNode);

        testRootNode.save();

        node.checkin();

        try {
            property.setValue(referenceableNode);
            node.save();
            fail("Property.setValue(Node) must throw a VersionException " +
                 "immediately or on save if the parent node of this property " +
                 "is checked-in.");
        }
        catch (VersionException e) {
            // success
        }

        node.checkout();
    }
}
