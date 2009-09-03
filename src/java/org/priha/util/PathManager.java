package org.priha.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.WeakHashMap;

import javax.jcr.PathNotFoundException;

/**
 *  PathManager stores all the Paths within a Session.
 */
public class PathManager
{
    private WeakHashMap<PathRef,Path> m_pathMap = new WeakHashMap<PathRef,Path>();
    private HashMap<Path,WeakPathRef> m_reversePathMap = new HashMap<Path,WeakPathRef>();
    private ReferenceQueue<PathRef> m_queue = new ReferenceQueue<PathRef>();
    
    public PathManager()
    {   
    }
        
    public Path getPath(PathRef p) throws PathNotFoundException
    {
        emptyStaleEntries();
        Path path = m_pathMap.get( p );
        
        if( path == null )
        {
            throw new PathNotFoundException("Path not found for reference "+p);
        }
        
        return path;
    }
    
    public PathRef getPathRef( Path path )
    {
        emptyStaleEntries();
        PathRef ref;
        WeakReference<PathRef> w = m_reversePathMap.get( path );
        
        if( w != null ) 
        {
            ref = w.get();
            
            if( ref != null ) return ref; 
        }
        
        ref = makeReference(path);
        
        //System.out.println("Adding "+ref+" = "+path);
        m_reversePathMap.put( path, new WeakPathRef(ref,path,m_queue) );
        m_pathMap.put( ref, path );
        
        return ref;
    }

    public void move( Path oldPath, Path newPath )
    {
        PathRef pr = getPathRef( oldPath );
        if( pr != null )
        {
            m_pathMap.put( pr, newPath );
            m_reversePathMap.put( newPath, new WeakPathRef(pr,newPath,m_queue) );
            m_reversePathMap.remove( oldPath );
        }
        else
        {
            throw new RuntimeException("Oldpath does not exist!?! "+oldPath);
        }
    }
    
    private void emptyStaleEntries()
    {
        /*
        System.out.println("-------------");
        System.out.println("pathMap.size() "+m_pathMap.size());
        System.out.println("reversePathMap.size() "+m_reversePathMap.size());
         */
        WeakPathRef r;
        while( (r = (WeakPathRef)m_queue.poll()) != null )
        {
            Path p = r.getPath();
            
            m_reversePathMap.remove( p );
            
            //System.out.println("   Expunged "+p);
        }
        /*
        System.out.println("pathMap.size() "+m_pathMap.size());
        System.out.println("reversePathMap.size() "+m_reversePathMap.size());
        */
    }
    
    // FIXME: I don't quite know what happens when this overflows...
    private static int c_counter = Integer.MIN_VALUE;
    
    private PathRef makeReference(Path path)
    {
        return new PathRef(c_counter++);
    }
    
    private static class WeakPathRef extends WeakReference<PathRef>
    {
        private Path m_path;
        
        public WeakPathRef( PathRef referent,Path path, ReferenceQueue<PathRef> queue )
        {
            super( referent, queue );
            m_path = path;
        }
        
        public final Path getPath()
        {
            return m_path;
        }
    }

    /**
     *  Creates an unique reference to a Path, that is, a PathRef which is not
     *  shared.  This is used in the case where you wish to retain an existing
     *  reference and make sure it does not change if the underlying Path 
     *  changes.  
     *  
     *  @param pathref A pathref to clone
     *  @return A new PathRef which is not shared.
     *  @throws PathNotFoundException If this pathref does not refer to a valid Path.
     */
    public PathRef getUniquePathRef( PathRef pathref ) throws PathNotFoundException
    {
        Path p = getPath( pathref );
        
        PathRef newRef = new PathRef(c_counter++);
        m_pathMap.put( newRef, p );
        
        return newRef;
    }
}