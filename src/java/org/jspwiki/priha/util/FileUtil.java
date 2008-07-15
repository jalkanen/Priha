package org.jspwiki.priha.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

public class FileUtil
{
    private static final int BUFFER_SIZE = 4096;
    
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
     *  <P>
     *  This method is smart and falls back to ISO-8859-1 if the input stream does not
     *  seem to be in the specified encoding.
     *
     *  @param input The InputStream to read from.
     *  @param encoding The encoding to assume at first.
     *  @return A String, interpreted in the "encoding", or, if it fails, in Latin1.
     *  @throws IOException If the stream cannot be read or the stream cannot be
     *          decoded (even) in Latin1
     */
    public static String readContents( InputStream input, String encoding )
        throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FileUtil.copyContents( input, out );

        ByteBuffer     bbuf        = ByteBuffer.wrap( out.toByteArray() );

        Charset        cset        = Charset.forName( encoding );
        CharsetDecoder csetdecoder = cset.newDecoder();

        csetdecoder.onMalformedInput( CodingErrorAction.REPORT );
        csetdecoder.onUnmappableCharacter( CodingErrorAction.REPORT );

        try
        {
            CharBuffer cbuf = csetdecoder.decode( bbuf );

            return cbuf.toString();
        }
        catch( CharacterCodingException e )
        {
            Charset        latin1    = Charset.forName("ISO-8859-1");
            CharsetDecoder l1decoder = latin1.newDecoder();

            l1decoder.onMalformedInput( CodingErrorAction.REPORT );
            l1decoder.onUnmappableCharacter( CodingErrorAction.REPORT );

            try
            {
                bbuf = ByteBuffer.wrap( out.toByteArray() );

                CharBuffer cbuf = l1decoder.decode( bbuf );

                return cbuf.toString();
            }
            catch( CharacterCodingException ex )
            {
                throw (CharacterCodingException) ex.fillInStackTrace();
            }
        }
    }
}
