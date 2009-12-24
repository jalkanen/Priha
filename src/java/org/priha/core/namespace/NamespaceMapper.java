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
package org.priha.core.namespace;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;

import org.priha.util.QName;

public interface NamespaceMapper
{
    public QName toQName( String prefixedName ) throws NamespaceException, RepositoryException;
    
    public String fromQName( QName name ) throws NamespaceException;
}
