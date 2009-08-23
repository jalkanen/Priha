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
package org.priha.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.xml.namespace.QName;

import org.priha.core.namespace.NamespaceMapper;

/**
 *  Manages paths, which are a key ingredient in JCR.  A Path is an immutable
 *  object, so you can't change it once you create it. 
 */
public final class Path implements Comparable<Path>, Serializable
{
    private static final long serialVersionUID = 2L;

    /**
     *  This is a static instance of the root path so that you don't have
     *  to create it every single time you use it (as it happens quite often).
     *  <p>
     *  Using this is faster than using <code>Path p = new Path("/")</code>.
     */
    public static final Path         ROOT = new Path("/");

    private final       Component[]  m_components;

    private boolean                  m_isAbsolute = false;

    private transient String         m_cachedString;

    private transient Path           m_cachedParentPath;
    
    /** This constructor is useful only to subclasses or serialization. */
    protected Path()
    {
        m_components = new Component[0];
    }
    
    /** Use only internally when you know you don't have namespaces. */
    private Path(String abspath)
    {
        if( abspath.length() > 0 && abspath.charAt(0) == '/' ) m_isAbsolute = true;

        Component[] c = null;
        try
        {
            c = parsePath( null, abspath );
        }
        catch(Exception e) {}
        
        m_components = c;
    }
    
    /**
     *  Creates a Path from a number of QName components.  The index
     *  of all the components is assumed to be 1.
     *  
     *  @param components A list of components
     *  @param absolute true, if this path should be absolute; false if relative.
     */
    public Path( QName[] components, boolean absolute )
    {
        Component[] comps = new Component[components.length];
        
        for( int i = 0; i < components.length; i++ )
        {
            comps[i] = new Component(components[i]);
        }
        m_components = comps;
        m_isAbsolute = absolute;
    }

    public Path( Component[] components, boolean absolute )
    {
        m_components = components;
        m_isAbsolute = absolute;
    }

    public final boolean isAbsolute()
    {
        return m_isAbsolute;
    }
    
    /**
     *  Create a new path from a String.  E.g.
     *  <code>
     *  Path p = new Path("/foo/bar/glob");
     *  </code>
     * @param abspath A path.
     * @throws RepositoryException 
     * @throws NamespaceException 
     */
    public Path( NamespaceMapper ns, String abspath ) throws NamespaceException, RepositoryException
    {
        if( abspath.length() > 0 && abspath.charAt(0) == '/' ) m_isAbsolute = true;

        m_components = parsePath( ns, abspath );
    }

    public Path( NamespaceMapper ns, String pathStart, Path pathEnd ) throws NamespaceException, RepositoryException
    {
        if( pathStart.length() > 0 && pathStart.charAt(0) == '/' ) m_isAbsolute = true;
        Component[] start = parsePath( ns, pathStart );
        
        m_components = new Component[start.length+pathEnd.depth()];
        
        System.arraycopy( start, 0, m_components, 0, start.length );
        /*
        for( int i = 0; i < start.length; i++ )
        {
            m_components[i] = start[i];
        }
        */
        System.arraycopy( pathEnd.m_components, 0, m_components, start.length, pathEnd.depth() );
        /*
        for( int i = 0; i < pathEnd.depth(); i++ )
        {
            m_components[i+start.length] = pathEnd.m_components[i];
        }
        */
    }

    public Path(QName name, boolean b)
    {
        this( new QName[] { name }, b );
    }

    public Path( Path parentPath, Component component )
    {
        m_isAbsolute = parentPath.m_isAbsolute;
        m_components = new Component[parentPath.getElements().length+1];
        
        System.arraycopy( parentPath.m_components, 0, m_components, 0, parentPath.m_components.length );
        /*
        for( int i = 0; i < parentPath.m_components.length; i++ )
        {
            m_components[i] = parentPath.m_components[i];
        }
        */
        m_components[m_components.length-1] = component;
    }

    /**
     *  In Priha context, any path component ending with [1] is always
     *  treated as the component itself, as per JCR-170 4.3.1.
     *  
     *  @param s Component name to clean
     *  @return A new String.
     * @throws RepositoryException 
     * @throws NamespaceException 
     */
    private Component cleanComponent(NamespaceMapper ns, String s) throws NamespaceException, RepositoryException
    {
        int index = 1;
        int bracket = s.indexOf('[');
      
        if( bracket != -1 )
        {
            String sidx = s.substring(bracket+1, s.length()-1);
            index = Integer.parseInt(sidx);
            s = s.substring( 0, bracket );
        }
        
        QName q;
        
        if( ns != null && s.indexOf( '{' ) == -1 )
        {
            q = ns.toQName( s );
        }
        else
        {
            q = QName.valueOf( s );
        }
        
        return new Component( q, index );
    }
    
