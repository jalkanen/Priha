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
package org.priha.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.version.VersionException;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import org.priha.core.locks.LockManager;
import org.priha.core.namespace.NamespaceAware;
import org.priha.core.values.ValueFactoryImpl;
import org.priha.nodetype.GenericNodeType;
import org.priha.util.InvalidPathException;
import org.priha.util.Path;
import org.priha.util.PathFactory;
import org.priha.xml.StreamContentHandler;
import org.priha.xml.XMLExport;
import org.priha.xml.XMLImport;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 *  The SessionImpl class implements a JCR Session.  It is non thread safe,
 *  so each Thread must have its own Session.
 */
public class SessionImpl implements Session, NamespaceAware
{
    private static final String JCR_SYSTEM = "jcr:system";
    private RepositoryImpl m_repository;
    private WorkspaceImpl  m_workspace;
    private List<String>   m_lockTokens = new ArrayList<String>();
    
    private SimpleCredentials m_credentials;

    private Logger         log = Logger.getLogger( getClass().getName() );
    private boolean        m_isSuperSession = false;
    
    protected SessionProvider m_provider;
   
    public SessionImpl( RepositoryImpl rep, Credentials creds, String name )
        throws RepositoryException
    {
        m_repository = rep;
        m_workspace  = new WorkspaceImpl( this, name, rep.getProviderManager() );
        m_provider   = new SessionProvider( this, rep.getProviderManager() );
        
        if( !hasNode("/") )
        {
            repopulate();
        }
        
        if( creds instanceof SimpleCredentials )
        {
            m_credentials = (SimpleCredentials)creds;
        }
    }

    protected void setSuper(boolean value)
    {
        m_isSuperSession = true;
    }
    
    /**
     *  Returns true, if this Session should be considered to be a supersession,
     *  which can do whatever it wants (that is, mostly ignore any Constraint Violations.
     *  <p>
     *  One should be careful, since it is possible with this method to end up in
     *  a repository with an inconsistent state.
     *  
     *  @return
     */
    public boolean isSuper()
    {
        return m_isSuperSession;
    }
    
    public List<Path> listNodes( Path parentpath ) throws RepositoryException
    {
        return m_provider.listNodes(parentpath);
    }
    
    protected void markDirty( ItemImpl ii ) throws RepositoryException
    {
        if( ii instanceof NodeImpl )
            m_provider.addNode( (NodeImpl) ii );
        else
            m_provider.putProperty( (NodeImpl) ii.getParent(), (PropertyImpl)ii );
    }
    
    
    /**
     * Adds a node to the internal checklists
     *
     * @param node
     * @throws RepositoryException
     */
    void addNode( NodeImpl node )
        throws RepositoryException
    {
        try
        {   
            m_provider.addNode( node );
        }
        catch (InvalidPathException e)
        {
            throw new ItemNotFoundException(e.getMessage());
        }
    }

    boolean hasNode( String absPath ) throws RepositoryException
    {
        return hasNode( PathFactory.getPath(this,absPath) );
    }
    
    boolean hasNode( Path absPath ) throws RepositoryException
    {
        return m_provider.nodeExists( absPath );
    }

    boolean hasProperty( Path absPath ) throws RepositoryException
    {
        try
        {
            ItemImpl ii = m_provider.getItem(absPath);
            
            if( !ii.isNode() ) return true;
        }
        catch( PathNotFoundException e) {}
        catch( ItemNotFoundException e) {}
        
        return false;
    }
    
    public void addLockToken(String lt)
    {
        m_lockTokens.add(lt);
    }

    /**
     *  Priha does not mandate any permission checking.  This method just returns quietly, since
     *  everyone has all the permissions all the time.
     */
    public void checkPermission(String absPath, String actions) throws AccessControlException, RepositoryException
    {
        return;
    }

    public Object getAttribute(String name)
    {
        Object res = null;

        if( m_credentials != null )
        {
            res = m_credentials.getAttribute(name);
        }
        return res;
    }

    public String[] getAttributeNames()
    {
        if( m_credentials != null )
        {
            return m_credentials.getAttributeNames();
        }

        return new String[0];
    }


    public ItemImpl getItem( Path absPath ) throws PathNotFoundException, RepositoryException
    {
        ItemImpl ii = m_provider.getItem( absPath );
        
        return ii;
    }
    
    public ItemImpl getItem(String absPath) throws PathNotFoundException, RepositoryException
    {
        return getItem( PathFactory.getPath(this,absPath) );
    }

    public String[] getLockTokens()
    {
        return m_lockTokens.toArray( new String[m_lockTokens.size()] );
    }


    public NodeImpl getNodeByUUID(String uuid) throws ItemNotFoundException, RepositoryException
    {
        return m_provider.findByUUID(uuid);
    }

