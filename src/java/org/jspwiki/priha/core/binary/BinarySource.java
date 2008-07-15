package org.jspwiki.priha.core.binary;

import java.io.IOException;
import java.io.InputStream;

public interface BinarySource
{
    public InputStream getStream() throws IOException;
    
    public long getLength() throws IOException;
}
