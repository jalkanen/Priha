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
package org.apache.jackrabbit.test.api.query;

/**
 * Test cases for order by queries on double properties.
 *
 * @tck.config testroot path to node that accepts child nodes of type
 *   <code>nodetype</code>
 * @tck.config nodetype name of a node type
 * @tck.config nodename1 name of a child node of type <code>nodetype</code>
 * @tck.config nodename2 name of a child node of type <code>nodetype</code>
 * @tck.config nodename3 name of a child node of type <code>nodetype</code>
 * @tck.config nodename4 name of a child node of type <code>nodetype</code>
 * @tck.config propertyname1 name of a single value double property.
 *
 * @test
 * @sources OrderByDoubleTest.java
 * @executeClass org.apache.jackrabbit.test.api.query.OrderByDoubleTest
 * @keywords level2
 */
public class OrderByDoubleTest extends AbstractOrderByTest {

    /**
     * Tests order by queries with double properties.
     */
    public void testDoubleOrder1() throws Exception {
        populate(new double[]{-2.4, 4.3, 0.0});
        checkOrder(new String[]{nodeName1, nodeName3, nodeName2});
    }

    /**
     * Tests order by queries with double properties.
     */
    public void testDoubleOrder2() throws Exception {
        populate(new double[]{-1.5, -1.4, -1.39});
        checkOrder(new String[]{nodeName1, nodeName2, nodeName3});
    }

}
