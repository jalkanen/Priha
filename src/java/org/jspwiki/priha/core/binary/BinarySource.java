package org.jspwiki.priha.core.binary;

import java.io.IOException;
import java.io.InputStream;


/**
 *  Represents a source of binary data.  Implementations of this
 *  class are used to represent the Binary data type instead of just
 *  a plain stream.  This allows hiding of whether the data is kept
 *  in-memory (e.g. {@link MemoryBinarySource}) or on disk (@link {@link FileBinarySource}.
 *  Different Providers can then implement their own ways of getting the stream
 *  out of the repository.
 */
public interface BinarySource
{
    public InputStream getStream() throws IOException;
    
    public long getLength() throws IOException;

    public BinarySource clone();
}
