package org.jspwiki.priha.xml;

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