    private Component[] parsePath( NamespaceMapper ns, String path ) throws NamespaceException, RepositoryException
    {
        ArrayList<Component> ls = new ArrayList<Component>();
        StringBuilder sb = new StringBuilder(32); // Just a guess
        
        for( int i = 0; i < path.length(); i++ )
        {
            char ch = path.charAt( i );
            
            if( ch == '{' )
            {
                int end = path.indexOf( '}', i+1 );
                sb.append( path.substring( i, end+1 ) );
                i = end;
            }
            else if( ch == '/' )
            {
                if( sb.length() > 0 )
                {
                    ls.add( cleanComponent(ns,sb.toString()) );
                    sb.delete( 0, sb.length() );
                }
            }
            else
            {
                sb.append( ch );
            }
        }
        
        if( sb.length() > 0 )
            ls.add( cleanComponent(ns, sb.toString()) );
        
        return ls.toArray( new Component[ls.size()] );
    }
    /**
     *  Gets one path component.
     * @param idx Which component to get.  The top-most component is at index zero.
     * @return The component.
     */
    public final Component getComponent( int idx )
    {
        return m_components[idx];
    }

    /**
     *  Returns the name of the last component of the path (i.e. the name)
     * @return The name.  If this is the root, returns "" (empty string).
     */
    public final Component getLastComponent()
    {
        if( isRoot() )
        {
            return Component.ROOT_COMPONENT;
        }
        return m_components[depth()-1];
    }

    /**
     *  Returns true, if this Path represents the root.
     *  @return True, if this Path is the root.
     */
    public final boolean isRoot()
    {
        return depth() == 0;
    }

    /**
     *  Returns the depth of this path.  Root is zero.
     *  @return The depth of the path.
     */
    public final int depth()
    {
        return m_components.length;
    }

    /**
     *  Returns the name of the parent (not the path).
     *  @return String describing the name of the parent.
     *  @throws InvalidPathException If you try to get the parent of the root node.
     */
    public final Component getParentName()
        throws InvalidPathException
    {
        if( isRoot() ) throw new InvalidPathException("Root has no parent");
        return m_components[depth()-1];
    }

    /**
     *  Returns a valid path pointing at the parent of this path.
     *
     *  @return A new Path object.
     *  @throws InvalidPathException If this Path is the root node.
     */
    public final Path getParentPath()
        throws InvalidPathException
    {
        if( m_cachedParentPath != null )
        {
            return m_cachedParentPath;
        }
        
        if( isRoot() ) throw new InvalidPathException("Root has no parent");
        
        m_cachedParentPath = getSubpath( 0, depth()-1 );
        
        return m_cachedParentPath;
    }

    /**
     *  Returns a subpath starting from index "startidx".  Start from zero
     *  to get a clone of this Path.
     *
     * @param startidx Where to start the path from.  Zero is root.
     * @return A valid Path.
     * @throws InvalidPathException If startidx < 0 or startidx > path depth.
     */
    public final Path getSubpath( int startidx )
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
    public final Path getSubpath( int startidx, int endidx )
        throws InvalidPathException
    {
        if( startidx > depth() || endidx > depth() )
        {
            throw new InvalidPathException("Supplied index deeper than the path");
        }
        if( startidx < 0 || endidx < 0 ) throw new InvalidPathException("Negative index");

        Component[] components = new Component[endidx-startidx];
        
        System.arraycopy( m_components, startidx, components, 0, endidx-startidx );
        /*
        for( int i = startidx; i < endidx; i++ )
        {
            components[i-startidx] = m_components[i];
        }
         */
        Path newpath = new Path( components, startidx == 0 ? m_isAbsolute : false );

        return newpath;
    }

    /**
     *  Updates the internal string representation cache.
     *
     */
    private void update()
    {
        StringBuilder sb = new StringBuilder( m_components.length * 16 );
        if( m_isAbsolute ) sb.append("/");

        for( Component c : m_components )
        {
            sb.append( c );
            sb.append("/");
        }

        if( depth() > 0 ) sb.deleteCharAt(sb.length()-1); // Remove final "/"

        //
        //  TODO: In theory, some performance/memory gain could be received by
        //  using String.intern() here.  However, in testing it looks like
        //  the penalty of using intern() [the cache lookups] seems to offset
        //  any speed gain in equals() and, in fact, make regular ops slower.  
        //  More testing is required to determine whether the memory savings are 
        //  worth the penalty.
        //
        m_cachedString = sb.toString();
    }

    /**
     *  Returns the Path in String format.
     *
     */
    public final String toString()
    {
        if( m_cachedString == null )
        {
            update();
        }

        return m_cachedString;
    }
    
    public final String toString( NamespaceMapper ns ) throws NamespaceException, RepositoryException
    {
        StringBuilder sb = new StringBuilder();
        
        if( isAbsolute() ) sb.append("/");
        
        for( Component q : m_components )
        {
            sb.append( ns.fromQName( q ) );
            if( q.getIndex() != 1 ) sb.append('[').append(q.getIndex()).append(']');
            sb.append( '/' );
        }
        
        if( depth() > 0 )
            sb.deleteCharAt( sb.length() - 1 );
        
        return sb.toString();
    }

