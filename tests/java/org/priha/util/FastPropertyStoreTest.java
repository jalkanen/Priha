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
