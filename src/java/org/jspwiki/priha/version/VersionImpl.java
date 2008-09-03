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

import javax.jcr.*;
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
    public VersionImpl( SessionImpl session, String path, GenericNodeType primaryType, NodeDefinition nDef )
        throws ValueFormatException,
               VersionException,
               LockException,
               ConstraintViolationException,
               RepositoryException
    {
        super( session, path, primaryType, nDef, true );
    }

    public VersionHistory getContainingHistory() throws RepositoryException
    {
        return (VersionHistory)getParent();
    }

    public Calendar getCreated() throws RepositoryException
    {
        Property p = getProperty("jcr:created");
        
        return p.getDate();
    }

    public Version[] getPredecessors() throws RepositoryException
    {
        Property p = getProperty("jcr:predecessors");
        
        return collateVersions( p );
    }

    public Version[] getSuccessors() throws RepositoryException
    {
        Property p = getProperty("jcr:successors");
        
        Version[] result = collateVersions(p);
        
        return result;
    }

    private Version[] collateVersions(Property p) throws ValueFormatException, RepositoryException, ItemNotFoundException
    {
        Value[] vals = p.getValues();
        
        Version[] result = new Version[vals.length];
        
        int i = 0;
        for( Value v : vals )
        {
            String uuid = v.getString();
           
            Version vers = (Version) getSession().getNodeByUUID( uuid );
            
            result[i++] = vers;
        }
        return result;
    }

}
