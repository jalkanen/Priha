/*
    Priha - A JSR-170 implementation library.

    Copyright (C) 2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package org.jspwiki.priha.core.binary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jspwiki.priha.util.FileUtil;

/**
 *  A BinarySource which stores the binary in the memory
 *  in a byte array.
 *  <p>
 *  Obviously, this source is limited by the size of the
 *  heap.
 */
public class MemoryBinarySource implements BinarySource
{
    private byte[] m_bytes;
    
    /**
     *  Creates a new MemoryBinarySource by slurping the
     *  contents into a binary array.
     *  
     *  @param in The InputStream to read.  If null, creates
     *            an empty MemoryBinarySource.
     *  @throws IOException If reading of the stream failed
     *                      and the memory could not be initialized.s
     */
    public MemoryBinarySource( InputStream in ) throws IOException
    {
        if( in != null )
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
        
            FileUtil.copyContents( in, out );
        
            m_bytes = out.toByteArray();
        }
        else
        {
            m_bytes = new byte[0];
        }
    }
    
    /**
     *  Creates a MemoryBinarySource by using the given byte array.
     *  
     *  @param v The byte array to use.
     */
    public MemoryBinarySource(byte[] v)
    {
        m_bytes = v;
    }

    /**
     *  {@inheritDoc}
     */
    public long getLength()
    {
        return m_bytes.length;
    }

    /**
     *  {@inheritDoc}
     */
    public InputStream getStream()
    {
        return new ByteArrayInputStream( m_bytes );
    }

    /**
     *  {@inheritDoc}
     */
    public MemoryBinarySource clone()
    {
        return new MemoryBinarySource(m_bytes);
    }
}
