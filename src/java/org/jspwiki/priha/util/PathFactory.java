package org.jspwiki.priha.util;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 *  Gets Paths from a local storage.
 *  <p>
 *  If you possibly can, you should use PathFactory to get yourself a Path object.  This
 *  factory class stores the Paths in a WeakHashMap, and is always guaranteed to give
 *  you a good Path object.  In most cases, this is a lot faster than calling the regular
 *  {@link Path#Path(String)} constructor, since path parsing can take quite a bit of
 *  time.
 */
public class PathFactory
{
    private static WeakHashMap<String,WeakReference<Path>> c_map = new WeakHashMap<String,WeakReference<Path>>();
    
    /**
     *  Turns a String to a Path.
     *  
     *  @param path A String representing the Path
     *  @return A valid Path object.
     */
    public static Path getPath(String path)
    {
        Path result = null;
        
        WeakReference<Path> ref = c_map.get(path);
        
        if( ref != null )
        {
            result = ref.get();   
        }
        
        if( result == null )
        {
            result = new Path(path);
            
            c_map.put( path, new WeakReference<Path>(result) );
        }
        
        return result;
    }
}
