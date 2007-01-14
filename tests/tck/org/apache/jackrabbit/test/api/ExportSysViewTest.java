/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.test.api;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.jcr.Workspace;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * <code>ExportSysViewTest</code> tests the SysView Export of a tree given by a
 * node path. This is done with checking the SAX events of the sysview export
 * against the items found by a traverse of the given tree.
 *
 * @test
 * @sources ExportSysViewTest.java
 * @executeClass org.apache.jackrabbit.test.api.ExportSysViewTest
 * @keywords level1
 */
public class ExportSysViewTest extends AbstractJCRTest {

    private Workspace workspace;
    private File file;

    private final boolean WORKSPACE = true, SESSION = false;
    private final boolean SKIPBINARY = true, SAVEBINARY = false;
    private final boolean NORECURSE = true, RECURSE = false;

    private Session session;
    private String testPath;
    private Node testNode;


    protected void setUp() throws Exception {
        isReadOnly = true;
        session = helper.getReadOnlySession();
        workspace = session.getWorkspace();
        file = File.createTempFile("SysViewExportTest", ".xml");

        super.setUp();
        this.testPath = testRoot;
        this.testNode = (Node) session.getItem(testPath);
    }

    protected void tearDown() throws Exception {
        file.delete();
        if (session != null) {
            session.logout();
        }
        super.tearDown();
    }

    /*
    // tests with content handler
    public void testExportSysView_handler_workspace_skipBinary_noRecurse()
            throws IOException, RepositoryException, SAXException, IOException {
        doTestWithHandler(WORKSPACE, SKIPBINARY, NORECURSE);
    }

    public void testExportSysView_handler_workspace_skipBinary_recurse()
            throws IOException, RepositoryException, SAXException, IOException {
        doTestWithHandler(WORKSPACE, SKIPBINARY, RECURSE);
    }

    public void testExportSysView_handler_workspace_saveBinary_noRecurse()
            throws IOException, RepositoryException, SAXException, IOException {
        doTestWithHandler(WORKSPACE, SAVEBINARY, NORECURSE);
    }

    public void testExportSysView_handler_workspace_saveBinary_recurse()
            throws IOException, RepositoryException, SAXException, IOException {
        doTestWithHandler(WORKSPACE, SAVEBINARY, RECURSE);
    }
  */

    public void testExportSysView_handler_session_skipBinary_noRecurse()
            throws IOException, RepositoryException, SAXException, IOException {
        doTestWithHandler(SESSION, SKIPBINARY, NORECURSE);
    }

    public void testExportSysView_handler_session_skipBinary_recurse()
            throws IOException, RepositoryException, SAXException, IOException {
        doTestWithHandler(SESSION, SKIPBINARY, RECURSE);
    }

    public void testExportSysView_handler_session_saveBinary_noRecurse()
            throws IOException, RepositoryException, SAXException, IOException {
        doTestWithHandler(SESSION, SAVEBINARY, NORECURSE);
    }

    public void testExportSysView_handler_session_saveBinary_recurse()
            throws IOException, RepositoryException, SAXException, IOException {
        doTestWithHandler(SESSION, SAVEBINARY, RECURSE);
    }

    /*
       // tests with output stream
       public void testExportSysView_stream_workspace_skipBinary_noRecurse()
               throws IOException, RepositoryException, SAXException {
           doTestWithStream(WORKSPACE, SKIPBINARY, NORECURSE);
       }

       public void testExportSysView_stream_workspace_skipBinary_recurse()
               throws IOException, RepositoryException, SAXException {
           doTestWithStream(WORKSPACE, SKIPBINARY, RECURSE);
       }

       public void testExportSysView_stream_workspace_saveBinary_noRecurse()
               throws IOException, RepositoryException, SAXException {
           doTestWithStream(WORKSPACE, SAVEBINARY, NORECURSE);
       }

       public void testExportSysView_stream_workspace_saveBinary_recurse()
               throws IOException, RepositoryException, SAXException {
           doTestWithStream(WORKSPACE, SAVEBINARY, RECURSE);
       }
      */

    public void testExportSysView_stream_session_skipBinary_recurse()
            throws IOException, RepositoryException, SAXException {
        doTestWithStream(SESSION, SKIPBINARY, RECURSE);
    }

    public void testExportSysView_stream_session_skipBinary_noRecurse()
            throws IOException, RepositoryException, SAXException {
        doTestWithStream(SESSION, SKIPBINARY, NORECURSE);
    }

    public void testExportSysView_stream_session_saveBinary_noRecurse()
            throws IOException, RepositoryException, SAXException {
        doTestWithStream(SESSION, SAVEBINARY, NORECURSE);
    }

    public void testExportSysView_stream_session_saveBinary_recurse()
            throws IOException, RepositoryException, SAXException {
        doTestWithStream(SESSION, SAVEBINARY, RECURSE);
    }

    /**
     * @throws RepositoryException
     * @throws SAXException
     * @throws IOException
     */
    public void doTestWithHandler(boolean workspace, boolean skipBinary, boolean noRecurse)
            throws RepositoryException, SAXException, IOException {

        ContentHandler contentHandler;
        try {

            contentHandler = new SysViewContentHandler(testPath, session, skipBinary, noRecurse);

            if (workspace) {
                //workspace.exportSysView(testPath, contentHandler, skipBinary, noRecurse);
            } else {
                session.exportSystemView(testPath, contentHandler, skipBinary, noRecurse);
            }
        } catch (RepositoryException re) {
            fail("Could not initialize the contenthandler due to: " + re.toString());
        }
    }

    /**
     * @throws RepositoryException
     * @throws SAXException
     * @throws IOException
     */
    public void doTestWithStream(boolean workSpace,
                                 boolean skipBinary, boolean noRecurse)
            throws RepositoryException, SAXException, IOException {

        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));

        Session thisSession = session;
        if (workSpace) {
            thisSession = workspace.getSession();
        }
        try {
            thisSession.exportSystemView(testPath, os, false, false);
            SysViewParser parser = new SysViewParser(testPath, thisSession, SAVEBINARY, RECURSE);
            parser.parse(file);
        } catch (RepositoryException re) {
            fail("Could not initialize the contenthandler due to: " + re.toString());
        } finally {
            os.close();
        }

    }

    /**
     * class to parse the XML file generated by the sysview export using an
     * OutputStream
     */
    protected class SysViewParser {
        //todo : test encoding of exported file
        // the path to the exported file
        String filePath;
        // the absolut path to the node which was exported
        String nodePath;
        Node node;
        XMLReader parser;
        SysViewContentHandler handler;

        public SysViewParser(String nodePath, Session session, boolean skipBinary, boolean noRecurse)
                throws SAXException, RepositoryException {
            this.nodePath = nodePath;
            this.handler = new SysViewContentHandler(nodePath, session, skipBinary, noRecurse);
            parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
            parser.setContentHandler(this.handler);
        }

        public void parse(File file) throws IOException, SAXException {
            FileInputStream in = null;
            try {
                in = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                fail("Input file not opened: " + e);
            }
            InputSource source = new InputSource(in);
            parser.parse(source);
        }
    }
}