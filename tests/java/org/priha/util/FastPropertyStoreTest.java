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

import java.io.*;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FastPropertyStoreTest extends TestCase
{
    File m_propertyFile;
    
    protected void setUp() throws Exception
    {
        m_propertyFile = File.createTempFile( "prihatest", "info" );
        m_propertyFile.deleteOnExit();
    }

    protected void tearDown()
    {
    }
    
    public void testWrite() throws FileNotFoundException, IOException
    {
        Properties p = new Properties();
        
        p.setProperty("txt","bar");
        p.setProperty("num","1");
        p.setProperty("utf","\u3041"); // HIRAGANA A
        
        FileOutputStream out = new FileOutputStream(m_propertyFile);
        FastPropertyStore.store( out, p );
        out.close();
        
        String s = FileUtil.readContents( new FileInputStream(m_propertyFile), "UTF-8" );
        
        assertTrue(s.indexOf( "txt=bar\n" ) != -1 );
        assertTrue(s.indexOf("num=1\n" ) != -1 );
        assertTrue(s.indexOf( "utf=\u3041\n") != -1 );
    }
    
    
    
    public static Test suite()
    {
        return new TestSuite( FastPropertyStoreTest.class );
    }
}
