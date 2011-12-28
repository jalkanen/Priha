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

import org.priha.path.PathUtil;

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
