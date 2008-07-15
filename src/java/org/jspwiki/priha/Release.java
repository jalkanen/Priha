/*
    Priha - A JSR-170 implementation library.

    Copyright (C) 2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

    public static final String COPYRIGHT     = "(C) Janne Jalkanen 2007-2008";

    public static final int    VERSION       = 0;
    public static final int    REVISION      = 0;
    public static final int    MINORREVISION = 16;

    public static final String VERSTR        = VERSION+"."+REVISION+"."+MINORREVISION;

    public static final void main(String[] argv)
    {
        System.out.println(APPNAME);
        System.out.println(VERSTR);
        System.out.println(COPYRIGHT);
    }
}
