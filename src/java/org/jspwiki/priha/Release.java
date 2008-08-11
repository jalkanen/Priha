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
package org.jspwiki.priha;

/**
 *  Details the release information.  This class is executable, i.e.
 *  you can run
 *  <pre>
 *  % java -jar priha.jar org.jspwiki.priha.Release
 *  Priha Content Repository
 *  1.0.2
 *  (C) Janne Jalkanen 2007-2009
 *  </pre>
 *  The first line is the application name, the second line is the version,
 *  and the third line is the copyright.
 *  <p>
 *  You may also use the argument "-versiononly" which will only print
 *  the version string.  E.g.
 *
 *  <pre>
 *  % java -jar priha.jar org.jspwiki.priha.Release -versiononly
 *  1.0.2
 *  </pre>
 *
 */
public class Release
{
    /**
     *  The application name.  Value is {@value}.
     */
    public static final String APPNAME       = "Priha Content Repository";

    /**
     *  The copyright string.  Value is {@value}.
     */
    public static final String COPYRIGHT     = "(C) Janne Jalkanen 2007-2008";

    /**
     *  The current major version.
     */
    public static final int    VERSION       = 0;
    
    /**
     *  The current minor version.
     */
    public static final int    REVISION      = 0;
    
    /**
     *  The current minor revision.
     */
    public static final int    MINORREVISION = 28;

    /**
     *  The version string of the form version.revision.minorrevision.  At
     *  the time of the generation of this documentation, it was {@value}.
     */
    public static final String VERSTR        = VERSION+"."+REVISION+"."+MINORREVISION;

    /**
     *  A static method which can be run to print the version information.
     */
    public static final void main(String[] argv)
    {
        if( argv.length > 0 && argv[0].equals("-versiononly") )
            System.out.println(VERSTR);
        else
        {
            System.out.println(APPNAME);
            System.out.println(VERSTR);
            System.out.println(COPYRIGHT);
        }
    }
}
