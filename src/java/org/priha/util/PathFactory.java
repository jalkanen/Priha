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
package org.priha.util;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;

import org.priha.core.namespace.NamespaceMapper;

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
    private static WeakHashMap<String,WeakReference<Path>> c_map = new WeakHashMap<String,WeakReference<Path>>(128);
    private static WeakHashMap<Path,String> c_reverseMap = new WeakHashMap<Path,String>(128);
    
    /**
     *  This method clears up the PathFactory cache maps.  It is mandatory that this
     *  method is called every time when you change namespace mappings, otherwise the results
     *  of getPath() might no longer be correct.
     */
    public static void reset()
    {
        c_map.clear();
        c_reverseMap.clear();
    }
    
    /**
     *  Turns a String to a Path.
     *  
     *  @param path A String representing the Path
     *  @return A valid Path object.
     * @throws RepositoryException 
     * @throws NamespaceException 
     */
    public static Path getPath(NamespaceMapper ns, String path) throws NamespaceException, RepositoryException
    {
        Path result = null;
        WeakReference<Path> ref = c_map.get(path);
        
        if( ref != null )
        {
            result = ref.get();   
        }
        
        if( result == null )
        {
            result = new Path(ns,path);
    
            c_map.put( path, new WeakReference<Path>(result) );
            c_reverseMap.put( result, path );
        }

        return result;
    }

    public static String getMappedPath(NamespaceMapper ns, Path path) throws NamespaceException, RepositoryException
    {
        String p = c_reverseMap.get( path );
        
        if( p == null )
        {
            p = path.toString(ns);
            
            c_reverseMap.put( path, p );
            c_map.put( p, new WeakReference<Path>(path) );
        }
            
        return p;
    }
    
    /**
     *  Turns a FQN-representation of a Path into a real Path.
     *  
     *  @param property
     *  @return
     * @throws NamespaceException, RepositoryException 
     * @throws  
     * @throws NamespaceException 
     */
    public static Path getPath(String property) throws NamespaceException, RepositoryException
    {
        Path p = new Path(null,property);
        
        return p;
    }
}
