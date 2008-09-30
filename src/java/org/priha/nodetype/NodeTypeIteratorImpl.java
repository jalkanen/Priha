/*
    Priha - A JSR-170 implementation library.

    Copyright (C) 2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    Licensed under the Apache License, Version 2.0 (the "License"); 
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at 
    
      http://www.apache.org/licenses/LICENSE-2.0 
      
    Unless required by applicable law or agreed to in writing, software 
    distributed under the License is distributed on an "AS IS" BASIS, 
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
    See the License for the specific language governing permissions and 
    limitations under the License. 
 */
package org.priha.nodetype;

import java.util.List;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;

import org.priha.util.GenericIterator;

public class NodeTypeIteratorImpl extends GenericIterator 
    implements NodeTypeIterator
{
    public NodeTypeIteratorImpl(List<NodeType> list)
    {
        super(list);
    }

    public NodeType nextNodeType()
    {
        return (NodeType)next();
    }
}
