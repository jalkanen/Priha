package org.jspwiki.priha.util;

import java.util.ArrayList;

public class PathUtil
{
    /**
     * Resolves an absolute path based on a base path and then a relative path.
     * @param base
     * @param path
     * @return
     */
    public static String resolve( String base, String path )
    {
        if( base == null || base.length() == 0 || base.charAt(0) != '/' )
            throw new IllegalArgumentException("Base path is not absolute");
        
        String finalpath;
        
        if( path.startsWith("/") )
            finalpath = path;
        else
            finalpath = base + "/"+ path;
        
        return normalize(finalpath);
    }
    
    /**
     * Normalizes the given absolute path
     * @param path
     * @return
     */
    public static String normalize(String path)
    {
        String[] p = path.split("/");
        
        ArrayList<String> list = new ArrayList<String>();
        
        for( int i = 0; i < p.length; i++ )
        {
            if( p[i].equals("..") )
            {
                if( list.size() == 0 )
                {
                    throw new IllegalArgumentException("Path goes above root!");
                }
                list.remove(list.size()-1);
            }
            else if( p[i].equals(".") )
            {
                // Do nothing
            }
            else if( p[i].length() > 0 )
            {
                list.add( p[i] );
            }
        }
        
        StringBuilder sb = new StringBuilder();
        
        for( int i = 0; i < list.size(); i++ )
        {
            sb.append( "/"+list.get(i) );
        }
        
        if( sb.length() == 0 ) sb.append("/");
        
        return sb.toString();
    }
}
