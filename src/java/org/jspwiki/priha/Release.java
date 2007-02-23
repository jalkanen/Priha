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
package org.jspwiki.priha;

/**
 *  Details the release information.  This class is executable, i.e.
 *  you can run
 *  <code>
 *  % java -jar priha.jar org.jspwiki.priha.Release
 *  Priha Content Repository
 *  1.0.2
 *  (C) Janne Jalkanen 2007
 *  </code>
 *  The first line is the application name, the second line is the version,
 *  and the third line is the copyright.
 *  
 *  @author jalkanen
 *
 */
public class Release
{

    public static final String APPNAME       = "Priha Content Repository";

    public static final String COPYRIGHT     = "(C) Janne Jalkanen 2007";
    
    public static final int    VERSION       = 0;
    public static final int    REVISION      = 0;
    public static final int    MINORREVISION = 7;
    
    public static final String VERSTR        = VERSION+"."+REVISION+"."+MINORREVISION;
    
    public static final void main(String[] argv)
    {
        System.out.println(APPNAME);
        System.out.println(VERSTR);
        System.out.println(COPYRIGHT);
    }
}
