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
package org.priha.providers;

import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.jcr.*;
import javax.xml.namespace.QName;

import static org.priha.core.JCRConstants.*;
import org.priha.core.PropertyImpl;
import org.priha.core.RepositoryImpl;
import org.priha.core.WorkspaceImpl;
import org.priha.core.binary.FileBinarySource;
import org.priha.core.values.ValueFactoryImpl;
import org.priha.core.values.ValueImpl;
import org.priha.util.Path;
import org.priha.util.PathFactory;
import org.priha.util.TextUtil;

/**
 *  A simple file system -based provider.  This is not particularly optimized,
 *  especially findByUUID() is very slow.
 */
public class FileProvider implements RepositoryProvider, PerformanceReporter
{
    private File m_root;
    
    private Logger log = Logger.getLogger( getClass().getName() );
    
    private long[] m_hitCount;
    
    private Path m_systemPath;
    
    public FileProvider() throws RepositoryException
    {
        resetCounts();
    }

    /**
     * Returns the File which houses all of the workspaces.
     * @return
     */
    private File getWorkspaceRoot()
    {
        return new File( m_root, "workspaces" );
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

    private String makeFilename( QName name, String suffix )
    {
        StringBuffer sb = new StringBuffer(24);
        
        if( name.getPrefix() != null )
        {
            sb.append( name.getPrefix() );
            sb.append( ":" );
        }
        sb.append( name.getLocalPart() );
        if( suffix != null ) sb.append( suffix );
        
        return mangleName(sb.toString());
    }
    
    /**
     *  Returns the directory for a particular Workspace.
     *  
     *  @param wsname
     *  @return
     */
    private File getWorkspaceDir( String wsname )
    {
        File wsDir = new File( getWorkspaceRoot(), wsname );

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
        if( m_systemPath.isParentOf( path ) ) 
        {
            return new File( m_root, getPathFilename(path) );
        }
        
        File wsDir   = getWorkspaceDir( getWorkspaceFilename(ws) );
        File nodeDir = new File( wsDir, getPathFilename(path) );
        
        return nodeDir;
    }
    
    /**
     *  Just copies all characters from <i>in</i> to <i>out</i>.
     */
    private static void copyContents( InputStream in, OutputStream out )
        throws IOException
    {
        byte[] buf = new byte[4096];
        int bytesRead = 0;

        while ((bytesRead = in.read(buf)) > 0) 
        {
            out.write(buf, 0, bytesRead);
        }

        out.flush();
    }
    
    private static void copyContents(FileReader in, StringWriter out)
        throws IOException
    {
        char[] buf = new char[4096];
        int bytesRead = 0;

        while ((bytesRead = in.read(buf)) > 0) 
        {
            out.write(buf, 0, bytesRead);
        }

        out.flush();
    }

    
    public void addNode(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        m_hitCount[Count.AddNode.ordinal()]++;

        File nodeDir = getNodeDir( ws, path );

        nodeDir.mkdirs();
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
                    Properties props = getPropertyInfo( f, QName.valueOf( "{"+NS_JCP+"}primaryType" ) );
                    
                    String qn = props.getProperty( "qname" );
                    Path newPath = startPath.resolve( QName.valueOf( qn ) ); // FIXME: WRONG!

                    list.add( newPath );
                
                    if( recurse )
                    {
                        acquirePaths( newPath, f, list, recurse );
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
                
            }
        }
    }
    

