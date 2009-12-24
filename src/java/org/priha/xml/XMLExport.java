/*
    Priha - A JSR-170 implementation library.

    Copyright (C) 2007-2009 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.priha.core.SessionImpl;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public abstract class XMLExport
{
    protected SessionImpl m_session;

    protected XMLExport(SessionImpl impl)
    {
        m_session = impl;
    }


    /**
     *  Exports the JCR repository starting from absPath.
     *  
     *  @param absPath The path from which to start the exporting.
     *  @param contentHandler The SAX ContentHandler which will receive the export events.
     *  @param skipBinary If true, all BINARY type values will be skipped.
     *  @param noRecurse If true, won't recurse into subdirectories.
     *  @throws PathNotFoundException
     *  @throws RepositoryException
     *  @throws SAXException If the ContentHandler throws one.
     */
    public void export(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) throws PathNotFoundException, RepositoryException, SAXException
    {
        contentHandler.startDocument();
        
        for( String prefix : m_session.getNamespacePrefixes() )
        {
            //
            //  XML and default prefixes are not exported.
            //
            if( !prefix.startsWith("xml") && !prefix.equals( "" ) )
                contentHandler.startPrefixMapping( prefix, m_session.getNamespaceURI( prefix ) );
        }
        
        exportElement(absPath, contentHandler, skipBinary, noRecurse);

        contentHandler.endDocument();
    }
    
    protected abstract void exportElement( String absPath, 
                                           ContentHandler contentHandler, 
                                           boolean skipBinary, 
                                           boolean noRecurse ) 
        throws PathNotFoundException, RepositoryException, SAXException;
}
