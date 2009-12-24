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
package org.priha.xml;

import org.priha.xml.XMLUtils;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class XMLUtilsTest extends TestCase
{
    public void testEncode()
    {
        assertEquals( "My Documents", "My_x0020_Documents", XMLUtils.encode("My Documents") );
        assertEquals( "My Documents_", "My_x0020_Documents_", XMLUtils.encode("My Documents_") );
        assertEquals( "My_Documents", "My_Documents", XMLUtils.encode("My_Documents") );
        assertEquals( "My_x0020Documents", "My_x005f_x0020Documents", XMLUtils.encode("My_x0020Documents") );
        assertEquals( "My_x0020_Documents", "My_x005f_x0020_Documents", XMLUtils.encode("My_x0020_Documents") );
        assertEquals( "My_x0020 Documents", "My_x005f_x0020_x0020_Documents", XMLUtils.encode("My_x0020 Documents") );
    }
   
    public void testDecode()
    {
        assertEquals( "My Documents", "My Documents", XMLUtils.decode("My_x0020_Documents") );
        assertEquals( "My Documents_", "My Documents_", XMLUtils.decode("My_x0020_Documents_") );
        assertEquals( "My_Documents", "My_Documents", XMLUtils.decode("My_Documents") );
        assertEquals( "My_x0020Documents", "My_x0020Documents", XMLUtils.decode("My_x005f_x0020Documents") );
        assertEquals( "My_x0020_Documents", "My_x0020_Documents", XMLUtils.decode("My_x005f_x0020_Documents") );
        assertEquals( "My_x0020 Documents", "My_x0020 Documents", XMLUtils.decode("My_x005f_x0020_x0020_Documents") );
    }
    
    public static Test suite()
    {
        return new TestSuite( XMLUtilsTest.class );
    }
}
