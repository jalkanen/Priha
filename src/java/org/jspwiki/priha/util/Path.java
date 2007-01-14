/* 
    Priha - A JSR-170 implementation library.

    Copyright (C) 2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jspwiki.priha.util;

import java.util.ArrayList;
import java.util.List;

/**
 *  Manages paths, which are a key ingredient in JCR.
 *  
 *  @author jalkanen
 */
public class Path
{
    private List<String> m_components;
    
    private boolean m_isAbsolute = false;
    
    private String  m_cachedString;
    
    protected Path( List<String> components, boolean absolute )
    {
        m_components = new ArrayList<String>();
        m_components.addAll(components);
        m_isAbsolute = absolute;
        update();
    }
    
    /**
     *  Create a new path from a String.  E.g.
     *  <code>
     *  Path p = new Path("/foo/bar/glob");
     *  </code>
     * @param abspath A path.
     */
    public Path( String abspath )
    {
        if( abspath.charAt(0) == '/' ) m_isAbsolute = true;

        m_components = parsePath( abspath );
        update();
    }
    
    private List<String> parsePath( String path )
    {
        ArrayList<String> ls = new ArrayList<String>();
        
        int start = 0, end = 0;
        while( (end = path.indexOf('/',start)) != -1 )
        {
            String component = path.substring( start, end );
            start = end+1;
            if( component.length() > 0 ) ls.add(component);
        }
        
        if( start < path.length() )
            ls.add(path.substring(start)); // Add the final component

        ls.trimToSize();
        
        return ls;
    }
    /**
     *  Gets one path component.
     * @param idx Which component to get.  The top-most component is at index zero.
     * @return The component.
     */
    public String getComponent( int idx )
    {
        return m_components.get(idx);
    }
    
    /**
     *  Returns the name of the last component of the path (i.e. the name)
     * @return The name.  If this is the root, returns "" (empty string).
     */
    public String getLastComponent()
    {
        if( isRoot() )
        {
            return "";
        }
        return m_components.get(depth()-1);
    }
    
    /**
     *  Returns true, if this Path represents the root.
     *  @return True, if this Path is the root.
     */
    public final boolean isRoot()
    {
        return m_components.size() == 0;
    }
    
    /**
     *  Returns the depth of this path.  Root is zero.
     *  @return The depth of the path.
     */
    public final int depth()
    {
        return m_components.size();
    }
    
    /**
     *  Returns the name of the parent (not the path).
     *  @return String describing the name of the parent.
     *  @throws InvalidPathException If you try to get the parent of the root node.
     */
    public String getParentName()
        throws InvalidPathException
    {
        if( isRoot() ) throw new InvalidPathException("Root has no parent");
        return m_components.get(depth()-1);
    }
    
    /**
     *  Returns a valid path pointing at the parent of this path.
     *  
     *  @return A new Path object.
     *  @throws InvalidPathException If this Path is the root node.
     */
    public Path getParentPath()
        throws InvalidPathException
    {
        if( isRoot() ) throw new InvalidPathException("Root has no parent");
        List<String> list = m_components.subList( 0, 
                                                  m_components.size()-1 );
        
        return new Path( list, m_isAbsolute );
    }
    
    /**
     *  Returns a subpath starting from index "startidx".  Start from zero
     *  to get a clone of this Path.
     *  
     * @param startidx Where to start the path from.  Zero is root.
     * @return A valid Path.
     * @throws InvalidPathException If startidx < 0 or startidx > path depth.
     */
    public Path getSubpath( int startidx )
        throws InvalidPathException
    {
        return getSubpath( startidx, depth() );
    }

    /**
     *  Gets a valid subpath starting from startidx and ending at endidx.
     * @param startidx Which component to start from.
     * @param endidx   Which component is the last one to include.
     * @return A valid Path
     * @throws InvalidPathException If the index values are invalid.
     */
    public Path getSubpath( int startidx, int endidx )
        throws InvalidPathException
    {
        if( startidx > depth() || endidx > depth() )
        {
            throw new InvalidPathException("Supplied index deeper than the path");
        }
        if( startidx < 0 || endidx < 0 ) throw new InvalidPathException("Negative index");
    
        List<String> list = m_components.subList( startidx, 
                                                  endidx );
        Path newpath = new Path( list, startidx == 0 ? m_isAbsolute : false );
    
        return newpath;
    }

    /**
     *  Updates the internal string representation cache.
     *
     */
    private void update()
    {
        StringBuilder sb = new StringBuilder();
        if( m_isAbsolute ) sb.append("/");
    
        for( String c : m_components )
        {
            sb.append( c );
            sb.append("/");
        }
    
        if( depth() > 0 ) sb.deleteCharAt(sb.length()-1); // Remove final "/"
        
        m_cachedString = sb.toString();        
    }
    
    /**
     *  Returns the Path in String format.
     *  
     */
    public String toString()
    {
        if( m_cachedString == null ) 
        {
            update();
        }
        
        return m_cachedString;
    }

    /**
     *  Resolves a relative Path against this Path.
     *  
     *  @param relPath String describing relative path.
     *  @return A valid Path.
     */
    public Path resolve(String relPath)
    {
        ArrayList<String> p    = new ArrayList<String>();
        ArrayList<String> list = new ArrayList<String>();
        
        if( !relPath.startsWith("/") )
            p.addAll( m_components );

        p.addAll( parsePath(relPath) );
        
        for( int i = 0; i < p.size(); i++ )
        {
            if( p.get(i).equals("..") )
            {
                if( list.size() == 0 )
                {
                    throw new IllegalArgumentException("Path goes above root!");
                }
                list.remove(list.size()-1);
            }
            else if( p.get(i).equals(".") )
            {
                // Do nothing
            }
            else if( p.get(i).length() > 0 )
            {
                list.add( p.get(i) );
            }
        }

        list.trimToSize();
        
        return new Path( list, m_isAbsolute );
    }

    /**
     *  Returns the Path up to ancestor "depth".
     *  
     *  @param depth
     *  @return
     *  @throws InvalidPathException
     */
    public Path getAncestorPath(int depth) throws InvalidPathException
    {
        return getSubpath(0,depth);
    }
}
