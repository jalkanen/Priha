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

import javax.jcr.Node;

/**
 * <code>SetPropertyNodeTest</code> tests the <code>Node.setProperty(String,
 * Node)</code> method
 *
 * @test
 * @sources SetPropertyNodeTest.java
 * @executeClass org.apache.jackrabbit.test.api.SetPropertyNodeTest
 * @keywords level2
 */
public class SetPropertyNodeTest extends AbstractJCRTest {

    private Node testNode;

    private Node n1;
    private Node n2;

    protected void setUp() throws Exception {
        super.setUp();
        testNode = testRootNode.addNode(nodeName1, testNodeType);

        n1 = testRootNode.addNode(nodeName2, testNodeType);
        n2 = testRootNode.addNode(nodeName3, testNodeType);
    }

    /**
     * Tests if adding a property with <code>Node.setProperty(String,
     * Node)</code> works with <code>Session.save()</code>
     */
    public void testNewNodePropertySession() throws Exception {
        testNode.setProperty(propertyName1, n1);
        superuser.save();
        assertEquals("Setting property with Node.setProperty(String, Node) and Session.save() not working",
                n1.getUUID(),
                testNode.getProperty(propertyName1).getString());
    }

    /**
     * Tests if modifying a property with <code>Node.setProperty(String,
     * Node)</code> works with <code>Session.save()</code>
     */
    public void testModifyNodePropertySession() throws Exception {
        testNode.setProperty(propertyName1, n1);
        superuser.save();
        testNode.setProperty(propertyName1, n2);
        superuser.save();
        assertEquals("Modifying property with Node.setProperty(String, Node) and Session.save() not working",
                n2.getUUID(),
                testNode.getProperty(propertyName1).getString());
    }

    /**
     * Tests if adding a property with <code>Node.setProperty(String,
     * Node)</code> works with <code>parentNode.save()</code>
     */
    public void testNewNodePropertyParent() throws Exception {
        testNode.setProperty(propertyName1, n1);
        testRootNode.save();
        assertEquals("Setting property with Node.setProperty(String, Node) and parentNode.save() not working",
                n1.getUUID(),
                testNode.getProperty(propertyName1).getString());
    }

    /**
     * Tests if modifying a property with <code>Node.setProperty(String,
     * Node)</code> works with <code>parentNode.save()</code>
     */
    public void testModifyNodePropertyParent() throws Exception {
        testNode.setProperty(propertyName1, n1);
        testRootNode.save();
        testNode.setProperty(propertyName1, n2);
        testRootNode.save();
        assertEquals("Modifying property with Node.setProperty(String, Node) and parentNode.save() not working",
                n2.getUUID(),
                testNode.getProperty(propertyName1).getString());
    }

    /**
     * Tests if removing a <code>Node</code> property with
     * <code>Node.setProperty(String, null)</code> works with
     * <code>Session.save()</code>
     */
    public void testRemoveNodePropertySession() throws Exception {
        testNode.setProperty(propertyName1, n1);
        superuser.save();
        testNode.setProperty(propertyName1, (Node) null);
        superuser.save();
        assertFalse("Removing property with Node.setProperty(String, (Node)null) and Session.save() not working",
                testNode.hasProperty(propertyName1));
    }

    /**
     * Tests if removing a <code>Node</code> property with
     * <code>Node.setProperty(String, null)</code> works with
     * <code>parentNode.save()</code>
     */
    public void testRemoveNodePropertyParent() throws Exception {
        testNode.setProperty(propertyName1, n1);
        testRootNode.save();
        testNode.setProperty(propertyName1, (Node) null);
        testRootNode.save();
        assertFalse("Removing property with Node.setProperty(String, (Node)null) and parentNode.save() not working",
                testNode.hasProperty(propertyName1));
    }

}