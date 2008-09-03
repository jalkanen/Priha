package org.jspwiki.priha.version;

import org.jspwiki.priha.util.Path;
import org.jspwiki.priha.util.PathFactory;

public class VersionManager
{
    private static final Path VERSIONPATH = PathFactory.getPath("/jcr:system/jcr:versionStorage");
    
    /**
     *  Returns true, if the path refers to a versioned object.
     *  @param p
     *  @return
     */
    public static boolean isVersionHistoryPath( Path p )
    {
        return VERSIONPATH.isParentOf( p );
    }
    
    /**
     *  Returns the location where the version storage is for a given UUID.
     *  
     *  @param uuid
     *  @return
     */
    public static Path getVersionStoragePath( String uuid )
    {
        String hashpath = uuid.substring(0,3) + "/" + uuid.substring(4,7) + "/" + uuid;
        Path p = PathFactory.getPath("/jcr:system/jcr:versionStorage/"+hashpath);
        
        return p;
    }
}
