package org.priha.util;

import org.priha.util.PathUtil;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class PathUtilTest extends TestCase
{
    public void testPath1() throws Exception
    {
        String res = PathUtil.resolve("/", "foo");

        assertEquals("/foo",res);
    }

    public void testPath2() throws Exception
    {
        String res = PathUtil.resolve("/foo/bar", "..");

        assertEquals("/foo",res);
    }

    public void testPath3() throws Exception
    {
        String res = PathUtil.resolve("/foo/bar/one/two/three/", "../../../blog/bar/../zup");

        assertEquals("/foo/bar/blog/zup",res);
    }

    public void testNormalize1()
    {
        try
        {
            PathUtil.normalize("/../../../");
            fail("No exception");
        }
        catch( IllegalArgumentException e ) {}
    }

    public static Test suite()
    {
        return new TestSuite( PathUtilTest.class );
    }
}
