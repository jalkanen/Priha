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
package org.priha;

import javax.jcr.Repository;

/**
 *  Details the release information.  This class is executable, i.e.
 *  you can run
 *  <pre>
 *  % java -jar priha.jar org.priha.Release
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
 *  % java -jar priha.jar org.priha.Release -versiononly
 *  1.0.2
 *  </pre>
 *
 */
public final class Release
{
    /**
     *  The application name.  Value is {@value}.
     */
    public static final String APPNAME       = "Priha Content Repository";

    /**
     *  The copyright string.  Value is {@value}.
     */
    public static final String COPYRIGHT     = "(C) Janne Jalkanen 2007-2009";

    /**
     *  The current major version.
     */
    public static final int    VERSION       = 0;
    
    /**
     *  The current minor version.
     */
    public static final int    REVISION      = 7;
    
    /**
     *  The current minor revision.
     */
    public static final int    MINORREVISION = 0;

    /**
     *  E.g. "alpha" or "beta".
     */
    public static final String POSTFIX       = "svn";

    /**
     *  The version string of the form version.revision.minorrevision.  At
     *  the time of the generation of this documentation, it was {@value}.
     */
    public static final String VERSTR        = VERSION+"."+REVISION+"."+MINORREVISION+
                                               ((POSTFIX.length() != 0 ) ? "-"+POSTFIX : "");


    private static final String  STR_TRUE  = "true";
    private static final String  STR_FALSE = "false";

    /**
     *  Prevent instantiation.
     */
    private Release() {}
    
    /**
     *  Contains the JCR Descriptors.  Essentially lists the different
     *  features that the repository supports.  Use {@link Repository#getDescriptor(String)}
     */
    public static final String[] DESCRIPTORS = {
        Repository.SPEC_NAME_DESC,                "Content Repository for Java Technology API",
        Repository.SPEC_VERSION_DESC,             "1.0",
        Repository.REP_NAME_DESC,                 APPNAME,
        Repository.REP_VENDOR_DESC,               "Janne Jalkanen",
        Repository.REP_VENDOR_URL_DESC,           "http://www.priha.org/",
        Repository.REP_VERSION_DESC,              VERSTR,
        Repository.LEVEL_1_SUPPORTED,             STR_TRUE,
        Repository.LEVEL_2_SUPPORTED,             STR_TRUE,
        Repository.OPTION_TRANSACTIONS_SUPPORTED, STR_FALSE,
        Repository.OPTION_VERSIONING_SUPPORTED,   STR_FALSE,
        Repository.OPTION_LOCKING_SUPPORTED,      STR_TRUE,
        Repository.OPTION_OBSERVATION_SUPPORTED,  STR_FALSE,
        Repository.OPTION_QUERY_SQL_SUPPORTED,    STR_FALSE,
        Repository.QUERY_XPATH_POS_INDEX,         STR_FALSE,
        Repository.QUERY_XPATH_DOC_ORDER,         STR_FALSE
    };

    /**
     *  A static method which can be run to print the version information.
     *  
     *  @param argv Arguments.  Currently allowed argument is "-versiononly", which prints
     *              just the version information.
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
