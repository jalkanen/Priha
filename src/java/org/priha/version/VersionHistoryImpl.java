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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.priha.core.JCRConstants;
import org.priha.core.SessionImpl;
import org.priha.nodetype.QNodeDefinition;
import org.priha.nodetype.QNodeType;
import org.priha.nodetype.QNodeTypeManager;
import org.priha.path.Path;

public class VersionHistoryImpl extends AbstractVersion implements VersionHistory
{
    private static final String JCR_VERSIONLABELS = "jcr:versionLabels";

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

    /**
     *  Adds a Version Label.  The labels are stored as Properties of the VersionHistory's "jcr:versionLabels" Node.
     *  Each Property is named
     */
    public void addVersionLabel(String versionName, String label, boolean moveLabel) throws VersionException, RepositoryException
    {
        SessionImpl session = m_session.getRepository().superUserLogin( m_session.getWorkspace().getName() );
        
        try
        {
            VersionHistory me = (VersionHistory)session.getItem(getInternalPath());
            
            if( me.hasVersionLabel(label) && !moveLabel )
            {
                throw new VersionException("Attempted to add a new label to a versionhistory, but it already existed.");
            }
            
            Node labels;
            
            try
            {
                labels = me.getNode(JCR_VERSIONLABELS);
            }
            catch( PathNotFoundException e )
            {
                labels = me.addNode(JCR_VERSIONLABELS);
            }
            
            String uuid = me.getVersion(versionName).getUUID();
            
            labels.setProperty( label, uuid );
            
            session.save();
        }
        finally
        {
            session.logout();
        }
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
        Node n = getNode(JCR_VERSIONLABELS);

        Property p = n.getProperty(versionLabel);
        
        String uuid = p.getString();
        
        return (Version)m_session.getNodeByUUID(uuid);
    }

    public String[] getVersionLabels() throws RepositoryException
    {
        ArrayList<String> result = new ArrayList<String>();
        
        try
        {
            Node n = getNode(JCR_VERSIONLABELS);
        
            for( PropertyIterator pi = n.getProperties(); pi.hasNext(); )
            {
                Property p = pi.nextProperty();
            
                if( p.getName().contains(":") ) continue; // Let's skip all namespaced beasts here.
                
                result.add( p.getName() );
            }
        }
        catch( PathNotFoundException e ) {}

        return result.toArray(new String[result.size()]);
    }

    public String[] getVersionLabels(Version v) throws VersionException, RepositoryException
    {
        ArrayList<String> res = new ArrayList<String>();

        try
        {
            if( !v.getContainingHistory().getUUID().equals(getUUID()) )
            {
                throw new VersionException("This version does not belong to this history.");
            }
        }
        catch( UnsupportedRepositoryOperationException e )
        {
        }
        
        try
        {
            String uuid = v.getUUID();
        
            Node labels = getNode(JCR_VERSIONLABELS);
        
            for( PropertyIterator pi = labels.getProperties(); pi.hasNext(); )
            {
                Property p = pi.nextProperty();
            
                if( p.getName().contains(":") ) continue; // Let's skip all namespaced beasts here.
            
                if( p.getValue().getString().equals(uuid) )
                    res.add( p.getName() );
            }
        }
        catch( PathNotFoundException e ) {}
        
        return res.toArray(new String[0]);
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


    @Override
    public void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        System.out.println("VersionHistory.remove("+getPath()+")");
        // First, we'll remove all the children
        
        List<Version> toberemoved = new ArrayList<Version>();
        
        VersionIterator i = getAllVersions();
        
        while( i.hasNext() )
        {
            Version v = i.nextVersion();
            
            toberemoved.add( v );
        }
        
        for( Version v : toberemoved ) 
        {
            System.out.println("  Removing version "+v.getName());
            try
            {
                v.remove();
            }
            catch(Exception ex) { ex.printStackTrace(); }
        }
        
        System.out.println("Remove version history "+this);
        // Then we remove the Node itself.
        super.remove();
    }

    public void removeVersionLabel(String label) throws VersionException, RepositoryException
    {
        SessionImpl session = m_session.getRepository().superUserLogin(m_session.getWorkspace().getName());
        
        try
        {
            //
            //  Any PathNotFoundException from these gets turned into a VersionException
            //
            VersionHistory me = (VersionHistory) session.getItem(getInternalPath());
            
            Node labels = me.getNode(JCR_VERSIONLABELS);
            
            Property p = labels.getProperty(label);
            
            p.remove();
            
            session.save();
        }
        catch( PathNotFoundException e ) 
        {
            throw new VersionException("Label "+label+" does not exist for this version history.");
        }
        finally
        {
            session.logout();
        }
    }

}
