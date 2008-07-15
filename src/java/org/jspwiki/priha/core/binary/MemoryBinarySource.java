package org.jspwiki.priha.core.binary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jspwiki.priha.util.FileUtil;

public class MemoryBinarySource implements BinarySource
{
    private byte[] m_bytes;
    
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
    
    public long getLength()
    {
        return m_bytes.length;
    }

    public InputStream getStream()
    {
        return new ByteArrayInputStream( m_bytes );
    }

}
