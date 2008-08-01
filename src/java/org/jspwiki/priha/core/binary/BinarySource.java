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
