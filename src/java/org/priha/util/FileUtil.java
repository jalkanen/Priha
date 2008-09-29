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
package org.priha.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.Properties;
import java.util.logging.Logger;

import org.priha.core.RepositoryImpl;

public class FileUtil
{
    private static final int BUFFER_SIZE = 4096;
    private static Logger log = Logger.getLogger(FileUtil.class.getName());
    
    /**
     *  Just copies all characters from <I>in</I> to <I>out</I>.  The copying
     *  is performed using a buffer of bytes.
     *
     *  @since 1.5.8
     *  @param in The reader to copy from
     *  @param out The reader to copy to
     *  @throws IOException If reading or writing failed.
     */
    public static void copyContents( Reader in, Writer out )
        throws IOException
    {
        char[] buf = new char[BUFFER_SIZE];
        int bytesRead = 0;

        while ((bytesRead = in.read(buf)) > 0)
        {
            out.write(buf, 0, bytesRead);
        }

        out.flush();
    }

    /**
     *  Just copies all bytes from <I>in</I> to <I>out</I>.  The copying is
     *  performed using a buffer of bytes.
     *
     *  @since 1.9.31
     *  @param in The inputstream to copy from
     *  @param out The outputstream to copy to
     *  @throws IOException In case reading or writing fails.
     */
    public static void copyContents( InputStream in, OutputStream out )
        throws IOException
    {
        byte[] buf = new byte[BUFFER_SIZE];
        int bytesRead = 0;

        while ((bytesRead = in.read(buf)) > 0)
        {
            out.write(buf, 0, bytesRead);
        }

        out.flush();
    }

    /**
     *  Reads in file contents.
     *
     *  @param input The InputStream to read from.
     *  @param encoding The encoding to assume at first.
     *  @return A String, interpreted in the "encoding", or, if it fails, in Latin1.
     *  @throws IOException If the stream cannot be read or the stream cannot be
     *          decoded.
     */
    public static String readContents( InputStream input, String encoding )
        throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FileUtil.copyContents( input, out );

        ByteBuffer     bbuf        = ByteBuffer.wrap( out.toByteArray() );

        Charset        cset        = Charset.forName( encoding );
        CharsetDecoder csetdecoder = cset.newDecoder();

        csetdecoder.onMalformedInput( CodingErrorAction.REPLACE );
        csetdecoder.onUnmappableCharacter( CodingErrorAction.REPLACE );

        try
        {
            CharBuffer cbuf = csetdecoder.decode( bbuf );

            return cbuf.toString();
        }
        catch( CharacterCodingException e )
        {
            log.fine( "Failed to map a character to UTF-8" );
        }
        return null;
    }

    /**
     *  Takes a list of paths and attempts to locate a property file from
     *  the list.  The first one which can be loaded is parsed and returned.
     *  
     *  @param propertyPaths
     *  @return
     *  @throws IOException
     */
    public static Properties findProperties(String[] propertyPaths) throws IOException
    {
        Properties props = new Properties();
    
        for( int i = 0; i < propertyPaths.length; i++ )
        {
            InputStream in = RepositoryImpl.class.getResourceAsStream(propertyPaths[i]);
            
            if( in != null )
            {
                props.load(in);
                log.fine("Loaded properties from "+propertyPaths[i]);
                break;
            }
        }
        return props;
    }
}
