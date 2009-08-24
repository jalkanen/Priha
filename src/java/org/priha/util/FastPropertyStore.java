package org.priha.util;

import java.io.*;
import java.util.Map;
import java.util.Properties;

/**
 *  Provides a very fast way of saving and loading Property files.  The format
 *  is almost the same as with regular ones, with few key differences:
 *  <ul>
 *  <li>The file encoding is UTF-8
 *  <li>Unicode entities (\\u) are not recognized
 *  <li>Line continuations are not supported (line ends with a backslash)
 *  <li>Always uses \\n for ending the line, on all architectures.
 *  </ul>
 */
public class FastPropertyStore
{
    public static void store(OutputStream out, Properties props) throws IOException
    {
        BufferedWriter o = new BufferedWriter(new OutputStreamWriter(out,"UTF-8") );
        
        for( Map.Entry<Object,Object> e : props.entrySet() )
        {
            String key   = (String)e.getKey();
            String value = (String)e.getValue();
            
            o.write( key );
            o.write( "=" );
            o.write( value );
            o.write( "\n" );
        }
        
        o.flush();
    }
    
    public static Properties load(InputStream in) throws IOException
    {
        Properties props = new Properties();
        
        /*
        BufferedReader i = new BufferedReader(new InputStreamReader( in,"UTF-8" ));
        */
        
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        FileUtil.copyContents( in, ba );
        
        String c = new String( ba.toByteArray(), "UTF-8" );
        
        BufferedStringReader i = new BufferedStringReader(c);
        
        String line;
        
        while( (line = i.readLine()) != null )
        {
            line = line.trim();
            
            if( line.length() == 0 || line.charAt( 0 ) == '#' ) continue;
            
            int eqSign = line.indexOf( '=' );
            
            if( eqSign == -1 ) throw new IOException("Illegal format in property file");
            
            String key = line.substring( 0, eqSign );
            String val = line.substring( eqSign+1 );
            
            props.put( key, val );
        }
        
        return props;
    }
    
    /**
     *  This is a very fast String reader which implements the readLine()
     *  method by returning substrings of the given string.  EOL is
     *  determined by the \\n character.
     */
    private static class BufferedStringReader
    {
        String m_string;
        int    m_pos;
        
        public BufferedStringReader( String s )
        {
            m_string = s;
        }

        public String readLine()
        {
            if( m_pos >= m_string.length() ) return null;
            
            String result;
            
            int newline = m_string.indexOf( '\n', m_pos );

            if( newline >= 0 )
            {
                result = m_string.substring( m_pos, newline );
                m_pos = newline+1;
            }
            else
            {
                result = m_string.substring( m_pos );
                m_pos = m_string.length()+1;
            }
            
            return result;
        }
    }
}
