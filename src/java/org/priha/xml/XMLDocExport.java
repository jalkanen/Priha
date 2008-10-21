package org.priha.xml;

import javax.jcr.*;
import javax.xml.namespace.QName;

import org.priha.core.JCRConstants;
import org.priha.core.NodeImpl;
import org.priha.core.PropertyImpl;
import org.priha.core.SessionImpl;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *  Implements the Document view export.
 */
public class XMLDocExport extends XMLExport
{
    public XMLDocExport( SessionImpl impl )
    {
        super(impl);
    }

    @Override
    protected void exportElement( String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse ) throws PathNotFoundException, RepositoryException, SAXException
    {
        NodeImpl startNode = (NodeImpl)m_session.getItem( absPath );
        
        AttributesImpl atts = new AttributesImpl();
        
        for( PropertyIterator pi = startNode.getProperties(); pi.hasNext(); )
        {
            PropertyImpl p = (PropertyImpl)pi.nextProperty();
            
            // Ignore multi-value properties
            if( p.getDefinition().isMultiple() ) continue;
            
            QName n = p.getQName();
            
            atts.addAttribute( "", "", 
                               XMLUtils.encode(m_session.fromQName( n )), 
                               "", p.getString() );
        }
        
        QName n = startNode.getQName();
        
        if( n.toString().length() == 0 ) n = JCRConstants.Q_JCR_ROOT;
        
        contentHandler.startElement( "", 
                                     "", 
                                     XMLUtils.encode(m_session.fromQName( n )), 
                                     atts );
        
        if( !noRecurse )
        {
            for( NodeIterator ni = startNode.getNodes(); ni.hasNext(); )
            {
                Node nd = ni.nextNode();
                
                exportElement( nd.getPath(), contentHandler, skipBinary, noRecurse );
            }
        }
        
        contentHandler.endElement( "", "", XMLUtils.encode(m_session.fromQName( n )) );
    }

}
