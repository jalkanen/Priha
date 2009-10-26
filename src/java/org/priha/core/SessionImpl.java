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
import javax.jcr.version.VersionException;
import javax.xml.parsers.ParserConfigurationException;

import org.priha.core.locks.QLock;
import org.priha.core.locks.LockManager;
import org.priha.core.namespace.NamespaceMapper;
import org.priha.core.values.ValueFactoryImpl;
import org.priha.nodetype.QNodeDefinition;
import org.priha.nodetype.QNodeType;
import org.priha.path.Path;
import org.priha.path.PathFactory;
import org.priha.path.PathManager;
import org.priha.path.PathRef;
import org.priha.path.Path.Component;
import org.priha.util.*;
import org.priha.version.VersionHistoryImpl;
import org.priha.version.VersionImpl;
import org.priha.xml.*;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 *  The SessionImpl class implements a JCR Session.  It is non thread safe,
 *  so each Thread must have its own Session.
 */
public class SessionImpl implements Session, NamespaceMapper
{
    static final String PRIHA_OLD_PATH = "priha:oldPath";

    private static final String JCR_SYSTEM = "jcr:system";
    
    /**
     *  This is a magical property which gets added to a Node whenever
     *  a move occurs.  It's value is a Path which points from source to target
     *  and target to source nodes which have been saved - it's point is
     *  to make sure that the user will save both the source and the target
     *  at the same time.  This property is never saved to the Provider.
     */
    static final String MOVE_CONSTRAINT = "priha:moveConstraint";
    
    private RepositoryImpl m_repository;
    private WorkspaceImpl  m_workspace;
    private List<String>   m_lockTokens = new ArrayList<String>();
    
    private SimpleCredentials m_credentials;

    private Logger         log = Logger.getLogger( getClass().getName() );
    private boolean        m_isSuperSession = false;
    
    private ValueFactoryImpl m_valueFactory = new ValueFactoryImpl(this);
    
    protected SessionProvider m_provider;
   
    public SessionImpl( RepositoryImpl rep, Credentials creds, String name )
        throws RepositoryException
    {
        m_repository = rep;
        m_workspace  = new WorkspaceImpl( this, name, rep.getProviderManager() );
        m_provider   = new SessionProvider( this, rep.getProviderManager() );
        
        if( creds instanceof SimpleCredentials )
        {
            m_credentials = (SimpleCredentials)creds;
        }

        if( !hasNode("/") )
        {
            repopulate();
        }
    }

    public boolean setSuper(boolean value)
    {
        boolean oldval = m_isSuperSession;
        m_isSuperSession = value;
        
        return oldval;
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
            m_provider.putProperty( ii.getParent(), (PropertyImpl)ii );
    }

    boolean hasNode( String absPath ) throws RepositoryException
    {
        return hasNode( PathFactory.getPath(this,absPath) );
    }
    
    public boolean hasNode( Path absPath ) throws RepositoryException
    {
        checkLive();
        return m_provider.nodeExists( absPath );
    }

