package org.jspwiki.priha.providers;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.jcr.*;

import org.jspwiki.priha.core.*;
import org.jspwiki.priha.core.binary.FileBinarySource;
import org.jspwiki.priha.core.values.ValueFactoryImpl;
import org.jspwiki.priha.core.values.ValueImpl;
import org.jspwiki.priha.util.Path;

public class FileProvider implements RepositoryProvider, PerformanceReporter
{
    private File m_root;
    
    private Logger log = Logger.getLogger( getClass().getName() );
    
    private long[] m_hitCount;
    
    public FileProvider()
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
     */
    // FIXME: Should escape the path properly
    private String getPathFilename( String path )
    {
        return path;
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
     */
    private File getNodeDir( Workspace ws, String path )
    {
        if( path.equals("/jcr:system") || path.startsWith("/jcr:system/" ) ) 
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


    
    private static void writeFile( File f, String contents )
        throws IOException
    {
        FileOutputStream out = new FileOutputStream(f);
        
        ByteArrayInputStream in = new ByteArrayInputStream(contents.getBytes("UTF-8"));
        
        try
        {
            copyContents(in, out);
        }
        finally
        {
            out.close();
            in.close();
        } 
    }
    
    /**
     *  Saves a binary property to a new file, and returns the file name.
     *  @param ws
     *  @param p
     *  @return
     *  @throws RepositoryException
     */
    private String saveBinary( Workspace ws, Property p ) throws RepositoryException
    {
        File file = getNodeDir(ws,p.getPath());
        OutputStream out = null;
        
        try
        {
            InputStream in = p.getStream();
        
            out = new FileOutputStream( file );
        
            copyContents( in, out );
            
            return file.getName();
        }
        catch( IOException e )
        {
            throw new RepositoryException("Failed to save binary: "+e.getMessage());
        }
        finally
        {
            if( out != null ) 
            {
                try{ out.close(); } catch( IOException e ) {} // FIXME
            }
        }
    }

    
    private String getStringFormat( Workspace ws, Property p ) throws ValueFormatException, RepositoryException
    {
        switch( p.getType() )
        {
            case PropertyType.DATE:
                return Long.toString(p.getDate().getTimeInMillis());
            case PropertyType.NAME:
            case PropertyType.PATH:
                return ((NamespaceRegistryImpl)ws.getNamespaceRegistry()).toQName(p.getString());
            case PropertyType.BINARY:
                return saveBinary( ws, p );
            default:
                return p.getString();
        }
    }
    
    public void addNode(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        m_hitCount[Count.AddNode.ordinal()]++;

        File nodeDir = getNodeDir( ws, path.toString() );

        nodeDir.mkdirs();
    }

    private void acquirePaths( Path startPath, File dir, List<Path> list, boolean recurse )
    {
        File[] files = dir.listFiles();
        
        if( files == null || files.length == 0 ) return;
        
        for( File f : files )
        {
            Path newPath = startPath.resolve( f.getName() );

            if( f.isDirectory() )
            {
                list.add( newPath );
                
                if( recurse )
                {
                    acquirePaths( newPath, f, list, recurse );
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
        // Does nothing
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
        
        acquirePaths( parentpath, new File(wsDir,parentpath.toString()), list, false);
        
        return list;
    }

    public List<String> listProperties(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        m_hitCount[Count.ListProperties.ordinal()]++;

        File nodeDir = getNodeDir( ws, path.toString() );
        List<String> proplist = new ArrayList<String>();
        
        try
        {
            File[] files = nodeDir.listFiles( new PropertyTypeFilter() );
            
            if( files != null )
            {
                for( File propertyFile : files )
                {
                    Properties props = new Properties();
            
                    InputStream in = new FileInputStream(propertyFile);
                
                    props.load(in);
        
                    String qname =  props.getProperty("qname");
                
                    qname = ((NamespaceRegistryImpl)ws.getNamespaceRegistry()).fromQName(qname);
                    proplist.add( qname );
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

        File nodeDir = getNodeDir( ws, path.toString() );

        File propFile = new File( nodeDir, "jcr:primaryType.info" );
        
        return propFile.exists();
    }

    public void open(RepositoryImpl rep, Credentials credentials, String workspaceName) throws RepositoryException, NoSuchWorkspaceException
    {
        m_hitCount[Count.Open.ordinal()]++;

        List<String> wsnames = listWorkspaces();
        
        if( wsnames.indexOf(workspaceName) == -1 )
            throw new NoSuchWorkspaceException(workspaceName);
        
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

        File nodeDir = getNodeDir( ws, property.getInternalPath().getParentPath().toString() );
        
        String qname = ((NamespaceRegistryImpl)ws.getNamespaceRegistry()).toQName(property.getName());
        
        File inf = new File( nodeDir, property.getName()+".info" );
        
        Properties props = new Properties();
        
        props.setProperty( "qname", qname );
        props.setProperty( "type",  PropertyType.nameFromValue( property.getType() ) );
        props.setProperty( "multiple", property.getDefinition().isMultiple() ? "true" : "false" );

        try
        {
            if( property.getDefinition().isMultiple() )
            {
                props.setProperty( "numProperties", Integer.toString(property.getValues().length) );
                Value[] values = property.getValues();
                
                for( int i = 0; i < values.length; i++ )
                {
                    File df = new File( nodeDir, property.getName()+"."+i+".data" );
                    writeValue( df, (ValueImpl)values[i] );
                }
            }
            else
            {
                File df = new File( nodeDir, property.getName()+".data" );
                writeValue( df, (ValueImpl)property.getValue() );
            }
        
            props.store( new FileOutputStream(inf), null );
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
    }

    private String readContentsAsString( File file )
        throws IOException
    {
        FileReader in = new FileReader( file );
        StringWriter out = new StringWriter();
        copyContents( in, out );  
        
        return out.toString();
    }
    
    private ValueImpl prepareValue( WorkspaceImpl ws, File propFile, String propType )
        throws IOException, RepositoryException
    {
        ValueFactoryImpl vf = ValueFactoryImpl.getInstance();
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
            String val = readContentsAsString(propFile);
            val = ((NamespaceRegistryImpl)ws.getNamespaceRegistry()).fromQName( val );
            value = vf.createValue( val, PropertyType.NAME );
        }
        else if( propType.equals(PropertyType.TYPENAME_PATH ))
        {
            String val = readContentsAsString(propFile);
            val = ((NamespaceRegistryImpl)ws.getNamespaceRegistry()).fromQName( val );
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
    
    public Object getPropertyValue(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        m_hitCount[Count.GetPropertyValue.ordinal()]++;

        File nodeDir = getNodeDir( ws, path.getParentPath().toString() );
       
        File inf = new File( nodeDir, path.getLastComponent()+".info" );

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
            
            String propType = props.getProperty("type");
            Boolean isMultiple = new Boolean(props.getProperty("multiple"));
            
            if( isMultiple )
            {
                int items = Integer.parseInt( props.getProperty( "numProperties" ) );
                
                ValueImpl[] result = new ValueImpl[items];
                for( int i = 0; i < items; i++ )
                {
                    File df = new File( nodeDir, path.getLastComponent()+"."+i+".data");
                    ValueImpl v = prepareValue( ws, df, propType );
                    result[i] = v;
                }
                
                return result;
            }
            else
            {
                File df = new File( nodeDir, path.getLastComponent()+".data" );
                ValueImpl v = prepareValue(ws, df, propType);

                return v;
            }
        }
        catch( IOException e )
        {
            throw new RepositoryException("Unable to read property file",e);
        }
    }

    public void remove(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        m_hitCount[Count.Remove.ordinal()]++;

        File nodeFile = getNodeDir( ws, path.toString() );

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
                nodeFile = new File( nodeFile.getParentFile(), path.getLastComponent()+".info" );
                nodeFile.delete();
                nodeFile = new File( nodeFile.getParentFile(), path.getLastComponent()+".data" );
                nodeFile.delete();
            }
        }

    }

    public void stop(RepositoryImpl rep)
    {
        m_hitCount[Count.Stop.ordinal()]++;

        // No need to do anything
    }

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
    
    public Path findByUUID(WorkspaceImpl ws, String uuid) throws RepositoryException
    {
        m_hitCount[Count.FindByUUID.ordinal()]++;

        Path p = findUUIDFromPath( ws, uuid, Path.ROOT );
        
        if( p == null ) throw new ItemNotFoundException( "There is no item with UUID "+uuid+" in the repository.");
        
        return p;
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
            List<String> propList = listProperties( ws, p );
            
            for( String property : propList )
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
    
}
