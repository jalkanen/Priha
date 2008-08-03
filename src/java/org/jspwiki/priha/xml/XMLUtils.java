package org.jspwiki.priha.xml;

public class XMLUtils
{
    /**
     *  Private constructor prevents instantiation.
     */
    private XMLUtils() {}
    
    /**
     *  This is pretty slow... But it's okay, XML export does not need to be very speedy.
     *  @param src
     *  @return
     */
    public static String escapeXML( String src )
    {
        src = src.replaceAll("&", "&amp;");
        src = src.replaceAll("<", "&lt;");
        src = src.replaceAll(">", "&gt;");
        src = src.replaceAll("\"", "&quot;");
        src = src.replaceAll("'", "&apos;");
        
        return src;
    }

    private static final String VALID_HEX_CHARS = "0123456789abcdefABCDEF";
    
    /**
     *  This method encodes a String so that it is a valid XML name, according
     *  to ISO/IEC 9075-14:2003.
     *  
     *  @param src
     *  @return
     */
    public static String encode( String src )
    {
        StringBuilder sb = new StringBuilder();
        
        for( int i = 0; i < src.length(); i++ )
        {
            int ch = src.charAt(i);
            
            if( !isXMLNameChar(ch) || 
                (ch == '_' && i < src.length()-5 && src.charAt(i+1) == 'x' &&
                    (VALID_HEX_CHARS.indexOf(src.charAt(i+2)) != -1) &&
                    (VALID_HEX_CHARS.indexOf(src.charAt(i+3)) != -1) &&
                    (VALID_HEX_CHARS.indexOf(src.charAt(i+4)) != -1) &&
                    (VALID_HEX_CHARS.indexOf(src.charAt(i+5)) != -1) ) )
            {
                sb.append("_x");
                String s = Integer.toHexString(ch);
                for( int j = 0; j < 4-s.length(); j++ ) sb.append('0');
                sb.append(s);
                sb.append('_');
            }
            else
            {
                sb.append( (char)ch );
            }
        }
        
        return sb.toString();
    }
    
    /**
     *  Decodes a string encoded by the encode() method.
     *  
     *  @param src The string to be decoded.
     *  @return A decoded string.
     */
    public static String decode( String src )
    {
        StringBuilder sb = new StringBuilder();
        
        for( int i = 0; i < src.length(); i++ )
        {
            int ch = src.charAt(i);
            
            if( ch == '_' && i < src.length()-6 )
            {
                if( src.charAt(i+1) == 'x' && src.charAt(i+6) == '_' )
                {
                    try
                    {
                        int digit = Integer.parseInt( src.substring(i+2,i+6), 16 );
                        
                        sb.append( (char) digit );
                        
                        i = i + 6;
                        
                        continue;
                    }
                    catch(Exception e) {}       
                }
            }
            
            sb.append( (char)ch );
        }
        
        return sb.toString();
    }
    
    /**
     *  Returns true, if the character given is a XML Name character, as per XML 1.0 specification
     *  section 2.3.
     *  However, this method does not (yet) include all characters on the upper planes.
     *  
     *  @param ch The character to check for.
     *  @return True, if the character is a valid XML name character.  Otherwise, returns false.
     */
    // FIXME: is not complete
    public static boolean isXMLNameChar( int ch )
    {
        // BaseChar
        if( (ch >= 0x0041 && ch <= 0x005A) || (ch >= 0x0061 && ch <= 0x007A) || (ch >= 0x00C0 && ch <= 0x00D6) ||
            (ch >= 0x00D8 && ch <= 0x00F6) || (ch >= 0x00F8 && ch <= 0x00FF) || (ch >= 0x0100 && ch <= 0x0131) )
        {
            return true;
        }
        
        // Ideographic.  COMPLETE.
        
        if( (ch >= 0x4E00 && ch <= 0x9FA5) || ch == 0x3007 || (ch >= 0x3021 && ch <= 0x3029) )
        {
            return true;
        }
        
        // Digit.  COMPLETE.
        if( (ch >= 0x0030 && ch <= 0x0039) || (ch >= 0x0660 && ch <= 0x0669) ||
            (ch >= 0x06F0 && ch <= 0x06F9) || (ch >= 0x0966 && ch <= 0x096F) || (ch >= 0x09E6 && ch <= 0x09EF) ||
            (ch >= 0x0A66 && ch <= 0x0A6F) || (ch >= 0x0AE6 && ch <= 0x0AEF) || (ch >= 0x0B66 && ch <= 0x0B6F) ||
            (ch >= 0x0BE7 && ch <= 0x0BEF) || (ch >= 0x0C66 && ch <= 0x0C6F) || (ch >= 0x0CE6 && ch <= 0x0CEF) ||
            (ch >= 0x0D66 && ch <= 0x0D6F) || (ch >= 0x0E50 && ch <= 0x0E59) || (ch >= 0x0ED0 && ch <= 0x0ED9) ||
            (ch >= 0x0F20 && ch <= 0x0F29) )
        {
            return true;
        }
        
        // Random char.  COMPLETE.
        if( ch == '.' || ch == '-' || ch == '_' || ch == ':' ) return true;
        
        // Combining char
        if( (ch >= 0x0300 && ch <= 0x0345) )
        {
            return true;
        }
        
        // Extender char.  COMPLETE.
        if( (ch == 0x00B7) || (ch == 0x02D0) || (ch == 0x02D1) || (ch == 0x0387) || (ch == 0x0640) || 
            (ch == 0x0E46) || (ch == 0x0EC6) || (ch == 0x3005) || (ch >= 0x3031 && ch <= 0x3035) ||
            (ch >= 0x309D && ch <= 0x309E) || (ch >= 0x30FC && ch <= 0x30FE) )
        {
            return true;
        }
        
        //  Wasn't part of the previous groups, so must not be an XML Name character.
        return false;
    }
}
