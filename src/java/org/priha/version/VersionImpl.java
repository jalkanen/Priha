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
package org.priha.version;

import java.util.Calendar;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.priha.core.NodeImpl;
import org.priha.core.SessionImpl;
import org.priha.nodetype.QNodeDefinition;
import org.priha.nodetype.QNodeType;
import org.priha.path.Path;

/**
 *  Implements a JCR Version.  The JCR specification is slightly ambiguous on
 *  whether a Version with no predecessors/successors should return an empty
 *  array, but we will return an empty array if no such beast exists.
 */
public class VersionImpl
    extends NodeImpl
    implements Version
{
    public VersionImpl( SessionImpl session, Path path, QNodeType primaryType, QNodeDefinition nDef, boolean initDefaults )
        throws ValueFormatException,
               VersionException,
               LockException,
               ConstraintViolationException,
               RepositoryException
    {
        super( session, path, primaryType, nDef, initDefaults );
    }

    /**
     *  {@inheritDoc}
     */
    public VersionHistory getContainingHistory() throws RepositoryException
    {
        return (VersionHistory)getParent();
    }

    /**
     *  {@inheritDoc}
     */
    public Calendar getCreated() throws RepositoryException
    {
        Property p = getProperty("jcr:created");
        
        return p.getDate();
    }

    /**
     *  Priha returns a valid array unless there was something weird
     *  going on. If there are no predecessors, an empty array is returned.
     */
    public Version[] getPredecessors() throws RepositoryException
    {
        try
        {
            Property p = getProperty("jcr:predecessors");
        
            return collateVersions( p );
        }
        catch( PathNotFoundException e )
        {
            return new Version[0];
        }
    }

    /**
     *  Priha returns a valid array unless there was something weird
     *  going on. If there are no successors, an empty array is returned.
     */
    public Version[] getSuccessors() throws RepositoryException
    {
        try
        {
            Property p = getProperty("jcr:successors");
        
            Version[] result = collateVersions(p);
        
            return result;
        }
        catch( PathNotFoundException e )
        {
            return new Version[0];
        }
    }

    /**
     *  Creates a Version array based on the UUIDs stored in the Property.
     *  
     *  @param p Multi-valued property to iterate over. Must contain UUIDs.
     *  @return An array of Versions.
     *  @throws ValueFormatException If the contents of p are not UUIDs.
     *  @throws RepositoryException If something goes wrong
     *  @throws ItemNotFoundException If an Version object based on the UUID cannot be found.
     */
    private Version[] collateVersions(Property p) 
        throws ValueFormatException, RepositoryException, ItemNotFoundException
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
