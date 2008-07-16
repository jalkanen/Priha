package org.jspwiki.priha.core.binary;

import java.io.*;

public class FileBinarySource implements BinarySource
{
    private File m_file;
    
    public FileBinarySource( File f )
    {
        m_file = f;
    }
    
    public long getLength()
    {
        return m_file.length();
    }

    public InputStream getStream() throws FileNotFoundException
    {
        return new FileInputStream( m_file );
    }
    
    public FileBinarySource clone()
    {
        return new FileBinarySource(m_file);
    }
}
