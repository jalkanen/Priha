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
package org.priha.util;

import java.util.regex.Pattern;

public final class TextUtil
{

    /**
     *  Replaces a string with an other string.
     *
     *  @param orig Original string.  Null is safe.
     *  @param src  The string to find.
     *  @param dest The string to replace <I>src</I> with.
     */
    public final static String replaceString( String orig, String src, String dest )
    {
        if ( orig == null ) return null;
        if ( src == null || dest == null ) throw new NullPointerException();
        if ( src.length() == 0 ) return orig;
    
        StringBuilder res = new StringBuilder();
        int start, end = 0, last = 0;
    
        while ( (start = orig.indexOf(src,end)) != -1 )
        {
            res.append( orig.substring( last, start ) );
            res.append( dest );
            end  = start+src.length();
            last = start+src.length();
        }
    
        res.append( orig.substring( end ) );
    
        return res.toString();
    }

    /**
     *  Turns a JCR pattern into a java.util.regex.Pattern
     *
     *  @param namePattern
     *  @return
     */
    public static Pattern parseJCRPattern(String namePattern)
    {
        namePattern = replaceString( namePattern, "*", ".*" );
        namePattern = "^("+namePattern +")$";
        Pattern p = Pattern.compile( namePattern );
        return p;
    }

}