    public RepositoryImpl getRepository()
    {
        return m_repository;
    }

    public NodeImpl getRootNode() throws RepositoryException
    {
        return (NodeImpl) getItem(Path.ROOT);
    }

    public String getUserID()
    {
        return "anonymous";
    }

    public ValueFactory getValueFactory() throws UnsupportedRepositoryOperationException, RepositoryException
    {
        return ValueFactoryImpl.getInstance();
    }

    public WorkspaceImpl getWorkspace()
    {
        return m_workspace;
    }

    public boolean hasPendingChanges() throws RepositoryException
    {
        return m_provider.hasPendingChanges();
    }

    public Session impersonate(Credentials credentials) throws LoginException, RepositoryException
    {
        throw new UnsupportedRepositoryOperationException("Session.impersonate()");
        // TODO Auto-generated method stub
    }

    public boolean isLive()
    {
        return m_workspace != null;
    }

    public boolean itemExists(String absPath) throws RepositoryException
    {
        return itemExists( PathFactory.getPath(this,absPath) );
    }

    public void logout()
    {
        if( isLive() )
        {
            LockManager.getInstance(m_workspace).expireSessionLocks( this );
            m_workspace.logout();
            m_workspace = null;
        }
    }

    public void move(String srcAbsPath, String destAbsPath) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException
    {
        if( hasNode( destAbsPath ) ) throw new ItemExistsException("Destination node already exists!");
        
        NodeImpl srcnode = getRootNode().getNode(srcAbsPath);
        
        //System.out.println("Moving "+srcAbsPath+" to "+destAbsPath);
        
        String newDestPath = destAbsPath;

        LockManager lm = LockManager.getInstance( m_workspace );
        if( lm.hasChildLock(srcnode.getInternalPath()))
            throw new LockException("Lock on source path prevents move");

        Node destnode = getRootNode().addNode( newDestPath, srcnode.getPrimaryNodeType().getName() );
        
        for( NodeIterator ni = srcnode.getNodes(); ni.hasNext(); )
        {
            Node child = ni.nextNode();

            String relPath = srcnode.getName();
            
            move( child.getPath(), destAbsPath+"/"+relPath );
        }
        
        for( PropertyIterator pi = srcnode.getProperties(); pi.hasNext(); )
        {
            Property p = pi.nextProperty();

            //newDestPath = destAbsPath + "/" + nd.getName() + "/" + p.getName();
            //System.out.println("  property "+p.getPath()+" ==> "+newDestPath );
            
            if( !p.getName().equals("jcr:primaryType") )
            {
                if( p.getDefinition().isMultiple() )
                {
                    destnode.setProperty( p.getName(), p.getValues() );
                }
                else
                {
                    destnode.setProperty( p.getName(), p.getValue() );
                }
            }
        }
        
        srcnode.remove();
    }

    void refresh( boolean keepChanges, Path path ) throws RepositoryException
    {
        m_provider.refresh( keepChanges, path );
    }
    
    public void refresh(boolean keepChanges) throws RepositoryException
    {
        refresh( keepChanges, Path.ROOT );
    }

    /**
     *  This method makes sure that certain default stuff is available.
     *  @throws RepositoryException
     */
    private void repopulate() throws RepositoryException
    {
        if( !hasNode("/") )
        {
            NodeImpl ni = null;
            
            log.info("Repository empty; setting up root node...");

            GenericNodeType rootType = (GenericNodeType)getWorkspace().getNodeTypeManager().getNodeType("nt:unstructured");
            
            NodeDefinition nd = rootType.findNodeDefinition( new QName("*") );
            
            ni = new NodeImpl( this, "/", rootType, nd, true );

            ni.sanitize();
                
            m_provider.addNode( ni );
            ni.markModified(false);
                
            save();
        }
        
        if( !hasNode("/"+JCR_SYSTEM) )
        {
            getRootNode().addNode(JCR_SYSTEM, "nt:unstructured");

            // FIXME: Should probably set up all sorts of things.
            save();
        }
    }

    public void removeLockToken(String lt)
    {
        m_lockTokens.remove(lt);
    }

    public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException
    {
        saveNodes( Path.ROOT );
    }


    /**
     *  Saves all modified nodes that start with the given path prefix. This
     *  can be used to save a node and all its children.
     *  
     *  @param pathprefix
     *  @throws RepositoryException
     */
    protected void saveNodes( Path pathprefix ) throws RepositoryException
    {
        m_provider.save( pathprefix );
    }

    public boolean itemExists( Path absPath )
        throws RepositoryException
    {
        return hasNode(absPath) || hasProperty(absPath);
    }

