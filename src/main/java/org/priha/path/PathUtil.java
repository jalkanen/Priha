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
package org.priha.path;

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

    private static final String ONECHARSIMPLENAME = "./:[]*'\"|";
    private static final String NONSPACE = "/:[]*'\"| \t\r\n";
    
    /**
     *  Validates a path so that it valid according to JCR 4.6.
     *  
     *  @param value
     *  @throws InvalidPathException If the path is not valid.
     */
    // FIXME: This is not complete.
    public static void validatePath(String value) throws InvalidPathException
    {
        int start=0;
        int end;
        while( (end = value.indexOf('/',start)) != -1 )
        {
            String component = value.substring(start,end);
            validateComponent(component);
            
            start = end+1;
        }
        
        validateComponent(value.substring(start));
    }

    private static void validateComponent(String component) throws InvalidPathException
    {
        if( component.length() == 1 )
        {
            if( ONECHARSIMPLENAME.indexOf(component) != -1 ) throw new InvalidPathException("Contained one of the illegal characters "+ONECHARSIMPLENAME);
        }
        else if( component.length() == 2 )
        {
            if( component.charAt(0) == '.' )
            {
                if( ONECHARSIMPLENAME.indexOf(component.charAt(1)) != -1 )
                    throw new InvalidPathException("Second character was one of the illegal characters "+ONECHARSIMPLENAME);
            }
            else if( component.charAt(1) == '.' )
            {
                if( ONECHARSIMPLENAME.indexOf(component.charAt(0)) != -1 )
                    throw new InvalidPathException("First character was one of the illegal characters "+ONECHARSIMPLENAME);
                
            }
            else if( ONECHARSIMPLENAME.indexOf(component.charAt(0)) != -1 ||
                ONECHARSIMPLENAME.indexOf(component.charAt(1)) != -1 )
            {    
                    throw new InvalidPathException("Two-character path contained an illegal character "+ONECHARSIMPLENAME);
            }
        }
        else
        {
            boolean hasColon = false;
            
            for( int i = 0; i < component.length(); i++ )
            {
                char ch = component.charAt(i);
            
                if( NONSPACE.indexOf(ch) != -1 )
                {
                    if( ch == ' ' && (i != 0 || i != component.length()) )
                        continue;

                    if( ch == ':' )
                    {
                        if( hasColon ) throw new InvalidPathException("More than one colon");
                        hasColon = true;
                        
                        String prefix = component.substring(0,i);
                        
                        if( prefix.length() == 0 ) throw new InvalidPathException("Zero-length prefix");
                        continue;
                    }
                    
                    throw new InvalidPathException("Illegal whitespace");
                }
            }
        }
    }
}
