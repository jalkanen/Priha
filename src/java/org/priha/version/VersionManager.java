package org.priha.version;

import java.util.UUID;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.priha.core.JCRConstants;
import org.priha.core.NodeImpl;
import org.priha.core.RepositoryImpl;
import org.priha.core.values.ValueImpl;
import org.priha.path.Path;
import org.priha.path.PathFactory;

public class VersionManager
{
    private static Path c_versionPath;

    static
    {
        try
        {
            c_versionPath = PathFactory.getPath(RepositoryImpl.getGlobalNamespaceRegistry(),
                                                "/jcr:system/jcr:versionStorage");
        }
        catch( RepositoryException e )
        {
            c_versionPath = null;
        }
    }
    
    /**
     *  Returns true, if the path refers to a versioned object.
     *  @param p
     *  @return
     */
    public static boolean isVersionHistoryPath( Path p )
    {
        return c_versionPath.isParentOf( p );
    }
    
    /**
     *  Returns the location where the version storage is for a given UUID.
     *  
     *  @param uuid
     *  @return
     * @throws RepositoryException 
     * @throws NamespaceException 
     */
    public static Path getVersionStoragePath( String uuid ) throws NamespaceException, RepositoryException
    {
        Path p;
    
        if( uuid != null )
        {
            //String hashpath = uuid.substring(0,3) + "/" + uuid.substring(4,7) + "/" + uuid;
            String hashpath = uuid;
            p = PathFactory.getPath(RepositoryImpl.getGlobalNamespaceRegistry(),
                                    "/jcr:system/jcr:versionStorage/"+hashpath);
        }
        else
        {
            p = PathFactory.getPath(RepositoryImpl.getGlobalNamespaceRegistry(),
                                    "/jcr:system/jcr:versionStorage");
        }
        return p;
    }
    
    /**
     *  Makes sure that a VersionHistory exists for a new, versionable Node.
     *  Returns quietly, if the history already exists.
     *  
     *  @param nd
     *  @throws RepositoryException 
     *  @throws UnsupportedRepositoryOperationException 
     */
    public static void createVersionHistory( NodeImpl nd ) throws UnsupportedRepositoryOperationException, RepositoryException
    {
        String uuid;

        boolean isSuper = nd.getSession().setSuper(true);
        

        try
        {
            if( !nd.getSession().hasNode( getVersionStoragePath( null ) ) )
            {
                // FIXME: A bit too complicated
                NodeImpl storage = nd.addNode( getVersionStoragePath( null ).toString(RepositoryImpl.getGlobalNamespaceRegistry()) );
                storage.getParent().save();
            }
        
            if( nd.hasProperty( JCRConstants.Q_JCR_UUID ) ) 
                uuid = nd.getUUID();
            else
                uuid = nd.setProperty( "jcr:uuid", UUID.randomUUID().toString() ).getString();
        
            Path historyPath = getVersionStoragePath( uuid );
        
            if( !nd.getSession().itemExists(historyPath) )
            {
                NodeImpl vh = nd.addNode( historyPath.toString(), "nt:versionHistory" );
            
                //
                //  Oddly enough, we need to create the UUID already here.
                //
            
                vh.setProperty( "jcr:uuid", UUID.randomUUID().toString() );
                vh.setProperty( "jcr:versionableUuid", uuid );
            
                //
                //  Create the root version.
                //
                NodeImpl root = vh.addNode( "jcr:rootVersion", "nt:version" );

                NodeImpl rootFrozen = root.addNode( "jcr:frozenNode", "nt:frozenNode" );
                
                // Mandatory properties. 
                rootFrozen.setProperty( "jcr:frozenPrimaryType", nd.getProperty( JCRConstants.Q_JCR_PRIMARYTYPE ).getValue() );
                rootFrozen.setProperty( "jcr:frozenMixinTypes", nd.getProperty( JCRConstants.JCR_MIXIN_TYPES ).getValues() );
                rootFrozen.setProperty( "jcr:frozenUuid", nd.getUUID() );
                root.setProperty( "jcr:uuid", UUID.randomUUID().toString() );

                root.setProperty( "jcr:successors", 
                                  new ValueImpl[] { }, 
                                  PropertyType.REFERENCE );
            

                //
                //  Set the reference properties back to the Node
                //
            
                nd.setProperty( "jcr:versionHistory", vh );
                nd.setProperty( "jcr:baseVersion", root );
            
                nd.setProperty( "jcr:predecessors", 
                                new ValueImpl[] { nd.getSession().getValueFactory().createValue( root ) }, 
                                PropertyType.REFERENCE );

                nd.setProperty( "jcr:isCheckedOut", true );
                
                vh.getParent().save();
            }
        }
        finally
        {
            nd.getSession().setSuper(isSuper);
        }
    }
}
