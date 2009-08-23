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

import static org.priha.core.JCRConstants.Q_JCR_MIXINTYPES;
import static org.priha.core.JCRConstants.Q_JCR_PRIMARYTYPE;
import static org.priha.core.JCRConstants.Q_JCR_UUID;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Logger;

import javax.jcr.*;

import org.priha.core.PropertyImpl;
import org.priha.core.SessionImpl;
import org.priha.util.Base64;
import org.priha.util.QName;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *  Exports the JCR repository as an XML tree using the System View.
 */
public class XMLSysExport extends XMLExport
{
    private static final int BINARY_BUF_SIZE = 4096;
    
    private static Logger log = Logger.getLogger(XMLSysExport.class.getName());
    
    public XMLSysExport(SessionImpl impl)
    {
        super(impl);
    }

    
    public void exportElement(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) throws PathNotFoundException, RepositoryException, SAXException
    {
        Node startNode = (Node) m_session.getItem(absPath);
        
        AttributesImpl atts = new AttributesImpl();
        
        atts.addAttribute( "", "", "sv:name", "", 
                           startNode.getName().length() > 0 ? XMLUtils.escapeXML(startNode.getName()) : "jcr:root");
        
        contentHandler.startElement( "", "", "sv:node", atts);
        
        log.finest("Serializing node at "+absPath);
        
        //
        //  Serialize the properties.  We need to sort them in a particular order
        //  according to the JCR spec.
        //
        ArrayList<PropertyImpl> sortedList = new ArrayList<PropertyImpl>();
        for( PropertyIterator pi = startNode.getProperties(); pi.hasNext(); )
        {
            PropertyImpl p = (PropertyImpl)pi.nextProperty();
         
            sortedList.add(p);
        }

        Collections.sort( sortedList, new PropertySorter() );
        
        for( PropertyImpl p : sortedList )
        {
            serializeProperty( p, contentHandler, skipBinary );
        }
        
        //
        //  Finally, serialize the child nodes, if so requested.
        //
        if( !noRecurse )
        {
            for( NodeIterator ni = startNode.getNodes(); ni.hasNext(); )
            {
                Node n = ni.nextNode();
                
                exportElement( n.getPath(), contentHandler, skipBinary, noRecurse );
            }
        }
        contentHandler.endElement("", "", "sv:node");
    }

    /**
     *  Serializes a property value to the given ContentHandler.
     */
    private void serializeProperty(Property p, ContentHandler contentHandler, boolean skipBinary) throws RepositoryException, SAXException
    {
        log.finest("   Property "+p.getName());
        
        AttributesImpl atts = new AttributesImpl();
        
        atts.addAttribute("", "", "sv:name", "", XMLUtils.escapeXML(p.getName()));
        atts.addAttribute("", "", "sv:type", "", PropertyType.nameFromValue( p.getType() ) );
        contentHandler.startElement("", "", "sv:property", atts);
        
        if( p.getDefinition().isMultiple() )
        {
            for( Value v : p.getValues() )
            {
                serializeValue( v, contentHandler, skipBinary );
            }
        }
        else
        {
            serializeValue(p.getValue(), contentHandler, skipBinary);
        }
        
        contentHandler.endElement("", "", "sv:property");
    }

    /**
     *  Serializes a single value.
     */
    private void serializeValue(Value v, ContentHandler contentHandler, boolean skipBinary) throws SAXException, ValueFormatException, RepositoryException
    {
        AttributesImpl atts2 = new AttributesImpl();
        contentHandler.startElement("", "", "sv:value", atts2);
        
        if( v.getType() == PropertyType.BINARY )
        {
            if( !skipBinary )
            {
                try
                {
                    InputStreamReader in = new InputStreamReader( new Base64.InputStream( v.getStream(), Base64.ENCODE ), 
                                                                  "UTF-8" );

                    char[] buf = new char[ BINARY_BUF_SIZE ];
                    int bytes;
                
                    while( (bytes = in.read(buf)) != -1 )
                    {
                        contentHandler.characters(buf, 0, bytes);                    
                    }
                }
                catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            
        }
        else 
        {
            String outval = XMLUtils.escapeXML(v.getString());
            contentHandler.characters( outval.toCharArray(), 0, outval.length() );
        }
        
        contentHandler.endElement("", "", "sv:value");
    }
    
    /**
     *  Implements property sorting according to JSR-170 1.0 chapter 6.4.1.
     */
    private static class PropertySorter implements Comparator<PropertyImpl>
    {
        public int compare(PropertyImpl o1, PropertyImpl o2)
        {
            QName n1 = o1.getInternalPath().getLastComponent();
            QName n2 = o2.getInternalPath().getLastComponent();
            
            if( n1.equals(Q_JCR_PRIMARYTYPE) && !n2.equals(Q_JCR_PRIMARYTYPE)) return -1;
            if( n2.equals(Q_JCR_PRIMARYTYPE) && !n1.equals(Q_JCR_PRIMARYTYPE)) return 1;
            if( n1.equals(Q_JCR_MIXINTYPES) && !n2.equals(Q_JCR_MIXINTYPES)) return -1;
            if( n2.equals(Q_JCR_MIXINTYPES) && !n1.equals(Q_JCR_MIXINTYPES)) return 1;
            if( n1.equals(Q_JCR_UUID) && !n1.equals(Q_JCR_UUID)) return -1;
            if( n2.equals(Q_JCR_UUID) && !n2.equals(Q_JCR_UUID)) return 1;
            
            return n1.toString().compareTo(n2.toString());
        }
        
    }
}
