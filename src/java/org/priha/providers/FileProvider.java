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
package org.priha.providers;

import static org.priha.core.JCRConstants.Q_JCR_PRIMARYTYPE;
import static org.priha.core.JCRConstants.Q_JCR_UUID;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import javax.jcr.*;

import org.priha.core.ItemType;
import org.priha.core.RepositoryImpl;
import org.priha.core.WorkspaceImpl;
import org.priha.core.binary.FileBinarySource;
import org.priha.core.values.QValue;
import org.priha.core.values.ValueFactoryImpl;
import org.priha.core.values.ValueImpl;
import org.priha.nodetype.QNodeDefinition;
import org.priha.path.InvalidPathException;
import org.priha.path.Path;
import org.priha.path.PathFactory;
import org.priha.path.Path.Component;
import org.priha.util.*;

/**
 *  A simple file system -based provider.  This is not particularly optimized.
 *  Stores UUIDs and references as journaling files which are compacted
 *  at every N writes and on shutdown.  If there is a power outage just
 *  in the middle of this process, it's possible that the file ends up
 *  being corrupted.  Currently there is no way to rebuild the UUIDs.
 *  <p>
 *  The FileProvider does not support failure recovery at the moment.  So it's probably not
 *  the best provider for production systems.
 */
public class FileProvider implements RepositoryProvider, PerformanceReporter
{
    private static final String PROP_NUM_PROPERTIES = "numProperties";
    private static final String PROP_MULTIPLE       = "multiple";
    private static final String PROP_TYPE           = "type";
    private static final String PROP_PATH           = "path";
    private static final int    BUFFER_SIZE         = 4096;
    private static final String LOCKFILE            = ".prihalock";
    
    private String m_root;
    private String m_workspaceRoot;
    
    private Logger log = Logger.getLogger( getClass().getName() );
    
    private long[] m_hitCount;
    
    private Path m_systemPath;
    
    private Map<String,UUIDObjectStore<Path[]>> m_references = new HashMap<String, UUIDObjectStore<Path[]>>();
    private Map<String,UUIDObjectStore<Path>>   m_uuids      = new HashMap<String, UUIDObjectStore<Path>>();
        
    private Map<String,String> m_mangledNames = new Hashtable<String, String>();
    private long m_cacheMisses = 0;
    private long m_cacheHits   = 0;

    public FileProvider() throws RepositoryException
    {
        resetCounts();
    }

    /**
     * Returns the File which houses all of the workspaces.
     * @return
     */
    private String getWorkspaceRoot()
    {
        if( m_workspaceRoot == null )
        {
            m_workspaceRoot = m_root + "/workspaces";
        }
        
        return m_workspaceRoot;
    }
    
    /**
     *  Figures out what the filename for a Workspace is.
     * @param ws
     * @return
     */
    // FIXME: Should escape the filename properly.
    private String getWorkspaceFilename( Workspace ws )
    {
        return ws.getName();
    }
    
    /**
     *  Figures out the what the filename for a given path is. 
     *  @param path
     *  @return
     * @throws RepositoryException 
     * @throws NamespaceException 
     */
    // FIXME: Should escape the path properly
    private String getPathFilename( Path path ) throws NamespaceException, RepositoryException
    {
        String p = path.toString( RepositoryImpl.getGlobalNamespaceRegistry() );
        
        return mangleName(p);
    }

    /**
     *  Create a valid filename based on the QName and a suffix.
     *  
     *  @param name
     *  @param suffix
     *  @return
     */
    private String makeFilename( QName name, String suffix )
    {
        String filename;
        
        try
        {
            filename = RepositoryImpl.getGlobalNamespaceRegistry().fromQName(name);
        }
        catch( NamespaceException e )
        {
            filename = name.getLocalPart();
        }

        if( suffix != null ) filename = filename + suffix;
        
        return mangleName(filename);
    }
    
    /**
     *  Returns the directory for a particular Workspace.
     *  
     *  @param wsname
     *  @return
     */
    private String getWorkspaceDir( String wsname )
    {
        String wsDir = getWorkspaceRoot() + "/" + wsname;

        return wsDir;
    }
    
    /**
     *  Returns the directory in which a particular Node in a particular workspace resides
     *  
     *  @param ws
     *  @param path
     *  @return
     * @throws RepositoryException 
     * @throws NamespaceException 
     */
    private File getNodeDir( Workspace ws, Path path ) throws NamespaceException, RepositoryException
    {
        if( m_systemPath.isParentOf( path ) || m_systemPath.equals( path ) ) 
        {
            return new File( m_root, getPathFilename(path) );
        }
        
        String wsDir = getWorkspaceDir( getWorkspaceFilename(ws) );
        File nodeDir = new File( wsDir, getPathFilename(path) );
        
        return nodeDir;
    }
    
    public void addNode(StoreTransaction tx, Path path, QNodeDefinition def) throws RepositoryException
    {
        m_hitCount[Count.AddNode.ordinal()]++;

        WorkspaceImpl ws = tx.getWorkspace();
        File nodeDir = getNodeDir( ws, path );

        nodeDir.mkdirs();
        
        if( !path.isRoot() )
        {
            List<String> order = getOrder(ws,path.getParentPath());

            if( !order.contains( path.getLastComponent().toString() ) )
            {
                order.add( path.getLastComponent().toString() );
            
                saveOrder( ws, path.getParentPath(), order );
            }
        }
        
    }

