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
package org.priha.version;

import java.util.ArrayList;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.priha.core.JCRConstants;
import org.priha.core.NodeImpl;
import org.priha.core.SessionImpl;
import org.priha.nodetype.QNodeDefinition;
import org.priha.nodetype.QNodeType;
import org.priha.nodetype.QNodeTypeManager;
import org.priha.util.Path;

public class VersionHistoryImpl extends NodeImpl implements VersionHistory
{
    public static VersionHistoryImpl getInstance( SessionImpl session, Path path )
        throws RepositoryException
    {
        QNodeTypeManager nt = QNodeTypeManager.getInstance();

        QNodeType versionType = nt.getNodeType(JCRConstants.Q_NT_VERSIONHISTORY);

        QNodeDefinition nDef = versionType.findNodeDefinition( path.getLastComponent() );

        return new VersionHistoryImpl( session, path, versionType, nDef, true );
    }

    public VersionHistoryImpl(SessionImpl session, Path path, QNodeType primaryType, QNodeDefinition nDef, boolean initDefaults)
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        super(session, path, primaryType, nDef, initDefaults);
    }

    public void addVersionLabel(String arg0, String arg1, boolean arg2) throws VersionException, RepositoryException
    {
        throw new UnsupportedRepositoryOperationException("addVersionLabel()");
    }

    public VersionIterator getAllVersions() throws RepositoryException
    {
        ArrayList<Version> allVersions = new ArrayList<Version>();
        Version v = getRootVersion();
        
        while( v != null )
        {
            allVersions.add( v );
            Version[] succs = v.getSuccessors();
            
            if( succs.length == 0 ) break;
            
            v = succs[0]; // Priha does not support multiple successors.
        }
        
        return new VersionIteratorImpl(allVersions);
    }

    public Version getRootVersion() throws RepositoryException
    {
        return getVersion("jcr:rootVersion");
    }

    public Version getVersion(String versionName) throws VersionException, RepositoryException
    {
        Path p = VersionManager.getVersionStoragePath( getVersionableUUID() ).resolve(getSession(),versionName);
        
        if( m_session.itemExists(p) )
        {
            return (Version) m_session.getItem( p );
        }
       
        throw new VersionException("Node "+getPath()+" has no such version "+versionName);
    }

    public Version getVersionByLabel(String versionLabel) throws RepositoryException
    {
        Node n = getNode("jcr:versionLabels");

        Property p = n.getProperty(versionLabel);
        
        String uuid = p.getString();
        
        return (Version)m_session.getNodeByUUID(uuid);
    }

    public String[] getVersionLabels() throws RepositoryException
    {
        ArrayList<String> result = new ArrayList<String>();
        Node n = getNode("jcr:versionLabels");
        
        for( PropertyIterator pi = n.getProperties(); pi.hasNext(); )
        {
            Property p = pi.nextProperty();
            
            result.add( p.getName() );
        }

        return result.toArray(new String[result.size()]);
    }

    public String[] getVersionLabels(Version arg0) throws VersionException, RepositoryException
    {
        throw new UnsupportedRepositoryOperationException("getVersionLabels(Version)");
    }

    public String getVersionableUUID() throws RepositoryException
    {
        return getProperty("jcr:versionableUuid").getString();
    }

    public boolean hasVersionLabel(String label) throws RepositoryException
    {
        String[] labels = getVersionLabels();
        
        for( String s : labels )
        {
            if( s.equals(label) ) return true;
        }
        
        return false;
    }

    public boolean hasVersionLabel(Version version, String label) throws VersionException, RepositoryException
    {
        String[] labels = getVersionLabels(version);
        
        for( String s : labels )
        {
            if( s.equals(label) ) return true;
        }
        
        return false;
    }

    public void removeVersion(String arg0)
                                          throws ReferentialIntegrityException,
                                              AccessDeniedException,
                                              UnsupportedRepositoryOperationException,
                                              VersionException,
                                              RepositoryException
    {
        throw new UnsupportedRepositoryOperationException("removeVersion(String)");
    }

    public void removeVersionLabel(String arg0) throws VersionException, RepositoryException
    {
        throw new UnsupportedRepositoryOperationException("removeVersionLabel(String)");
    }

}
