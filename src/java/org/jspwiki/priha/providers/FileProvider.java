package org.jspwiki.priha.providers;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import javax.jcr.*;

import org.jspwiki.priha.core.*;
import org.jspwiki.priha.util.PropertyList;

import com.sun.org.apache.xml.internal.utils.UnImplNode;

public class FileProvider extends RepositoryProvider
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

    private File getWorkspaceRoot()
    {
        return new File( m_root, "workspaces" );
    }
    private String getWorkspaceFilename( Workspace ws )
    {
        return ws.getName();
    }
    
    private String getPathFilename( String path )
    {
        return path;
    }

    private File getNodeDir( Workspace ws, String path )
    {
        File wsDir = new File( getWorkspaceRoot(), getWorkspaceFilename(ws) );
        
        File nodeDir = new File( wsDir, getPathFilename(path) );
        
        return nodeDir;
    }
    
    /**
     *  Just copies all characters from <I>in</I> to <I>out</I>.
     *
     *  @since 1.9.31
     */
    public static void copyContents( InputStream in, OutputStream out )
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
    
    /**
     *  Saves a binary property to a new file, and returns the file name.
     * @param ws
     * @param p
     * @return
     * @throws RepositoryException
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
    
    public void putNode(WorkspaceImpl ws, NodeImpl node) throws RepositoryException
    {
        File nodeDir = getNodeDir( ws, node.getPath() );

        nodeDir.mkdirs();
        
        File propertyfile = new File( nodeDir, ".properties" );
        
        Properties props = new Properties();
        
        for( PropertyIterator i = node.getProperties(); i.hasNext(); )
        {
            Property p = i.nextProperty();
         
            String qname = ((NamespaceRegistryImpl)ws.getNamespaceRegistry()).toQName(p.getName());
            
            props.setProperty( qname+".type",  PropertyType.nameFromValue(p.getType()) );
            props.setProperty( qname+".value", getStringFormat( ws, p ) );
        }
        
        OutputStream out = null;
        try
        {
            out = new FileOutputStream(propertyfile);
            props.store(out, "");
        }
        catch( IOException e )
        {
            throw new RepositoryException( "IO Exception when trying to save node properties ",e);
        }
        finally
        {
            if( out != null ) try { out.close(); } catch( IOException e ) {}
        }
    }

    public PropertyImpl getProperty(WorkspaceImpl ws, String path) throws RepositoryException
    {
        throw new UnsupportedRepositoryOperationException("getProperty()");
    }

    public PropertyList getProperties(WorkspaceImpl ws, String path)
        throws RepositoryException
    {
        File nodeDir = getNodeDir( ws, path );
        PropertyList proplist = new PropertyList();
        
        Properties props = new Properties();
        
        File propertyFile = new File( nodeDir, ".properties" );
        
        if( !propertyFile.exists() )
        {
            return proplist;
        }
        
        InputStream in = null;
        
        try
        {
            in = new FileInputStream(propertyFile);
            
            props.load(in);
        
            for( Map.Entry entry : props.entrySet() )
            {
                String key = (String)entry.getKey();
                if( key.endsWith(".value") ) 
                {
                    String qName = key.substring(0,key.length()-".value".length());
                    String propName = ((NamespaceRegistryImpl)ws.getNamespaceRegistry()).fromQName(qName);
                    String propVal  = (String) entry.getValue();                    
                    String propType = props.getProperty(qName+".type");
         
                    String propertyPath = path + "/" + propName;
                    
                    PropertyImpl pi = ws.createPropertyImpl( propertyPath );
                    
                    if( propType.equals(PropertyType.TYPENAME_STRING) )
                        pi.setValue( (String) propVal );
                    else if( propType.equals(PropertyType.TYPENAME_BOOLEAN) )
                        pi.setValue( Boolean.parseBoolean(propVal) );
                    else if( propType.equals(PropertyType.TYPENAME_DOUBLE) )
                        pi.setValue( Double.parseDouble(propVal) );
                    else if( propType.equals(PropertyType.TYPENAME_LONG) )
                        pi.setValue( Long.parseLong(propVal) );
                    else if( propType.equals(PropertyType.TYPENAME_DATE) )
                    {
                        Calendar c = Calendar.getInstance();
                        c.setTimeInMillis( Long.parseLong(propVal) );
                        pi.setValue( c );
                    }
                    else if( propType.equals(PropertyType.TYPENAME_NAME) ||
                        propType.equals(PropertyType.TYPENAME_PATH ))
                    {
                        propVal = ((NamespaceRegistryImpl)ws.getNamespaceRegistry()).fromQName( propVal );
                        pi.setValue( propVal, PropertyType.valueFromName(propType) );
                    }
                    else if( propType.equals(PropertyType.TYPENAME_REFERENCE ) )
                    {
                        pi.setValue( (String) propVal, PropertyType.REFERENCE );
                    }
                    else if( propType.equals(PropertyType.TYPENAME_BINARY) )
                    {
                        // FIXME: Should not absolutely do this
                        InputStream input = new FileInputStream( new File(nodeDir, propVal) );
                        pi.setValue( input );
                    }
                    else
                        throw new RepositoryException("Cannot deserialize property type "+propType);
        
                    proplist.add( pi );
                }
            }
        }
        catch( IOException e )
        {
            throw new RepositoryException("Thingy said booboo", e);
        }
        finally
        {
            if( in != null ) try { in.close(); } catch(IOException ex) {}            
        }
        
        return proplist;
    }

    public boolean nodeExists(Workspace ws, String path)
    {
        File nodeDir = getNodeDir( ws, path );

        return nodeDir.exists();
    }

    private void acquirePaths( String path, File dir, List<String> list )
    {
        File[] files = dir.listFiles();
        
        if( files == null || files.length == 0 ) return;
        
        for( File f : files )
        {
            String newPath = path + "/" + f.getName();
            if( f.isDirectory() )
            {
                list.add( newPath );
                acquirePaths( newPath, f, list );
            }
        }
    }
    
    public List<String> listNodePaths(Workspace ws)
    {
        ArrayList<String> list = new ArrayList<String>();

        File wsDir = new File( getWorkspaceRoot(), getWorkspaceFilename(ws) );

        list.add("/");
        
        acquirePaths("",wsDir,list);
        
        return list;
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

    public void open(Repository rep, Credentials credentials, String workspaceName) 
        throws NoSuchWorkspaceException
    {
        List<String> wsnames = listWorkspaces();
        
        if( wsnames.indexOf(workspaceName) == -1 )
            throw new NoSuchWorkspaceException(workspaceName);
        
        log.fine("Repository has been opened.");
    }

    @Override
    public void remove( WorkspaceImpl ws, String path )
    {
        File nodeDir = getNodeDir( ws, path );

        log.fine("Deleting path and all subdirectories: "+path);
        
        if( nodeDir != null && nodeDir.exists() )
        {
            deleteContents( nodeDir );
        
            nodeDir.delete();
        }
    }

    private void deleteContents( File dir )
    {
        for( File f : dir.listFiles() )
        {
            if( f.isDirectory() )
            {
                deleteContents( f );
            }
            
            f.delete();
        }
    }
}
