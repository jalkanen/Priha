package org.jspwiki.priha.providers;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.jcr.*;
import javax.xml.namespace.QName;

import org.jspwiki.priha.core.*;
import org.jspwiki.priha.core.values.ValueFactoryImpl;
import org.jspwiki.priha.core.values.ValueImpl;
import org.jspwiki.priha.util.Path;
import org.jspwiki.priha.util.PropertyList;

public class FileProvider implements RepositoryProvider
{
    private File m_root;
    
    private Logger log = Logger.getLogger( getClass().getName() );
    
    public FileProvider()
    {
        m_root = new File("/tmp/priha/fileprovider");
    
        log.fine("Initializing FileProvider with root "+m_root);
        File wsroot = getWorkspaceRoot();
        
        if( !wsroot.exists() )
        {
            wsroot.mkdirs();
        }
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

    public void start(RepositoryImpl rep)
    {
        Preferences prefs = rep.getPreferences();
        
        Preferences p = prefs.node("fileprovider");
        
        String wsname = p.get("workspace", "default");
        
        File rootDir = getWorkspaceDir( wsname );
        
        rootDir.mkdirs();
        
        System.out.println("Created workspace directory "+rootDir);
        log.fine("Created workspace directory "+rootDir);
    }

    public void close(WorkspaceImpl ws)
    {
        // Does nothing
    }

    public void copy(WorkspaceImpl ws, Path srcpath, Path destpath) throws RepositoryException
    {
        throw new UnsupportedRepositoryOperationException("copy()");
    }

    public List<Path> listNodes(WorkspaceImpl ws, Path parentpath)
    {
        ArrayList<Path> list = new ArrayList<Path>();

        File wsDir = new File( getWorkspaceRoot(), getWorkspaceFilename(ws) );
        
        acquirePaths( parentpath, new File(wsDir,parentpath.toString()), list, false);
        
        return list;
    }

    public List<String> listProperties(WorkspaceImpl ws, Path path) throws RepositoryException
    {
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
        throw new UnsupportedRepositoryOperationException("move()");
    }

    /**
     *  A Node exists only if it has a primaryType.info in the directory.
     */
    public boolean nodeExists(WorkspaceImpl ws, Path path)
    {
        File nodeDir = getNodeDir( ws, path.toString() );

        File propFile = new File( nodeDir, "jcr:primaryType.info" );
        
        return propFile.exists();
    }

    public void open(RepositoryImpl rep, Credentials credentials, String workspaceName) throws RepositoryException, NoSuchWorkspaceException
    {
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
        File nodeDir = getNodeDir( ws, property.getParent().getPath() );
        
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
        /*
        else if( propType.equals(PropertyType.TYPENAME_BINARY) )
        {
            // FIXME: Should not absolutely do this
            InputStream input = new FileInputStream( new File(nodeDir, propVal) );
            pi.setValue( input );
        }
        */
        else
            throw new RepositoryException("Cannot deserialize property type "+propType);

        return value;
    }
    
    public Object getPropertyValue(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        File nodeDir = getNodeDir( ws, path.getParentPath().toString() );
       
        File inf = new File( nodeDir, path.getLastComponent()+".info" );

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
        // TODO Auto-generated method stub
        
    }

    public Path findByUUID(WorkspaceImpl ws, String uuid) throws RepositoryException
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    private static class PropertyTypeFilter implements FilenameFilter 
    {

        public boolean accept(File dir, String name)
        {
            return name.endsWith(".info");
        }
        
    }
}
