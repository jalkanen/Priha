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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllTests extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite("Priha unit tests");
        
        suite.addTest( org.priha.nodetype.AllTests.suite() );
        suite.addTest( org.priha.core.AllTests.suite() );
        suite.addTest( org.priha.util.AllTests.suite() );
        suite.addTest( org.priha.xml.AllTests.suite() );
        suite.addTest( org.priha.query.AllTests.suite() );
        suite.addTest( org.priha.providers.AllTests.suite() );
        
        return suite;
    }
}
