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
