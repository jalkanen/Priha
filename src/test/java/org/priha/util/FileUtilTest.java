package org.priha.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.Random;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.priha.core.PerformanceTest.Perf;

public class FileUtilTest extends TestCase
{
    public static String readContentsIO( FileInputStream input, String encoding, boolean decodeNIO )
    throws IOException
    {
        ByteBuffer bbuf;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FileUtil.copyContents(input, out);

        bbuf = ByteBuffer.wrap(out.toByteArray());

        return decodeNIO ? decodeNIO(encoding, bbuf) : decodeIO(encoding, bbuf);
    }

    public static String readContentsNIO( FileInputStream input, String encoding, boolean decodeNIO )
    throws IOException
    {
        ByteBuffer bbuf;

        FileChannel fc = input.getChannel();
        bbuf = ByteBuffer.allocate( (int)fc.size() );
        
        fc.read(bbuf);
        bbuf.flip();

        return decodeNIO ? decodeNIO(encoding, bbuf) : decodeIO(encoding, bbuf);
    }
    
    private static String decodeIO(String encoding, ByteBuffer bbuf) throws UnsupportedEncodingException
    {
        if( bbuf.hasArray() )
            return new String( bbuf.array(), encoding );
        
        throw new IllegalArgumentException();
    }
    
    private static String decodeNIO(String encoding, ByteBuffer bbuf)
    {
        Charset cset = Charset.forName(encoding);
        CharsetDecoder csetdecoder = cset.newDecoder();

        csetdecoder.onMalformedInput(CodingErrorAction.REPLACE);
        csetdecoder.onUnmappableCharacter(CodingErrorAction.REPLACE);

        try
        {
            CharBuffer cbuf = csetdecoder.decode(bbuf);

            return cbuf.toString();
        }
        catch (CharacterCodingException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private static File createTempFile( int txtlen ) throws IOException
    {
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        
        for(int i = 0; i < txtlen; i++ )
        {
            int c = 'A' + r.nextInt(25);
            sb.append( (char)c );
        }
        
        String s = sb.toString();
        
        assertEquals( txtlen, s.length() );
        File f = File.createTempFile("tmp", "txt");
        
        FileOutputStream fout = new FileOutputStream(f);
        
        fout.write( s.getBytes("UTF-8") );
        
        fout.close();
        
        return f;
    }
    
    /**
     *  Test what is the fastest way to read a file.
     *  
     * @throws Exception
     */
    public void testReadContent() throws Exception
    {
        runSingleTest(16);
        runSingleTest(32);
        runSingleTest(64);
        runSingleTest(128);
        runSingleTest(256);
        runSingleTest(512);
        runSingleTest(1024);
        runSingleTest(2048);
        runSingleTest(4096);
        runSingleTest(16384);
        runSingleTest(65536);
        runSingleTest(65536*16);

        System.out.println();
        Perf.print();
     }

    private void runSingleTest(int size) throws IOException, FileNotFoundException
    {
        int numIters = 1000;
        File f = createTempFile( size );
        
        System.out.print("."); System.out.flush();
        
        Perf.setTestable("Length: "+size);
        Perf.start("IO+IO");
        
        for( int i = 0; i < numIters; i++ )
        {
            FileInputStream fis = new FileInputStream(f);
            String s = readContentsIO(fis, "UTF-8",false);
            fis.close();
            assertEquals( size, s.length());
        }
        
        Perf.stop(numIters);

        Perf.start("NIO+IO");
        
        for( int i = 0; i < numIters; i++ )
        {
            try
            {
                FileInputStream fis = new FileInputStream(f);
                String s = readContentsNIO(fis, "UTF-8",false);
                fis.close();
                assertEquals( size, s.length());
            }
            catch( IllegalArgumentException e ) { break; }
        }
        
        Perf.stop(numIters);

        Perf.start("IO+NIO");
        
        for( int i = 0; i < numIters; i++ )
        {
            try
            {
                FileInputStream fis = new FileInputStream(f);
                String s = readContentsIO(fis, "UTF-8",true);
                fis.close();
                assertEquals( size, s.length());
            }
            catch( IllegalArgumentException e ) { break; }
        }
        
        Perf.stop(numIters);
        Perf.start("NIO+NIO");
        
        for( int i = 0; i < numIters; i++ )
        {
            try
            {
                FileInputStream fis = new FileInputStream(f);
                String s = readContentsNIO(fis, "UTF-8",true);
                fis.close();
                assertEquals( size, s.length());
            }
            catch( IllegalArgumentException e ) { break; }
        }
        
        Perf.stop(numIters);
    }
    
    public static TestSuite suite()
    {
        return new TestSuite(FileUtilTest.class);
    }
}