    private void acquirePaths( Path startPath, File dir, List<Path> list, boolean recurse )
    {
        File[] files = dir.listFiles();
        
        if( files == null || files.length == 0 ) return;
        
        for( File f : files )
        {
            if( f.isDirectory() )
            {
                try
                {
                    Properties props = getPropertyInfo( f, Q_JCR_PRIMARYTYPE );
                    
                    Path p = PathFactory.getPath( props.getProperty( PROP_PATH ) );
                   
                    list.add( p.getParentPath() );
                
                    if( recurse )
                    {
                        acquirePaths( p.getParentPath(), f, list, recurse );
                    }
                }
                catch( PathNotFoundException e )
                {
                    // Skip, don't include.
                }
                catch( IOException e )
                {
                    // Skip, don't include.
                }
                catch (NamespaceException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (InvalidPathException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (RepositoryException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch( NullPointerException e )
                {
                    e.printStackTrace();
                }
            }
        }
    }
    

    public List<String> listWorkspaces()
    {
        m_hitCount[Count.ListWorkspaces.ordinal()]++;

        ArrayList<String> list = new ArrayList<String>();
        
        File[] dirs = new File(getWorkspaceRoot()).listFiles();
        
        if( dirs != null )
        {
            for( File f : dirs )
            {
                if( f.isDirectory() )
                {
                    list.add( f.getName() );
                }
            }
        }
        
        return list;
    }

    private boolean deleteContents( File dir )
    {
        // System.out.println("Deleting "+dir.getAbsolutePath());
        for( File f : dir.listFiles() )
        {
            if( f.isDirectory() )
            {
                deleteContents( f );
            }
            
            if( !f.delete() ) return false;
        }
        
        return true;
    }

    public void start(RepositoryImpl rep, Properties props) throws ConfigurationException
    {
        m_hitCount[Count.Start.ordinal()]++;
        
        try
        {
            m_systemPath = PathFactory.getPath( RepositoryImpl.getGlobalNamespaceRegistry(), 
                                                "/jcr:system" );
        }
        catch( RepositoryException e )
        {
            throw new ConfigurationException("Unable to create a /jcr:system instance: "+e.getMessage());
        }

        String wsList = props.getProperty("workspaces", "default");
        m_root = props.getProperty("directory");

        //
        //  Make sure that root exists.
        //
        File root = new File(m_root);
        root.mkdirs();
        
        //
        //  Make sure this instance is the only one using that root.
        //
        File lockFile = new File(m_root, LOCKFILE);
        
        if( lockFile.exists() )
        {
            String msg = "Cannot use the Priha FileProvider repository while another instance of Priha is using it. "+
                         "If you are sure there are no other instances using this same repository, please "+
                         "remove the '"+lockFile.getAbsolutePath()+"' file and restart.";
            log.severe( msg );
            throw new ConfigurationException(msg);
        }
        
        //
        //  Create the lock file; and make sure it gets cleaned up properly in 
        //  most cases.
        //
        try
        {
            lockFile.createNewFile();
            lockFile.deleteOnExit();
        }
        catch( IOException e )
        {
            throw new ConfigurationException("Cannot start Priha ",e);
        }
        
        //
        //  Initializing the workspaces.
        //
        log.fine("Initializing FileProvider with root "+m_root);
        
        String[] workspaces = wsList.split("\\s");
        
        for( String wsname : workspaces )
        {
            File wsroot = new File(getWorkspaceDir(wsname));
        
            if( !wsroot.exists() )
            {
                wsroot.mkdirs();
                log.finer("Created workspace directory "+wsroot);
            }

            //
            //  Reset the workspaces.
            //
            m_references.put( wsname, 
                              new UUIDObjectStore<Path[]>(mangleName(wsname)+"-references.ser") );
            m_uuids.put( wsname, 
                         new UUIDObjectStore<Path>(mangleName(wsname)+"-uuid.ser") );
        }
    }

    public void stop(RepositoryImpl rep)
    {
        m_hitCount[Count.Stop.ordinal()]++;
     
        log.fine("Shutting down FileProvider...");
        
        for( UUIDObjectStore<Path[]> s : m_references.values() )
        {
            s.shutdown();
        }
        
        for( UUIDObjectStore<Path> s : m_uuids.values() )
        {
            s.shutdown();
        }
        
        m_references.clear();
        m_uuids.clear();
        
        //
        //  Clean up the lockfile, in case we're e.g. switching repos mid-flight.
        //
        File lockFile = new File(m_root, LOCKFILE);
        lockFile.delete();
        
        log.finer("Manglename cache hits = "+m_cacheHits+", misses = "+m_cacheMisses);
    }

    public void copy(WorkspaceImpl ws, Path srcpath, Path destpath) throws RepositoryException
    {
        m_hitCount[Count.Copy.ordinal()]++;

        throw new UnsupportedRepositoryOperationException("copy()");
    }

    public List<Path> listNodes(WorkspaceImpl ws, Path parentpath) throws NamespaceException, RepositoryException
    {
        m_hitCount[Count.ListNodes.ordinal()]++;

        ArrayList<Path> list = new ArrayList<Path>();

        File startPath = getNodeDir( ws, parentpath );
        
        if( !startPath.exists() ) throw new PathNotFoundException("No such path found: "+parentpath);
        
        //acquirePaths( parentpath, startPath, list, false );
        
        List<String> orderArray = getOrder( ws, parentpath );
        
        for( String s : orderArray )
        {
            Path childPath = parentpath.resolve( Path.Component.valueOf( s ) );
            list.add( childPath );
        }
        
        //
        //  The .order -file will probably not contain the system path for anyone else
        //  except the workspace which was created first.
        //
        if( parentpath.isRoot() && !list.contains( m_systemPath ))
        {
            list.add( 0, m_systemPath );
        }
        
        return list;
    }

    public List<QName> listProperties(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        m_hitCount[Count.ListProperties.ordinal()]++;

        File nodeDir = getNodeDir( ws, path );
        List<QName> proplist = new ArrayList<QName>();
        
        try
        {
            File[] files = nodeDir.listFiles( new PropertyTypeFilter() );
            
            if( files != null )
            {
                for( File propertyFile : files )
                {
                    Properties props;
                    FileInputStream in = null;
                    
                    try
                    {
                        in = new FileInputStream(propertyFile);
                
                        props = FastPropertyStore.load(in);
        
                        String qname = props.getProperty(PROP_PATH);
                
                        proplist.add( PathFactory.getPath( qname ).getLastComponent() );
                    }
                    finally
                    {
                        if( in != null ) in.close();
                    }
                }
            }
        }
        catch( IOException e )
        {
            throw new RepositoryException("Thingy said booboo", e);
        }
        
        return proplist;
    }

    public void move(WorkspaceImpl ws, Path srcpath, Path destpath) throws RepositoryException
    {
        m_hitCount[Count.Move.ordinal()]++;

        throw new UnsupportedRepositoryOperationException("move()");
    }

    /**
     *  A Node exists only if it has a primaryType.info in the directory.
     */
    public boolean itemExists(WorkspaceImpl ws, Path path, ItemType type)
    {
        m_hitCount[Count.ItemExists.ordinal()]++;

        File nodeDir;
        try
        {
            File propFile;
            
            if( type == ItemType.NODE )
            {
                nodeDir = getNodeDir( ws, path );
                
                propFile = new File( nodeDir, mangleName("jcr:primaryType.info") );
            }
            else if( type == ItemType.PROPERTY )
            {
                nodeDir = getNodeDir( ws, path.getParentPath() );
                
                propFile = new File( nodeDir, makeFilename( path.getLastComponent(), ".info" ) );
            }
            else
            {
                throw new IllegalArgumentException("Type "+type.name()+" not supported");
            }
            
            return propFile.exists();
        }
        catch( NamespaceException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch( RepositoryException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return false;
    }

    public void open(RepositoryImpl rep, Credentials credentials, String workspaceName) throws RepositoryException, NoSuchWorkspaceException
    {
        m_hitCount[Count.Open.ordinal()]++;

        List<String> wsnames = listWorkspaces();
        
        if( wsnames.indexOf(workspaceName) == -1 )
            throw new NoSuchWorkspaceException(workspaceName);
        
        log.finer("Workspace "+workspaceName+" has been opened.");
    }

    public void close(WorkspaceImpl ws)
    {
        log.finer( "Workspace "+ws.getName()+" closing..." );
        m_hitCount[Count.Close.ordinal()]++;
        /*
        try
        {
            m_uuids.get( ws.getName() ).serialize();
        }
        catch( IOException e )
        {
            log.info( "Unable to store UUID references upon workspace logout."+e.getMessage() );
        }
        catch( NullPointerException e )
        {
            log.warning( "A non-configured workspace '"+ws.getName()+"' detected.  If you are using a multiprovider configuration, please check this workspace actually exists." );
            return;
        }
        
        try
        {
            m_references.get( ws.getName() ).serialize();
        }
        catch( IOException e )
        {
            log.info( "Unable to store UUID references upon workspace logout."+e.getMessage() );
        }
        */
    }
    
    private void writeValue( File f, ValueImpl v ) throws IOException, IllegalStateException, RepositoryException
    {
        OutputStream out = null;
        InputStream in   = null;      
        
        try
        {
            if( v instanceof QValue.QValueInner )
            {
                byte[] ba = ((QValue.QValueInner)v).getQValue().getString().getBytes("UTF-8");
                in = new ByteArrayInputStream(ba);
            }
            else
            {
                in = v.getStream();
            }
            
            out = new FileOutputStream( f );
        
            FileUtil.copyContents( in, out );
        }
        finally
        {
            //
            //  ...and close all the streams.
            //
            if( out != null ) 
            {
                try
                {
                    out.close();
                }
                finally
                {
                    if( in != null ) in.close();
                }
            }
        }
    }
    
    public void putPropertyValue(StoreTransaction tx, Path path, ValueContainer vc) throws RepositoryException
    {
        m_hitCount[Count.PutPropertyValue.ordinal()]++;

        WorkspaceImpl ws = tx.getWorkspace();
        File nodeDir = getNodeDir( ws, path.getParentPath() );

        saveUuidShortcut(ws,path,vc);
        
        QName qname = path.getLastComponent();
        
        File inf = new File( nodeDir, makeFilename( qname, ".info" ) );
        
        Properties props = new Properties();
        
        props.setProperty( PROP_PATH, path.toString() );
        props.setProperty( PROP_TYPE,  PropertyType.nameFromValue( vc.getType() ) );
        props.setProperty( PROP_MULTIPLE, vc.isMultiple() ? "true" : "false" );

        OutputStream out = null;
        
        try
        {
            if( vc.isMultiple() )
            {
                props.setProperty( PROP_NUM_PROPERTIES, Integer.toString(vc.getValues().length) );
                Value[] values = vc.getValues();

                //
                //  Let's clear previous references, if this is an attempt to save
                //  a null value.
                //
                if( vc.getType() == PropertyType.REFERENCE )
                {
                    try
                    {
                        ValueImpl[] oldval = getPropertyValue( ws, path ).getValues();
                    
                        for( ValueImpl vi : oldval )
                        {
                            String uuid = vi.getString();
                            cleanRefMapping( ws, path, uuid );
                        }
                    }
                    catch( PathNotFoundException e ){} // This is okay
                }
                
                for( int i = 0; i < values.length; i++ )
                {
                    File df = new File( nodeDir, makeFilename( qname, "."+i+".data" ) );
                    saveRefShortcut( ws, path, (ValueImpl)values[i] );
                    writeValue( df, (ValueImpl)values[i] );
                }
                // Remove the rest of old values
                
                int i = values.length;
                while(true)
                {
                    File df = new File( nodeDir, makeFilename( qname, "."+i+".data" ) );
                    if( df.exists() ) df.delete();
                    else break;
                }
            }
            else
            {
                if( vc.getType() == PropertyType.REFERENCE )
                {
                    try
                    {
                        ValueImpl oldval = getPropertyValue( ws, path ).getValue();
                        cleanRefMapping( ws, path, oldval.getString() );
                    }
                    catch(PathNotFoundException e) {} // OK

                    saveRefShortcut( ws, path, vc.getValue() );
                }

                File df = new File( nodeDir, makeFilename( qname, ".data" ) );
                writeValue( df, vc.getValue() );
            }
            out = new FileOutputStream(inf);
            
            FastPropertyStore.store( out, props );
        }
        catch (FileNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally
        {
            if( out != null )
            {
                try
                {
                    out.close();
                }
                catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     *  Saves a shortcut to an UUID.  The contents of the file are the path of the Node.
     *  
     */
    private void saveUuidShortcut(WorkspaceImpl ws, Path path, ValueContainer vc) throws RepositoryException
    {
        if( path.getLastComponent().equals(Q_JCR_UUID) )
        {
            UUIDObjectStore<Path> uuids = m_uuids.get( ws.getName() );
            uuids.setObject( vc.getValue().valueAsString(), path.getParentPath() );
        }
    }
    
    private ValueImpl prepareValue( WorkspaceImpl ws, File propFile, String propType )
        throws IOException, RepositoryException
    {
        ValueFactoryImpl vf = ws.getSession().getValueFactory();
        ValueImpl value;
        
        if( propType.equals(PropertyType.TYPENAME_STRING) )
        {
            value = vf.createValue( readContentsAsString(propFile) );
        }
        else if( propType.equals(PropertyType.TYPENAME_BOOLEAN) )
        {
            value = vf.createValue( "true".equals(readContentsAsString(propFile)) );
        }
        else if( propType.equals(PropertyType.TYPENAME_DOUBLE) )
        {
            value = vf.createValue( Double.parseDouble(readContentsAsString(propFile)) );
        }
        else if( propType.equals(PropertyType.TYPENAME_LONG) )
        {
            value = vf.createValue( Long.parseLong(readContentsAsString(propFile)) );
        }
        else if( propType.equals(PropertyType.TYPENAME_DATE) )
        {
            value = vf.createValue( readContentsAsString( propFile ), PropertyType.DATE );
        }
        else if( propType.equals(PropertyType.TYPENAME_NAME) )
        {
            String qnameStr = readContentsAsString(propFile);
            QName qn = QName.valueOf( qnameStr );
            value = vf.createValue( qn, PropertyType.NAME );
        }
        else if( propType.equals(PropertyType.TYPENAME_PATH ))
        {
            String val = readContentsAsString(propFile);
            value = vf.createValue( val, PropertyType.PATH );
        }
        else if( propType.equals(PropertyType.TYPENAME_REFERENCE ) )
        {
            value = vf.createValue( readContentsAsString(propFile), PropertyType.REFERENCE );
        }
        else if( propType.equals(PropertyType.TYPENAME_BINARY) )
        {
            value = vf.createValue( new FileBinarySource(propFile) );
        }
        else
            throw new RepositoryException("Cannot deserialize property type "+propType+" for file "+propFile.getAbsolutePath());

        return value;
    }
    
    /**
     *  Returns the contents of the .info -file.
     *  
     *  @param nodeDir
     *  @return
     *  @throws PathNotFoundException 
     *  @throws IOException 
     */
    private Properties getPropertyInfo( File nodeDir, QName name ) throws PathNotFoundException, IOException
    {
        File inf = new File( nodeDir, makeFilename( name, ".info" ) );
        
        if( !inf.exists() )
        {
            throw new PathNotFoundException("The property metadata file was not found: "+inf.getAbsolutePath());
        }
        Properties props;
        FileInputStream in = null;
        
        try
        {
            in = new FileInputStream( inf );
            props = FastPropertyStore.load(in);
        }
        finally
        {
            if( in != null ) in.close();
        }
        
        return props;
    }
    
    public ValueContainer getPropertyValue(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        m_hitCount[Count.GetPropertyValue.ordinal()]++;

        File nodeDir = getNodeDir( ws, path.getParentPath() );
       
        try
        {
            Properties props = getPropertyInfo( nodeDir, path.getLastComponent() );
            
            String propType = props.getProperty(PROP_TYPE);
            Boolean isMultiple = new Boolean(props.getProperty(PROP_MULTIPLE));
            
            if( isMultiple )
            {
                int items = Integer.parseInt( props.getProperty( PROP_NUM_PROPERTIES ) );
                
                ValueImpl[] result = new ValueImpl[items];
                for( int i = 0; i < items; i++ )
                {
                    File df = new File( nodeDir, makeFilename(path.getLastComponent(),"."+i+".data") );
                    ValueImpl v = prepareValue( ws, df, propType );
                    result[i] = v;
                }
                
                return new ValueContainer(result, PropertyType.valueFromName( propType ) );
            }
            
            File df = new File( nodeDir, makeFilename( path.getLastComponent(), ".data" ) );
            ValueImpl v = prepareValue(ws, df, propType);

            return new ValueContainer(v);
        }
        catch( IOException e )
        {            
            throw new RepositoryException("Unable to read property file",e);
        }
    }

    private List<String> getOrder( WorkspaceImpl ws, Path path )
    {
        File order;
        ArrayList<String> result = new ArrayList<String>();
        try
        {
            order = new File(getNodeDir(ws,path), ".order");
            String o = FileUtil.readContents( new FileInputStream(order), "UTF-8" );
            String[] oo = o.split( "\n" );
            
            //
            //  Clean away any empty Strings.
            //
            List<String> res = Arrays.asList( oo );
            for( String s : res )
            {
                if( s.length() > 0 ) result.add( s );
            }
            return result;
        }
        catch( IOException e )
        {
            
        }
        catch( NamespaceException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch( RepositoryException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }
    
    private void saveOrder( WorkspaceImpl ws, Path path, List<String> list)
    {
        FileOutputStream out = null;
        try
        {
            File order = new File(getNodeDir(ws,path),".order");
            
            out = new FileOutputStream(order);
            
            for( String s : list )
            {
                out.write( s.getBytes("UTF-8") );
                out.write( '\n' );
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if( out != null ) 
            {
                try
                {
                    out.close();
                }
                catch( IOException e )
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void remove(final StoreTransaction tx, final Path path) throws RepositoryException
    {
        m_hitCount[Count.Remove.ordinal()]++;

        WorkspaceImpl ws = tx.getWorkspace();
        File nodeFile = getNodeDir( ws, path );

        if( nodeFile != null )
        {
            if( nodeFile.exists() )
            {
                // OK, so this is a Node (i.e. a subdir) and we can just remove everything
                // from underneath it
                if( deleteContents( nodeFile ) )
                {
                    if( !nodeFile.delete() )
                    {
                        log.warning("Unable to delete path "+path);
                    }
                }
                else
                {
                    log.warning("Failed to delete contents of path "+path);
                }
                
                List<String> order = getOrder(ws,path.getParentPath());
               
                order.remove( path.getLastComponent().toString() );
                
                saveOrder(ws, path.getParentPath(), order);
            }
            else
            {
                // Must be a property

                File infoFile = new File( nodeFile.getParentFile(), makeFilename(path.getLastComponent(),".info") );

                DataContent dc = new DataContent( path, nodeFile.getParentFile(), path.getLastComponent() );
                
                try
                {
                    dc.iterate( new DataVisitor(){
                        public void visit(Path p, Properties props, File dataFile) throws IOException
                        {
                            int type = PropertyType.valueFromName( props.getProperty( PROP_TYPE ) );
                        
                            if( type == PropertyType.REFERENCE )
                            {
                                String uuid = readContentsAsString( dataFile );
                                //System.out.println("Removing refs "+uuid);
                                cleanRefMapping( tx.getWorkspace(), p, uuid );
                            }
                        
                            if( p.getLastComponent().equals( Q_JCR_UUID ) )
                            {
                                try
                                {
                                    String uuid = readContentsAsString( dataFile );
                                    
                                    //System.out.println("Removing UUID "+uuid);
                                    cleanUuidMapping( tx.getWorkspace(), uuid );
                                }
                                catch( IOException e )
                                {
                                    // Fail quietly; it might be that the UUID file does not exist for some reason.
                                }  
                            }
                        
                            //System.out.println("Deleting datafile "+dataFile.getAbsolutePath());
                            dataFile.delete();
                        }
                    } );
                    
                    infoFile.delete();
                }
                catch( Exception ex )
                {
                    log.warning( "Oh noes, removal failed! "+ex.getMessage() );
                }
            }
        }

    }

    private interface DataVisitor
    {
        public void visit(Path path, Properties properties, File dataFile) throws IOException;
    }
    
    private class DataContent
    {
        private Path m_path;
        private File m_dir;
        private QName m_basename;
        
        public DataContent(Path path, File dir, QName basename )
        {
            m_path = path;
            m_dir = dir;
            m_basename = basename;
        }
        
        /**
         *  Visits over each property value file in this content.
         *  
         *  @param visitor
         *  @throws PathNotFoundException
         *  @throws IOException
         */
        public void iterate( DataVisitor visitor ) throws PathNotFoundException, IOException
        {
            Properties props = getPropertyInfo( m_dir, m_basename );
            
            // String propType = props.getProperty(PROP_TYPE);
            Boolean isMultiple = new Boolean(props.getProperty(PROP_MULTIPLE));
            
            if( isMultiple )
            {
                int items = Integer.parseInt( props.getProperty( PROP_NUM_PROPERTIES ) );
                
                for( int i = 0; i < items; i++ )
                {
                    File df = new File( m_dir, makeFilename(m_basename,"."+i+".data") );

                    visitor.visit( m_path, props, df );
                }
            }
            else
            {
                File df = new File( m_dir, makeFilename(m_basename, ".data") );
                
                visitor.visit( m_path, props, df );
            }
   
        }
    }
    
    /**
     *  The UUID in question is removed, so we'll clear it now.
     *  
     *  @param uuid
     */
    private void cleanUuidMapping( WorkspaceImpl ws, String uuid )
    {
        m_uuids.get(ws.getName()).setObject( uuid, null );
    }

    private void cleanRefMapping( WorkspaceImpl ws, Path path, String uuid ) throws IOException
    {
        Path[] refs = m_references.get(ws.getName()).getObject( uuid );
            
        //System.out.println("Removing "+path+" ==> "+uuid );
        
        ArrayList<Path> newrefs = new ArrayList<Path>();
        
        if(refs == null) 
        {
            /*
            refs = new Path[1];
            refs[0] = path;
            */
        }
        else
        {
            for( Path p : refs )
            {
                if( !p.equals(path) ) 
                {
                    newrefs.add( p );
                }
            }
        }
        
        m_references.get(ws.getName()).setObject( uuid, 
                                                  newrefs.size() > 0 ? 
                                                      newrefs.toArray( new Path[newrefs.size()] ) :
                                                          null );
    }

    private void saveRefShortcut( WorkspaceImpl ws, Path path, ValueImpl v ) throws ValueFormatException, IllegalStateException, RepositoryException, IOException
    {
        if( v.getType() == PropertyType.REFERENCE )
        {
            String uuid = v.getString();

            Path[] refs = m_references.get(ws.getName()).getObject( uuid );
            if( refs == null ) refs = new Path[0];
            
            Path[] newrefs = new Path[refs.length+1];
            
            for( int i = 0; i < refs.length; i++ ) newrefs[i] = refs[i];
            
            newrefs[newrefs.length-1] = path;

            m_references.get(ws.getName()).setObject( uuid, newrefs );
        }
    }
    


/*
    private Path findUUIDFromPath( WorkspaceImpl ws, String uuid, Path path ) throws RepositoryException
    {
        List<Path> list = listNodes( ws, path );
        
        for( Path p : list )
        {
            //
            //  Check child nodes
            //
            
            Path res = findUUIDFromPath( ws, uuid, p );
            if( res != null ) return res;
            
            //
            //  Check properties of this node
            //
            
            List<String> propList = listProperties( ws, p );
            
            for( String property : propList )
            {
                if( property.equals( NodeImpl.JCR_UUID ) )
                {
                    Value v = (Value)getPropertyValue( ws, p.resolve(property) );
                    
                    if( uuid.equals(v.getString()) ) return p;
                }
            }
        }
        
        return null;
    }
*/    
    
    public Path findByUUID(WorkspaceImpl ws, String uuid) throws RepositoryException
    {
        m_hitCount[Count.FindByUUID.ordinal()]++;

        Path cachedPath = m_uuids.get(ws.getName()).getObject( uuid );
        
        if( cachedPath == null )
        {
            throw new ItemNotFoundException( "There is no item with UUID "+uuid+" in the repository.");
        }
        
        return cachedPath;
    }
    
    private static class PropertyTypeFilter implements FilenameFilter 
    {

        public boolean accept(File dir, String name)
        {
            return name.endsWith(".info");
        }
        
    }

    private List<Path> findReferencesFromPath( WorkspaceImpl ws, String uuid, Path path ) throws RepositoryException
    {
        List<Path> response = new ArrayList<Path>();
        
        List<Path> list = listNodes( ws, path );
        
        for( Path p : list )
        {
            //
            //  Depth-first.
            //
            response.addAll( findReferencesFromPath( ws, uuid, p ) );
            
            //
            //  List the properties
            //
            List<QName> propList = listProperties( ws, p );
            
            for( QName property : propList )
            {
                Path propertyPath = p.resolve(property);
                
                Object o = getPropertyValue(ws, propertyPath );
                
                if( o instanceof Value )
                {
                    Value v = (Value) o;
                    
                    if( v.getType() == PropertyType.REFERENCE &&
                        v.getString().equals( uuid ) )
                    {
                        response.add( propertyPath );
                    }
                }
                else if( o instanceof Value[] )
                {
                    for( Value v : (Value[])o )
                    {             
                        if( v.getType() == PropertyType.REFERENCE &&
                            v.getString().equals( uuid ) )
                        {
                            response.add( propertyPath );
                        }
                    }
                }
            }
        }
        
        return response;
    }

    public List<Path> findReferences(WorkspaceImpl ws, String uuid) throws RepositoryException
    {
        m_hitCount[Count.FindReferences.ordinal()]++;

        Path[] refs = m_references.get(ws.getName()).getObject( uuid );
        
        if( refs != null )
        {
            return Arrays.asList( refs );
        }
        return new ArrayList<Path>();
    }

    public long getCount(Count item)
    {
        return m_hitCount[item.ordinal()];
    }

    public void resetCounts()
    {
        m_hitCount = new long[Count.values().length];
    }
    

    private static final String[] WINDOWS_DEVICE_NAMES =
    {
        "con", "prn", "nul", "aux", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9",
        "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9"
    };

    private static final String APPROVED_PUNCTUATION = " ./_,-+[]";

    /**
     *  This makes sure that the name
     *  is still readable by the file system.  The basic algorithm is
     *  as follows: 
     *  <ul>
     *   <li>All lower-case ASCII characters and digits are passed as-is.
     *   <li>All upper-case ASCII characters are postfixed with '^'. This is
     *       to make sure it all works on a case-insensitive file system such
     *       as Windows or OSX.
     *   <li>Approved punctuation characters are passed as-is.
     *   <li>All other-characters are transformed into their unicode presentation formats
     *       preceded with a '='.  E.g. =00e5.
     *  </ul>
     *  
     *  @param pagename The name to mangle
     *  @return The mangled name.
     */
    
    // TODO: Protect against windows special names (CON) etc.
    protected String mangleName( String name )
    {
        //
        //  Check the internal cache first.
        //
        String cached = m_mangledNames.get(name);
        
        if( cached != null ) 
        {
            m_cacheHits++;
            return cached;
        }
        
        m_cacheMisses++;
        StringBuilder sb = new StringBuilder(name.length()+32);
        
        int len = name.length();
        for( int i = 0; i < len; i++ )
        {
            char ch = name.charAt( i );
            
            if( (ch >= 'a' && ch <= 'z') || 
                (APPROVED_PUNCTUATION.indexOf(ch) != -1) ||
                (ch >= '0' && ch <= '9') )
            {
                sb.append( ch );
            }
            else if( ch == ':' )
            {
                // Colon is not allowed on NTFS or OSX.
                sb.append( ';' );
            }
            else if( ch >= 'A' && ch <= 'Z' )
            {
                // JSR-170 is case sensitive, so we have to make
                // sure our storage is too.
                
                sb.append( ch );
                sb.append( '^' );
            }
            else
            {
                sb.append( '=' );
                String xa = Integer.toHexString( ch );
                for( int j = 0; j < 4-xa.length(); j++ ) sb.append("0");
                sb.append(xa);
            }
           
        }
        
        String result = sb.toString();
        
        m_mangledNames.put(name, result);
        
        return result;
    }

    // FIXME: FileProvider does not support recovery.  Bummer.
    public void storeFinished( StoreTransaction tx )
    {
    }

    public void storeCancelled( StoreTransaction tx )
    {
    }

    public StoreTransaction storeStarted( WorkspaceImpl ws )
    {
        return new BaseStoreTransaction(ws);
    }


    private String readContentsAsString( File file )
        throws IOException
    {
        FileInputStream fis = new FileInputStream(file);
        try
        {
            return FileUtil.readContents(fis, "UTF-8");
        }
        finally
        {
            if( fis != null ) fis.close();
        }
    }
    


    /*---------------------------------------------------------------------------*/
    
    /**
     *  Stores something based on UUIDs in an internal map, and
     *  always serializes the map on disk upon addition of new objects.
     *  
     *  @param <T> A class to store - MUST implement Serializable.
     */
    // FIXME: Should be faster by storing the map in a separate thread to collect multiple changes.
    // FIXME: Should create a new file each time and not overwrite a previous one; then remove
    //        the old one.
    private class UUIDObjectStore<T extends Serializable>
    {
        private String           m_name;
        private Map<String,T>    m_map;
        private ObjectOutputStream m_journal;
        private int              m_writeCount;
        private long             m_lastWrite = System.currentTimeMillis();
        
        /* After this many writes the store is compacted. */
        private static final int  COMPACT_LIMIT = 100;
        
        /* But compacting is not done more often than this limit (in ms) */
        private static final long COMPACT_TIME_LIMIT = 30*1000L;
        
        public UUIDObjectStore( String name )
        {
            m_name = name;
            
            try
            {
                m_journal = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream( new File(m_root, m_name), true )));
            }
            catch( FileNotFoundException e )
            {
                // FIXME: Should do something sane.
                e.printStackTrace();
            }
            catch( IOException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        public synchronized void shutdown()
        {
            try
            {
                if( m_journal != null )
                {
                    serialize();
                    m_journal.close();
                    m_journal = null;
                }
            }
            catch( IOException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        public synchronized String toString()
        {
            return "UUIDObjectStore["+m_name+", "+(m_map != null ? m_map.size() : "no")+" objs]";
        }
        
        public synchronized T getObject( String uuid )
        {
            if( m_map == null ) unserialize();
            return m_map.get( uuid );
        }
        
        public synchronized void setObject( String uuid, T object )
        {
            if( m_map == null ) unserialize();
            
            try
            {
                if( object != null )
                {
                    m_map.put( uuid, object );
                    writeAddedObject( uuid, object );
//                    System.out.println("   ADD "+uuid+" = "+object);
                }
                else
                {
                    m_map.remove( uuid );

                    writeRemovedObject( uuid );
//                    System.out.println("   REMOVE "+uuid);
                }

                m_journal.flush();
                
                long now = System.currentTimeMillis();
                if( ++m_writeCount > COMPACT_LIMIT && (now-m_lastWrite) > COMPACT_TIME_LIMIT )
                {
                    //System.out.println( now-m_lastWrite );
                    serialize();
                    m_writeCount = 0;
                    m_lastWrite  = now;
                }
            }
            catch( IOException e )
            {
                // FIXME: Should do something sane.
                e.printStackTrace();
            }
        }

        private void writeRemovedObject( String uuid ) throws IOException
        {
            m_journal.writeUTF( uuid );
            m_journal.writeBoolean( false );
        }

        private void writeAddedObject( String uuid, T object ) throws IOException
        {
            m_journal.writeUTF( uuid );
            m_journal.writeBoolean( true );
            m_journal.writeObject( object );
        }
        
        @SuppressWarnings("unchecked")
        private synchronized void unserialize()
        {
            File objFile = new File(m_root, m_name);
            m_map = new HashMap<String,T>();
            
//            System.out.println("------ UNSERIALIZE()");
            
            if( !objFile.exists() ) return;
            
            long start = System.currentTimeMillis();
            
            ObjectInputStream in = null;
            try
            {
                in = new ObjectInputStream( new BufferedInputStream(new FileInputStream(objFile)) );
                
                while( in.available() > 0 )
                {
                    String uuid = in.readUTF();
                    Boolean addRemove = in.readBoolean();
                    
                    if( addRemove )
                    {
                        T o = (T) in.readObject();
//                        System.out.println(" >>> read in "+uuid+" = "+o);
                        m_map.put( uuid, o );
                    }
                    else
                    {
//                        System.out.println(" >>> removing "+uuid);
                        m_map.remove( uuid );
                    }
                }
            }
            catch( EOFException e )
            {
                // S'okay; it's just empty when we open it.
            }
            catch( FileNotFoundException e )
            {
                // Fine, the journal just did not exist.
            }
            catch( IOException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch( ClassNotFoundException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            finally
            {
                if( in != null ) 
                {
                    try
                    {
                        in.close();
                    }
                    catch( IOException e )
                    {
                        e.printStackTrace();
                    }
                }
                
                log.fine( "Unserialized UUID store "+m_name+" in "+(System.currentTimeMillis()-start)+" ms" );
            }
            
        }
        
        private synchronized void serialize() throws IOException
        {
            // System.out.println("++++++ COMPACTING...");

            long start = System.currentTimeMillis();
            
            if( m_map == null ) return;
            
            try
            {
                File fout = new File( m_root, m_name );

                //
                //  Close the old journal and create a new one.
                //
                if( m_journal != null )
                {
                    try
                    {
                        m_journal.close();
                    }
                    catch( Exception e )
                    {
                        // May happen, but we'll continue anyway.
                        log.warning( "Unable to close journal: "+e.getMessage() );
                    }
                    m_journal = null;
                }
                
                m_journal = new ObjectOutputStream( new BufferedOutputStream(new FileOutputStream(fout,false)) );
                
                for( Map.Entry<String, T> i : m_map.entrySet() )
                {
                    writeAddedObject( i.getKey(), i.getValue() );
                }
            }
            finally
            {
                m_journal.flush();
                
                long stop = System.currentTimeMillis();
                
                log.fine("Compacted UUID store "+m_name+" in "+(stop-start)+" ms");
            }
        }
    }

    public void reorderNodes(StoreTransaction tx, Path internalPath, List<Path> childOrder)
    {
        ArrayList<String> newOrder = new ArrayList<String>();
        
        for( Path p : childOrder )
        {
            newOrder.add( p.getLastComponent().toString() );
        }
        saveOrder(tx.getWorkspace(), internalPath, newOrder);
    }

    public void rename(StoreTransaction tx, Path path, Component newName) throws RepositoryException
    {
        throw new UnsupportedRepositoryOperationException();
    }
}
