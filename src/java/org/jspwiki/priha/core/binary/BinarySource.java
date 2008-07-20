package org.jspwiki.priha.core.binary;

import java.io.IOException;
import java.io.InputStream;


/**
 *  Represents a source of binary data.  Implementations of this
 *  class are used to represent the Binary data type instead of just
 *  a plain stream.  This allows hiding of whether the data is kept
 *  in-memory (e.g. {@link MemoryBinarySource}) or on disk {@link FileBinarySource}.
 *  Different Providers can then implement their own ways of getting the stream
 *  out of the repository.
 */
public interface BinarySource
{
    /**
     *  Returns the contents of the BinarySource as an InputStream.
     *  Note that this must always return a valid stream pointing
     *  at the beginning of the binary object.
     *  
     *  @return An InputStream representing the binary item.
     *  @throws IOException If the stream could not be opened.
     */
    public InputStream getStream() throws IOException;
    
    /**
     *  Return the length of the object represented by this BinarySource.
     *  
     *  @return The length of the object.
     *  @throws IOException If the length cannot be determined.
     */
    public long getLength() throws IOException;

    /**
     *  Duplicates the BinarySource object.
     *  
     *  @return A new BinarySource which represents the same object.
     */
    public BinarySource clone();
}