    public List<String> listWorkspaces()
    {
        m_hitCount[Count.ListWorkspaces.ordinal()]++;

        ArrayList<String> list = new ArrayList<String>();
        
        File[] dirs = getWorkspaceRoot().listFiles();
        
        boolean m_hasdefault = false;
        for( File f : dirs )
        {
            if( f.isDirectory() )
            {
                list.add( f.getName() );
                if( f.getName().equals(RepositoryImpl.DEFAULT_WORKSPACE) ) m_hasdefault = true;
            }
        }
        
        if( !m_hasdefault ) list.add( RepositoryImpl.DEFAULT_WORKSPACE );
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

    public void start(RepositoryImpl rep, Properties props)
    {
        m_hitCount[Count.Start.ordinal()]++;
        
        String wsList = props.getProperty("workspaces", "default");
        m_root = new File(props.getProperty("directory"));
        
        log.fine("Initializing FileProvider with root "+m_root);
        
        String[] workspaces = wsList.split("\\s");
        
        for( String wsname : workspaces )
        {
            File wsroot = getWorkspaceDir(wsname);
        
            if( !wsroot.exists() )
            {
                wsroot.mkdirs();
                log.finer("Created workspace directory "+wsroot);
            }
        }
        
    }

    public void close(WorkspaceImpl ws)
    {
        m_hitCount[Count.Close.ordinal()]++;
        // Does nothing, which is fine.
    }

    public void copy(WorkspaceImpl ws, Path srcpath, Path destpath) throws RepositoryException
    {
        m_hitCount[Count.Copy.ordinal()]++;

        throw new UnsupportedRepositoryOperationException("copy()");
    }

    public List<Path> listNodes(WorkspaceImpl ws, Path parentpath)
    {
        m_hitCount[Count.ListNodes.ordinal()]++;

        ArrayList<Path> list = new ArrayList<Path>();

        File wsDir = new File( getWorkspaceRoot(), getWorkspaceFilename(ws) );
        
        acquirePaths( parentpath, new File(wsDir,parentpath.toString()), list, false );
        
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
                    Properties props = new Properties();
                    InputStream in = null;
                    
                    try
                    {
                        in = new FileInputStream(propertyFile);
                
                        props.load(in);
        
                        String qname =  props.getProperty("qname");
                
                        proplist.add( QName.valueOf(qname) );
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
    public boolean nodeExists(WorkspaceImpl ws, Path path)
    {
        m_hitCount[Count.NodeExists.ordinal()]++;

        File nodeDir;
        try
        {
            nodeDir = getNodeDir( ws, path );
            File propFile = new File( nodeDir, "jcr:primaryType.info" );
            
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
        
        m_systemPath = PathFactory.getPath( RepositoryImpl.getGlobalNamespaceRegistry(), 
                                            "/jcr:system" );

        log.fine("Repository has been opened.");
    }

    private void writeValue( File f, ValueImpl v ) throws IOException, IllegalStateException, RepositoryException
    {
        OutputStream out = null;
        
        try
        {
            out = new FileOutputStream( f );
        
            copyContents( v.getStream(), out );
        }
        finally
        {
            if( out != null ) out.close();
        }
    }
    
    public void putPropertyValue(WorkspaceImpl ws, PropertyImpl property) throws RepositoryException
    {
        m_hitCount[Count.PutPropertyValue.ordinal()]++;

        File nodeDir = getNodeDir( ws, property.getInternalPath().getParentPath() );

        saveUuidShortcut(property);
        
        QName qname = property.getQName();
        
        File inf = new File( nodeDir, makeFilename( qname, ".info" ) );
        
        Properties props = new Properties();
        
        props.setProperty( "qname", qname.toString() );
        props.setProperty( "type",  PropertyType.nameFromValue( property.getType() ) );
        props.setProperty( "multiple", property.getDefinition().isMultiple() ? "true" : "false" );

        OutputStream out = null;
        
        try
        {
            if( property.getDefinition().isMultiple() )
            {
                props.setProperty( "numProperties", Integer.toString(property.getValues().length) );
                Value[] values = property.getValues();
                
                for( int i = 0; i < values.length; i++ )
                {
                    File df = new File( nodeDir, makeFilename( qname, "."+i+".data" ) );
                    writeValue( df, (ValueImpl)values[i] );
                }
            }
            else
            {
                File df = new File( nodeDir, makeFilename( qname, ".data" ) );
                writeValue( df, (ValueImpl)property.getValue() );
            }
            out = new FileOutputStream(inf);
            
            props.store( out, null );
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
    private void saveUuidShortcut(PropertyImpl property) throws RepositoryException
    {
        if( property.getQName().equals(NS_JCP+"uuid") )
        {
            File f = new File( getHashPath(property.getString()) );
            
            f.getParentFile().mkdirs(); // Make sure the paths exist.
            BufferedWriter out = null;
            try
            {
                // System.out.println("Writing uuid "+f.getAbsolutePath()+" => "+property.getInternalPath().getParentPath().toString());
                out = new BufferedWriter( new FileWriter(f) );
                
                out.write( property.getInternalPath().getParentPath().toString() );
                
                out.close();
            }
            catch (IOException e)
            {
                log.warning("Cannot create a UUID cache file: "+e.getMessage());
                throw new RepositoryException("Cannot create UUID cache file",e);
            }
            finally
            {
                if( out == null ) try { out.close(); } catch( Exception e ) {}
            }
        }
    }

    private String readContentsAsString( File file )
        throws IOException
    {
        FileReader in = new FileReader( file );
        
        try
        {
            StringWriter out = new StringWriter();
            copyContents( in, out );  
        
            return out.toString();
        }
        finally
        {
            in.close();
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
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis( Long.parseLong(readContentsAsString(propFile)) );
            value = vf.createValue( c );
        }
        else if( propType.equals(PropertyType.TYPENAME_NAME) )
        {
            String qnameStr = readContentsAsString(propFile);
            //QName qn = QName.valueOf( qnameStr );
            value = vf.createValue( qnameStr, PropertyType.NAME );
        }
        else if( propType.equals(PropertyType.TYPENAME_PATH ))
        {
            String val = readContentsAsString(propFile);
            //val = ws.fromQName( val );
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
            throw new RepositoryException("Cannot deserialize property type "+propType);

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
        Properties props = new Properties();
        InputStream in = null;
        
        try
        {
            in = new FileInputStream( inf );
            props.load(in);
        }
        finally
        {
            if( in != null ) in.close();
        }
        
        return props;
    }
    
    public Object getPropertyValue(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        m_hitCount[Count.GetPropertyValue.ordinal()]++;

        File nodeDir = getNodeDir( ws, path.getParentPath() );
       
        try
        {
            Properties props = getPropertyInfo( nodeDir, path.getLastComponent() );
            
            String propType = props.getProperty("type");
            Boolean isMultiple = new Boolean(props.getProperty("multiple"));
            
            if( isMultiple )
            {
                int items = Integer.parseInt( props.getProperty( "numProperties" ) );
                
                ValueImpl[] result = new ValueImpl[items];
                for( int i = 0; i < items; i++ )
                {
                    File df = new File( nodeDir, makeFilename(path.getLastComponent(),"."+i+".data") );
                    ValueImpl v = prepareValue( ws, df, propType );
                    result[i] = v;
                }
                
                return result;
            }
            else
            {
                File df = new File( nodeDir, makeFilename( path.getLastComponent(), ".data" ) );
                ValueImpl v = prepareValue(ws, df, propType);

                return v;
            }
        }
        catch( IOException e )
        {            
            throw new RepositoryException("Unable to read property file",e);
        }
    }

    // FIXME: Does not remove UUID maps.
    public void remove(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        m_hitCount[Count.Remove.ordinal()]++;

        File nodeFile = getNodeDir( ws, path );

        log.fine("Deleting path and all subdirectories: "+path);
        
        if( nodeFile != null )
        {
            if( nodeFile.exists() )
            {
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
            }
            else
            {
                // Must be a property
                nodeFile = new File( nodeFile.getParentFile(), makeFilename(path.getLastComponent(),".info") );
                nodeFile.delete();
                nodeFile = new File( nodeFile.getParentFile(), makeFilename(path.getLastComponent(),".data") );
                nodeFile.delete();
            }
        }

    }

    public void stop(RepositoryImpl rep)
    {
        m_hitCount[Count.Stop.ordinal()]++;

        // No need to do anything
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
    /**
     *  To make directory entries last longer, we first distribute the files in
     *  4096 buckets, each one of which gets distributed in 4096 buckets again.
     *  
     *  @param uuid
     *  @return
     */
    private String getHashPath( String uuid )
    {
        String hashpath = m_root + "/uuidmap/" + uuid.substring(0,3) + "/" + uuid.substring(3,6) + "/" + uuid;
        
        return hashpath;
    }
    
    public Path findByUUID(WorkspaceImpl ws, String uuid) throws RepositoryException
    {
        m_hitCount[Count.FindByUUID.ordinal()]++;

        String cachedPath = null;
        try
        {
            cachedPath = readContentsAsString( new File(getHashPath(uuid)) );
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if( cachedPath == null )
        {
            throw new ItemNotFoundException( "There is no item with UUID "+uuid+" in the repository.");
        }
        
        return PathFactory.getPath(ws.getSession(),cachedPath);
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

        return findReferencesFromPath(ws, uuid, Path.ROOT);
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
    
    /**
     *  This makes sure that the queried page name
     *  is still readable by the file system.  For example, all XML entities
     *  and slashes are encoded with the percent notation.
     *  
     *  @param pagename The name to mangle
     *  @return The mangled name.
     */
    protected static String mangleName( String pagename )
    {
        try
        {
            pagename = URLEncoder.encode( pagename, "UTF-8" );
        }
        catch( UnsupportedEncodingException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        pagename = TextUtil.replaceString( pagename, "%2F", "/" );
        pagename = TextUtil.replaceString( pagename, "%3A", ":" );
        
        //
        //  Names which start with a dot must be escaped to prevent problems.
        //  Since we use URL encoding, this is invisible in our unescaping.
        //
        if( pagename.startsWith( "." ) )
        {
            pagename = "%2E" + pagename.substring( 1 );
        }
        
        String pn = pagename.toLowerCase();
        for( int i = 0; i < WINDOWS_DEVICE_NAMES.length; i++ )
        {
            if( WINDOWS_DEVICE_NAMES[i].equals(pn) )
            {
                pagename = "$$$" + pagename;
            }
        }
        
        return pagename;
    }

}