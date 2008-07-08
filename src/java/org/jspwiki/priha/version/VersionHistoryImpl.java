package org.jspwiki.priha.version;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.jspwiki.priha.core.NodeImpl;
import org.jspwiki.priha.core.SessionImpl;
import org.jspwiki.priha.nodetype.GenericNodeType;
import org.jspwiki.priha.util.Path;

public class VersionHistoryImpl extends NodeImpl implements VersionHistory
{
    private List<Version> m_versions = new ArrayList<Version>();

    public static VersionHistoryImpl getInstance( SessionImpl session, Path path )
        throws RepositoryException
    {
        NodeTypeManager nt = session.getWorkspace().getNodeTypeManager();

        GenericNodeType versionType = (GenericNodeType)nt.getNodeType("jcr:versionHistory");

        NodeDefinition nDef = versionType.findNodeDefinition( path.getLastComponent() );

        return new VersionHistoryImpl( session, path, versionType, nDef );
    }

    private VersionHistoryImpl(SessionImpl session, Path path, GenericNodeType primaryType, NodeDefinition nDef)
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        super(session, path, primaryType, nDef, true);
        // TODO Auto-generated constructor stub
    }

    public void addVersionLabel(String arg0, String arg1, boolean arg2) throws VersionException, RepositoryException
    {
        // TODO Auto-generated method stub

    }

    public VersionIterator getAllVersions() throws RepositoryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Version getRootVersion() throws RepositoryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Version getVersion(String arg0) throws VersionException, RepositoryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Version getVersionByLabel(String arg0) throws RepositoryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String[] getVersionLabels() throws RepositoryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String[] getVersionLabels(Version arg0) throws VersionException, RepositoryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getVersionableUUID() throws RepositoryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean hasVersionLabel(String arg0) throws RepositoryException
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean hasVersionLabel(Version arg0, String arg1) throws VersionException, RepositoryException
    {
        // TODO Auto-generated method stub
        return false;
    }

    public void removeVersion(String arg0)
                                          throws ReferentialIntegrityException,
                                              AccessDeniedException,
                                              UnsupportedRepositoryOperationException,
                                              VersionException,
                                              RepositoryException
    {
        // TODO Auto-generated method stub

    }

    public void removeVersionLabel(String arg0) throws VersionException, RepositoryException
    {
        // TODO Auto-generated method stub

    }

}