    /**
     *  Adds a component to the path (since Components are not paths),
     *  and returns a new Path.
     *  
     *  @param component Component to add at the end of the Path.
     *  @return A new Path with the component added.
     */
    public final Path resolve( QName component )
    {
        return new Path( this, new Component(component) );
    }
    
    /**
     *  Resolves a relative Path against this Path.
     *
     *  @param relPath String describing relative path.
     *  @return A valid Path.
     * @throws RepositoryException 
     * @throws NamespaceException 
     */
    public final Path resolve(NamespaceMapper ns, String relPath) throws NamespaceException, RepositoryException
    {
        //
        //  Speedup for the most common case.
        //
        if( relPath.indexOf('/') == -1 ) 
            //return new Path(this, cleanComponent( ns, relPath ) );
            return PathFactory.getPath( ns, toString(ns)+"/"+relPath );
        
        ArrayList<Component> p    = new ArrayList<Component>();
        ArrayList<Component> list = new ArrayList<Component>();

        if( !relPath.startsWith("/") )
        {
            p.addAll( Arrays.asList(m_components) );
        }
        
        p.addAll( Arrays.asList(parsePath(ns, relPath)) );

        for( int i = 0; i < p.size(); i++ )
        {
            String lp = p.get(i).getLocalPart();
            if( lp.equals("..") )
            {
                if( list.size() == 0 )
                {
                    throw new IllegalArgumentException("Path goes above root!");
                }
                list.remove(list.size()-1);
            }
            else if( lp.equals(".") )
            {
                // Do nothing
            }
            else if( lp.length() > 0 )
            {
                list.add( p.get(i) );
            }
        }

        return new Path( list.toArray(new Component[list.size()]), m_isAbsolute );
    }

    /**
     *  Returns the Path up to ancestor "depth".
     *
     *  @param depth
     *  @return
     *  @throws InvalidPathException
     */
    public final Path getAncestorPath(int depth) throws InvalidPathException
    {
        return getSubpath(0,depth);
    }

    /**
     *  Returns true, if this path is a parent of the given Path.
     *  Root is the parent of all other paths.
     *  
     *  @param path
     *  @return
     */
    public final boolean isParentOf( Path p )
    {
        if( p.depth() > depth() )
        {
            for( int i = 0; i < m_components.length; i++ )
            {
                if( !m_components[i].equals(p.m_components[i]) ) 
                    return false;
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     *  Two paths are equal if their string representations are equal.
     */
    @Override
    public final boolean equals(Object obj)
    {
        if( obj == this ) return true;
        if( obj instanceof Path )
        {
            return ((Path)obj).toString().equals( toString() );
        }
        return false;
    }

    @Override
    public final int hashCode()
    {
        return toString().hashCode() + 13;
    }

    public final int compareTo(Path o)
    {
        return toString().compareTo( o.toString() );
    }

    public final Component[] getElements()
    {
        return m_components;
    }
    
    /**
     *  A Path component consists of a QName with an optional index (to support
     *  same name siblings).
     *  <p>
     *  This class also stores a rendered version of its own name internally
     *  for speed purposes.
     */
    public static class Component extends QName implements Serializable
    {
        /**
         *  Name of the root component.
         */
        public static final Component ROOT_COMPONENT = new Component("");

        private static final long serialVersionUID = 8038593715235147912L;

        private int m_index = 1;
        private transient String m_cachedString;
        
        public Component(String localPart)
        {
            super(localPart);
        }

        public Component(String namespaceURI, String localpart)
        {
            super( namespaceURI, localpart );
        }
        
        public Component(String namespaceURI, String localPart, String prefix )
        {
            super( namespaceURI, localPart, prefix );
        }
        
        public Component(QName name)
        {
            super( name.getNamespaceURI(), name.getLocalPart(), name.getPrefix() );
        }
        
        public Component(QName name, int index )
        {
            this(name);
            m_index = index;
        }

        public final int getIndex()
        {
            return m_index;
        }
        
        /**
         *  Returns the QName String representation of the Component, including the index.
         */
        public final String toString()
        {
            if( m_cachedString == null )
            {
                StringBuilder sb = new StringBuilder();
                sb.append(super.toString());
                if( m_index != 1 )
                {
                    sb.append("[").append(m_index).append("]");
                }
                m_cachedString = sb.toString();
            }
            
            return m_cachedString;
        }
        
        /**
         *  Returns the String representation of the Component, including the index,
         *  using a NamespaceMapper.
         *  <p>
         *  The difference between using ns.fromQName(component) and component.toString(ns)
         *  is that the first one will NOT return the index.  Sometimes this may be
         *  desireable.
         */
        public final String toString(NamespaceMapper ns) throws NamespaceException
        {
            return ns.fromQName( this ) + ((m_index != 1) ? "["+m_index+"]" : "");
        }
    }
}
