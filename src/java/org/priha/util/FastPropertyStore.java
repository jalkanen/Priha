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
        
        BufferedReader i = new BufferedReader(new InputStreamReader( in,"UTF-8" ));
        
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
}
