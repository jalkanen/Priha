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
package org.jspwiki.priha.version;

import java.util.Calendar;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.jspwiki.priha.core.NodeImpl;
import org.jspwiki.priha.core.SessionImpl;
import org.jspwiki.priha.nodetype.GenericNodeType;

public class VersionImpl
    extends NodeImpl
    implements Version
{
    private VersionHistoryImpl m_history;

    public VersionImpl( SessionImpl session, String path, GenericNodeType primaryType, NodeDefinition nDef, VersionHistoryImpl history )
        throws ValueFormatException,
               VersionException,
               LockException,
               ConstraintViolationException,
               RepositoryException
    {
        super( session, path, primaryType, nDef, true );

        m_history = history;
    }

    public VersionHistory getContainingHistory() throws RepositoryException
    {
        return m_history;
    }

    public Calendar getCreated() throws RepositoryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Version[] getPredecessors() throws RepositoryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Version[] getSuccessors() throws RepositoryException
    {
        // TODO Auto-generated method stub
        return null;
    }

}
