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
package org.priha.xml;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 *  Outputs a somewhat-formatted XML document from the SAX event stream.
 */
public class StreamContentHandler implements ContentHandler
{
    private PrintWriter m_out;
    private int         m_depth = 0;
    private boolean     m_nameSpacesWritten = false;
    
    private HashMap<String,String> m_prefixes = new HashMap<String,String>();
    
    public StreamContentHandler(OutputStream out)
    {
        try
        {
            m_out = new PrintWriter(new OutputStreamWriter( out, "UTF-8" ));
        }
        catch (UnsupportedEncodingException e)
        {
            // This never happens
            e.printStackTrace();
        }
    }

    private void indent()
    {
        m_out.print('\n');
        for( int i = 0; i < m_depth; i++ ) m_out.print(' ');
    }
    
    public void characters(char[] ch, int start, int length) throws SAXException
    {
        m_out.write(ch,start,length);
    }

    public void endDocument() throws SAXException
    {
        m_out.flush();
    }

    public void endPrefixMapping(String prefix) throws SAXException
    {
        m_prefixes.remove(prefix);
    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException
    {
        // TODO Auto-generated method stub

    }

    public void processingInstruction(String target, String data) throws SAXException
    {
        // TODO Auto-generated method stub

    }

    public void setDocumentLocator(Locator locator)
    {
        // TODO Auto-generated method stub

    }

    public void skippedEntity(String name) throws SAXException
    {
        // TODO Auto-generated method stub

    }

    public void startDocument() throws SAXException
    {
        m_out.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    }

    private String getQName( String uri, String localName, String qName )
    {
        if( qName.length() == 0 )
        {
            qName = m_prefixes.get(uri)+":"+localName;
        }        
        
        return qName;
    }
    
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
        indent();
        m_out.print( "<" + getQName(uri,localName,qName) );
        
        if( !m_nameSpacesWritten )
        {
            for( Map.Entry<String, String> e : m_prefixes.entrySet() )
            {
                if( e.getKey().length() > 0 )
                {
                    m_out.print(" xmlns:"+e.getValue()+"='"+e.getKey()+"'");
                }
                else
                {
                    m_out.print(" xmlns='"+e.getKey()+"'");
                }
                m_out.print("\n    ");
            }
            m_nameSpacesWritten = true;
        }
        for( int i = 0; i < atts.getLength(); i++ )
        {
            String qname = getQName( atts.getURI(i), atts.getLocalName(i), atts.getQName(i) );
            String value = atts.getValue(i);
                       
            m_out.print(" "+qname+"='"+value+"'");
        }
        
        m_out.print(">");
        
        m_depth++;
    }

    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        m_depth--;
        m_out.print( "</"+getQName(uri,localName,qName)+">" );
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException
    {
        m_prefixes.put(uri, prefix);
    }

}
