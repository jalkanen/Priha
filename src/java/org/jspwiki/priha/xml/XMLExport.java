package org.jspwiki.priha.xml;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.jcr.*;

import org.jspwiki.priha.core.NamespaceRegistryImpl;
import org.jspwiki.priha.core.PropertyImpl;
import org.jspwiki.priha.core.SessionImpl;
import org.jspwiki.priha.util.Base64;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *  Exports the JCR repository as an XML tree.
 *  
 */
public class XMLExport
{
    private SessionImpl m_session;
    
    private static final int BINARY_BUF_SIZE = 4096;
    
    public XMLExport(SessionImpl impl)
    {
        m_session = impl;
    }

    
    /**
     *  This is pretty slow... But it's okay, XML export does not need to be very speedy.
     * @param src
     * @return
     */
    private String escapeXML( String src )
    {
        src = src.replaceAll("&", "&amp;");
        src = src.replaceAll("<", "&lt;");
        src = src.replaceAll(">", "&gt;");
        src = src.replaceAll("\"", "&quot;");
        src = src.replaceAll("'", "&apos;");
        
        return src;
    }
    
    public void export(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) throws PathNotFoundException, RepositoryException, SAXException
    {
        contentHandler.startDocument();
        
        contentHandler.startPrefixMapping(m_session.getNamespacePrefix( NamespaceRegistryImpl.NS_JCP_SV ), 
                                          NamespaceRegistryImpl.NS_JCP_SV);
        contentHandler.startPrefixMapping("jcr", NamespaceRegistryImpl.NS_JCP);
        contentHandler.startPrefixMapping("mix", NamespaceRegistryImpl.NS_JCP_MIX);
        contentHandler.startPrefixMapping("nt",  NamespaceRegistryImpl.NS_JCP_NT);
        
        exportElement(absPath, contentHandler, skipBinary, noRecurse);

        contentHandler.endDocument();
    }
    
    public void exportElement(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) throws PathNotFoundException, RepositoryException, SAXException
    {
        Node startNode = (Node) m_session.getItem(absPath);
        
        AttributesImpl atts = new AttributesImpl();
        
        atts.addAttribute( "", "", "sv:name", "", 
                           startNode.getName().length() > 0 ? escapeXML(startNode.getName()) : "jcr:root");
        
        contentHandler.startElement( "", "", "sv:node", atts);
        
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

    private void serializeProperty(Property p, ContentHandler contentHandler, boolean skipBinary) throws RepositoryException, SAXException
    {
        if( p.getType() == PropertyType.BINARY && skipBinary ) return;
        
        AttributesImpl atts = new AttributesImpl();
        
        atts.addAttribute("", "", "sv:name", "", escapeXML(p.getName()));
        atts.addAttribute("", "", "sv:type", "", PropertyType.nameFromValue( p.getType() ) );
        contentHandler.startElement("", "", "sv:property", atts);
        
        if( p.getDefinition().isMultiple() )
        {
            for( Value v : p.getValues() )
            {
                serializeValue( v, contentHandler );
            }
        }
        else
        {
            serializeValue(p.getValue(), contentHandler);
        }
        
        contentHandler.endElement("", "", "sv:property");
    }

    private void serializeValue(Value v, ContentHandler contentHandler) throws SAXException, ValueFormatException, RepositoryException
    {
        AttributesImpl atts2 = new AttributesImpl();
        contentHandler.startElement("", "", "sv:value", atts2);
        
        if( v.getType() == PropertyType.BINARY )
        {
            try
            {
                InputStreamReader in = new InputStreamReader( new Base64.InputStream( v.getStream(), Base64.ENCODE ), "UTF-8" );

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
        else 
        {
            String outval = escapeXML(v.getString());
            contentHandler.characters( outval.toCharArray(), 0, outval.length() );
        }
        
        contentHandler.endElement("", "", "sv:value");
    }
    
    /**
     *  Implements property sorting according to JCR 6.4.1
     *
     */
    private static class PropertySorter implements Comparator<PropertyImpl>
    {
        public int compare(PropertyImpl o1, PropertyImpl o2)
        {
            String n1 = o1.getInternalPath().getLastComponent();
            String n2 = o2.getInternalPath().getLastComponent();
            
            if( n1.equals("jcr:primaryType") && !n2.equals("jcr:primaryType")) return -1;
            if( n2.equals("jcr:primaryType") && !n1.equals("jcr:primaryType")) return 1;
            if( n1.equals("jcr:mixinTypes") && !n2.equals("jcr:mixinTypes")) return -1;
            if( n2.equals("jcr:mixinTypes") && !n1.equals("jcr:mixinTypes")) return 1;
            if( n1.equals("jcr:uuid") && !n1.equals("jcr:uuid")) return -1;
            if( n2.equals("jcr:uuid") && !n2.equals("jcr:uuid")) return 1;
            
            return n1.compareTo(n2);
        }
        
    }
}