    boolean hasProperty( Path absPath ) throws RepositoryException
    {
        checkLive();
        try
        {
            ItemImpl ii = m_provider.getItem(absPath);
            
            if( ii.getState() == ItemState.REMOVED ) return false;
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
     *  Any credentials are fine to give full access.
     */
    public void checkPermission(String absPath, String actions) throws AccessControlException, RepositoryException
    {
        checkLive();
        actions = actions.trim();
        
        if( actions.equals( "read" ) )
            return;
        
        if( m_credentials == null && !isSuper() ) 
            throw new AccessControlException("Read-only session");
        
        return;
    }

    /** Quick way to check for write permissions. */
    protected void checkWritePermission() throws AccessControlException, RepositoryException
    {
        try
        {
            checkPermission("/","add_node");
        }
        catch( AccessControlException e )
        {
            throw new AccessDeniedException(e.getMessage());
        }
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
        checkLive();
        
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
        checkLive();
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

    public ValueFactoryImpl getValueFactory() throws UnsupportedRepositoryOperationException, RepositoryException
    {
        checkLive();
        return m_valueFactory;
    }

    public WorkspaceImpl getWorkspace()
    {
        return m_workspace;
    }

    public boolean hasPendingChanges() throws RepositoryException
    {
        checkLive();
        return m_provider.hasPendingChanges();
    }

    public Session impersonate(Credentials credentials) throws LoginException, RepositoryException
    {
        checkLive();
        
        return m_repository.login( credentials, getWorkspace().getName() );
    }

    public final boolean isLive()
    {
        return m_workspace != null;
    }

    private final void checkLive() throws RepositoryException
    {
        if( !isLive() ) throw new RepositoryException("This Session is no longer live and cannot be used.");
    }
    
    public boolean itemExists(String absPath) throws RepositoryException
    {
        checkLive();
        return itemExists( PathFactory.getPath(this,absPath) );
    }

    public void logout()
    {
        if( isLive() )
        {
            m_repository.removeSession(this);
            LockManager.getInstance(m_workspace).expireSessionLocks( this );
            m_workspace.logout();
            m_workspace = null;
        }
    }

    /**
     *  Moves work as follows:
     *  <ol>
     *  <li>We add a new Node to the destAbsPath
     *  <li>We copy all properties from the old Node to the new Node
     *  <li>The old Node gets tagged with a property "priha:oldPath" which contains the current path
     *  <li>The Path of the old Node (and all other Nodes which refer to it) is changed to point at the new location
     *  <li>The Node is marked as being MOVED instead of REMOVED.
     *  </ol>
     */
    public void move(String srcAbsPath, String destAbsPath) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException
    {
        checkLive();
        checkWritePermission();

        System.out.println("Moving "+srcAbsPath+" to "+destAbsPath);


        if( destAbsPath.endsWith("]") && !isSuper() )
            throw new ConstraintViolationException("Destination path must not have an index as its final component.");        

        boolean isSuper = setSuper( true );
                
        try
        {
            Path path = PathFactory.getPath( this, destAbsPath );

            if( m_workspace.isCheckedIn( (NodeImpl)getItem(path.getParentPath()) ) )
                throw new VersionException("Versioned node checked in");

            if( hasNode( path ) ) 
            {
                throw new ItemExistsException("Destination node "+getWorkspace().getName()+":"+path.toString(this)+" already exists!");
            }
        
            NodeImpl srcnode = getRootNode().getNode(srcAbsPath);
        
            String newDestPath = destAbsPath;

            LockManager lm = LockManager.getInstance( m_workspace );
            if( lm.findLock( srcnode.getParent().getInternalPath() ) != null )
                throw new LockException( "Lock on source path prevents move" );

            QLock lock = lm.findLock( srcnode.getInternalPath() );
            if( lock != null )
                lm.moveLock( lock, PathFactory.getPath(newDestPath) );
            
            NodeImpl destnode = getRootNode().addNode( newDestPath, srcnode.getPrimaryNodeType().getName() );

            destnode.tag( PRIHA_OLD_PATH, srcnode.getPath() );

            for( NodeIterator ni = srcnode.getNodes(); ni.hasNext(); )
            {
                Node child = ni.nextNode();

                String relPath = child.getName();

                String newPath = destAbsPath + "/" + relPath;
                System.out.println("Child path move "+child.getPath()+" => "+newPath );
                move( child.getPath(), newPath );
            }
        
            for( PropertyIterator pi = srcnode.getProperties(); pi.hasNext(); )
            {
                PropertyImpl p = (PropertyImpl)pi.nextProperty();

                // newDestPath = destAbsPath + "/" + nd.getName() + "/" +
                // p.getName();
                // System.out.println(" property "+p.getPath()+" ==> "+newDestPath
                // );
            
                // Primary type has already been set, so we won't move that.
                
                if( !p.getName().equals( "jcr:primaryType" ) )
                {
                    if( p.getDefinition().isMultiple() )
                    {
                        PropertyImpl pix = destnode.internalSetProperty( p.getQName(), p.getValues(), p.getType() );
                        pix.setState( ItemState.NEW );
                    }
                    else
                    {
                        destnode.setProperty( p.getName(), p.getValue() );
                    }
                }
                p.remove();
            }

            // Set up move constraints (that is, making sure the user can only save
            // a top node, and not just moved bits).
            
            srcnode.getParent().tag( MOVE_CONSTRAINT, destnode.getParent().getPath() );
            destnode.getParent().tag( MOVE_CONSTRAINT, srcnode.getParent().getPath() );
            
            // Since this is a move op, we want to make sure that the old path
            // does not change.
            
            //srcnode.freezePath();
            
            // Finally, remove the source node.
            
            //srcnode.remove();
            srcnode.setState( ItemState.MOVED );
//            m_provider.remove( srcnode ); // FIXME: Make sure this is ok
            
            getPathManager().move( srcnode.getInternalPath(), destnode.getInternalPath() ); // FIXME: OK with new ChangeStore?
        }
        finally
        {
            setSuper( isSuper );
        }
    }

    void refresh( boolean keepChanges, Path path ) throws RepositoryException
    {
        checkLive();
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
        boolean isSuper = setSuper(true);
        
        if( !hasNode("/") )
        {
            NodeImpl ni = null;
            
            log.info("Repository empty; setting up root node...");

            QNodeType rootType = getWorkspace().getNodeTypeManager().getNodeType("nt:unstructured").getQNodeType();
                     
            QNodeDefinition nd = rootType.findNodeDefinition( QName.valueOf("*") );
            
            ni = new NodeImpl( this, "/", rootType, nd, true );
            ni.setState( ItemState.NEW );
            ni.addMixin( "mix:referenceable" ); // Make referenceable.
            
            //m_provider.addNode( ni );
            
            ni.sanitize();
                            
            save();
        }
        
        if( !hasNode("/"+JCR_SYSTEM) )
        {
            getRootNode().addNode(JCR_SYSTEM, "nt:unstructured");

            // FIXME: Should probably set up all sorts of things.
            save();
        }
        
        setSuper(isSuper);
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
        checkLive();
        checkWritePermission();
        m_provider.save( pathprefix );
    }

    public boolean itemExists( Path absPath )
        throws RepositoryException
    {
        return hasNode(absPath) || hasProperty(absPath);
    }

    public void remove(ItemImpl itemImpl) throws RepositoryException
    {
        checkLive();
        checkWritePermission();
        m_provider.remove( itemImpl );
    }

    public List<PropertyImpl> getReferences(String uuid) throws RepositoryException
    {
        checkLive();
        return m_provider.getReferences( uuid );
    }

    /*  ========================================================================= */
    /*  XML import/export */
    
    public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException, RepositoryException
    {
        checkLive();
        checkWritePermission();
        
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
            
            if( e.getException() != null && e.getException() instanceof RepositoryException ) 
                throw (RepositoryException) e.getException();
            
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
        checkLive();
        
        XMLExport export = new XMLDocExport(this);

        export.export( absPath, contentHandler, skipBinary, noRecurse );
    }

    public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException
    {
        checkLive();
        XMLExport export = new XMLDocExport( this );

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

    public void exportSystemView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException
    {
        checkLive();
        XMLExport export = new XMLSysExport( this );
        
        export.export( absPath, contentHandler, skipBinary, noRecurse );
    }

    public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException
    {
        checkLive();
        XMLExport export = new XMLSysExport( this );

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
    /** Maps prefixes to URIs.  Prefixes are always unique, therefore they are the keys */
    private HashMap<String,String> m_nsmap = new HashMap<String,String>();

    public void setNamespacePrefix(String newPrefix, String existingUri) throws NamespaceException, RepositoryException
    {
        checkLive();
        
        // Throws an exception if the URI does not exist, so this is a cheap way to check for validity.
        m_workspace.getNamespaceRegistry().getPrefix(existingUri);
        
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
        
        PathFactory.reset();
        m_nsmap.put(newPrefix, existingUri );
    }

    public String getNamespacePrefix(String uri) throws NamespaceException, RepositoryException
    {
        checkLive();
        for( Map.Entry<String, String> e : m_nsmap.entrySet() )
        {
            if( e.getValue().equals( uri ) ) return e.getKey();
        }
        
        return m_workspace.getNamespaceRegistry().getPrefix(uri);
    }

    public String[] getNamespacePrefixes() throws RepositoryException
    {
        checkLive();
        Set<String> prefixes = new TreeSet<String>();
        
        Set<String> uris = new TreeSet<String>();
        uris.addAll( m_nsmap.values() );
        uris.addAll( Arrays.asList( m_workspace.getNamespaceRegistry().getURIs() ) );
        
        for( String u : uris )
        {
            prefixes.add( getNamespacePrefix( u ) );
        }
        
        return prefixes.toArray( new String[prefixes.size()] );
    }

    public String getNamespaceURI(String prefix) throws NamespaceException, RepositoryException
    {
        checkLive();
        String uri = m_nsmap.get(prefix);
        
        if( uri == null )
        {
            return m_workspace.getNamespaceRegistry().getURI(prefix);
        }
        
        return uri;
    }

    public String fromQName(QName c)
    {
        if( c == null ) return null;
        
        try
        {
            String uri = c.getNamespaceURI();
            
            if( uri.length() > 0 ) 
            {   
                String prefix = getNamespacePrefix( uri );
            
                return prefix+":"+c.getLocalPart();
            }
        }
        catch( Exception e )
        {
        }
        
        return c.getLocalPart();
    }

    public QName toQName(String c) throws NamespaceException, RepositoryException
    {
        checkLive();
        if( c == null ) return null;
        
        if( c.indexOf( '{' ) != -1 ) throw new RepositoryException("Already in QName format: "+c);

        int idx = c.indexOf(':');
        if( idx != -1 )
        {
            String prefix = c.substring(0,idx);
            String name   = c.substring(idx+1);
                
            String uri = getNamespaceURI( prefix );
        
            return new QName( uri, name, prefix );
        }
        
        return new QName(c);
    }
    
    /**
     *  This method creates a correct Node subclass based on the NodeType.  It
     *  can return Version or VersionHistory objects, as well as regular Nodes. 
     * 
     *  @param absPath
     *  @param assignedType
     *  @param assignedNodeDef
     *  @return
     *  @throws RepositoryException
     */
    protected NodeImpl createNode(Path            absPath, 
                                  QNodeType       assignedType, 
                                  QNodeDefinition assignedNodeDef,
                                  boolean         initDefaults)
        throws RepositoryException
    {
        NodeImpl ni;
        
        if( assignedType.isNodeType(JCRConstants.Q_NT_VERSION) )
        {
            ni = new VersionImpl( this, absPath, assignedType, assignedNodeDef, initDefaults );
        }
        else if( assignedType.isNodeType(JCRConstants.Q_NT_VERSIONHISTORY) )
        {
            ni = new VersionHistoryImpl( this, absPath, assignedType, assignedNodeDef, initDefaults );                
        }
        else
        {
            ni = new NodeImpl( this, absPath, assignedType, assignedNodeDef, initDefaults );
        }
        
        return ni;
    }
    
    public String toString()
    {
        return "Session["+(isSuper()?"SUPER":"")+"]";
    }

    /**
     *  This method provides debug information about the state of the Session.  Do not rely
     *  it giving it in any particular format.  The dump will be done to the System.out stream.
     */
    public void dump()
    {
        System.out.println(this);
        m_provider.m_changedItems.dump();
    }
    
    /**
     * Shortcut for getPathManager().getPath().
     * @param p
     * @return
     * @throws PathNotFoundException
     */
    public Path getPath(PathRef p) throws PathNotFoundException
    {
        return m_provider.getPath( p );
    }

    public PathManager getPathManager()
    {
        return m_provider.getPathManager();
    }

    public void rename(Path path1, Component newName)
    {
        System.out.println("Renaming "+path1+" to "+newName);
    }
}