    public void remove(ItemImpl itemImpl) throws RepositoryException
    {
        m_provider.remove( itemImpl );
    }

    public Collection<PropertyImpl> getReferences(String uuid) throws RepositoryException
    {
        return m_provider.getReferences( uuid );
    }

    /*  ========================================================================= */
    /*  XML import/export */
    
    public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException, RepositoryException
    {
        XMLImport importer = new XMLImport( this, false, PathFactory.getPath(this,parentAbsPath), uuidBehavior );
        
        try
        {
            importer.doImport( in );
        }
        catch (ParserConfigurationException e)
        {
            log.log( Level.WARNING, "Could not get SAX parser", e );
            throw new RepositoryException("Could not get SAX parser, please check logs.");
        }
        catch (SAXException e)
        {
            log.log( Level.WARNING, "Importing failed", e );
            
            if( e.getException() != null && e.getException() instanceof ItemExistsException ) 
                throw (ItemExistsException) e.getException();
            
            throw new InvalidSerializedDataException("Importing failed: "+e.getMessage());
        }
    }

    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, RepositoryException
    {
        XMLImport importer = new XMLImport( this, false, PathFactory.getPath(this,parentAbsPath), uuidBehavior );

        return importer;
    }


    public void exportDocumentView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException("Session.exportDocumentView()");
    }

    public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException
    {
        // TODO Auto-generated method stub

        throw new UnsupportedRepositoryOperationException("Session.exportDocumentView2()");
    }

    public void exportSystemView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException
    {
        XMLExport export = new XMLExport( this );
        
        export.export( absPath, contentHandler, skipBinary, noRecurse );
    }

    public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException
    {
        XMLExport export = new XMLExport( this );

        ContentHandler handler = new StreamContentHandler( out );
        
        try
        {
            export.export( absPath, handler, skipBinary, noRecurse );
        }
        catch (SAXException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /*  ========================================================================= */
    /*  Namespaces */

    public void setNamespacePrefix(String newPrefix, String existingUri) throws NamespaceException, RepositoryException
    {
        String oldPrefix = m_workspace.getNamespaceRegistry().getPrefix(existingUri);
        
        if( newPrefix.toLowerCase().startsWith("xml") )
            throw new NamespaceException("No namespace starting with 'xml' may be remapped (6.3.3)");
        
        String currentUri = null;
        try
        {
            currentUri = m_workspace.getNamespaceRegistry().getURI( newPrefix );
        }
        catch( NamespaceException e ) {}
        
        if( currentUri != null && currentUri.equals(existingUri) )
        {
            throw new NamespaceException("Existing prefix cannot be remapped (6.3.3)");
        }
        
        m_workspace.getNamespaceRegistry().registerNamespace(newPrefix, existingUri );
    }

    public String getNamespacePrefix(String uri) throws NamespaceException, RepositoryException
    {
        try
        {
            return m_workspace.getNamespaceRegistry().getPrefix(uri);
        }
        catch( NamespaceException e )
        {
            return m_workspace.getNamespaceRegistry().getPrefix(uri);
        }
    }

    public String[] getNamespacePrefixes() throws RepositoryException
    {
        TreeSet<String> result = new TreeSet<String>();
        
        String[] uris = m_repository.getGlobalNamespaceRegistry().getURIs();
        String[] uris2 = m_workspace.getNamespaceRegistry().getURIs();
        
        TreeSet<String> uriCol = new TreeSet<String>();
        uriCol.addAll( Arrays.asList(uris) );
        uriCol.addAll( Arrays.asList(uris2) );

        for( String s : uriCol )
        {
            result.add( getNamespacePrefix(s) );
        }

        return result.toArray(new String[0]);
    }

    public String getNamespaceURI(String prefix) throws NamespaceException, RepositoryException
    {
        try
        {
            return m_workspace.getNamespaceRegistry().getURI(prefix);
        }
        catch( NamespaceException e )
        {
            return m_repository.getGlobalNamespaceRegistry().getURI(prefix);
        }
    }

    public String fromQName(QName c)
    {
        try
        {
            return m_workspace.getNamespaceRegistry().fromQName(c);
        }
        catch( Exception e )
        {
            try
            {
                return m_repository.getGlobalNamespaceRegistry().fromQName(c);
            }
            catch (Exception e1)
            {
            }
        }
        
        return c.getLocalPart();
    }

    public QName toQName(String c)
    {
        try
        {
            return m_workspace.getNamespaceRegistry().toQName(c);
        }
        catch( Exception e )
        {
            try
            {
                return m_repository.getGlobalNamespaceRegistry().toQName(c);
            }
            catch (Exception e1)
            {
            }
        }
        
        return new QName(c);
    }
}
