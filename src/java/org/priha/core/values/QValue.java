/*
    Priha - A JSR-170 implementation library.

    Copyright (C) 2007-2009 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package org.priha.core.values;

import org.priha.core.namespace.NamespaceMapper;

/**
 *  A QValue is a Value which contains something with an FQN, like a QName or a Path.
 *  Because some Values require a mapping to the current namespace, we need to have
 *  a way to represent a Session-independent Value.
 */
public abstract class QValue
{
    public abstract ValueImpl getValue(NamespaceMapper nsm);
    
    public interface QValueInner
    {
        public QValue getQValue();
    }

    public abstract String getString();
}
