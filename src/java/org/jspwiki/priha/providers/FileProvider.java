package org.jspwiki.priha.providers;

import java.io.*;
import java.util.*;

import javax.jcr.*;

import org.jspwiki.priha.core.NamespaceRegistryImpl;
import org.jspwiki.priha.core.NodeImpl;
import org.jspwiki.priha.core.RepositoryImpl;
import org.jspwiki.priha.core.SessionImpl;

public class FileProvider extends RepositoryProvider
{
    private File m_root;
    
    public FileProvider()
    {
        m_root = new File("/tmp/priha/fileprovider");
        
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
    public void putNode(Workspace ws, NodeImpl node) throws RepositoryException
    {
        File nodeDir = getNodeDir( ws, node.getPath() );

        nodeDir.mkdirs();
        
        File propertyfile = new File( nodeDir, ".properties" );
        
        Properties props = new Properties();
        
        for( PropertyIterator i = node.getProperties(); i.hasNext(); )
        {
            Property p = i.nextProperty();
         
            props.setProperty( p.getName()+".type",  PropertyType.nameFromValue(p.getType()) );
            props.setProperty( p.getName()+".value", getStringFormat( ws, p ) );
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

    public NodeImpl getNode(Workspace ws, String path)
        throws RepositoryException
    {
        File nodeDir = getNodeDir( ws, path );
   
        NodeImpl n = new NodeImpl( (SessionImpl)ws.getSession(), path ); // FIXME: Should set m_new to false
        
        Properties props = new Properties();
        
        File propertyFile = new File( nodeDir, ".properties" );
        
        if( !propertyFile.exists() )
        {
            return n;
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
                    String propName = key.substring(0,key.length()-".value".length());
                    String propVal  = (String) entry.getValue();                    
                    String propType = props.getProperty(propName+".type");
                    
                    if( propType.equals(PropertyType.TYPENAME_STRING) )
                        n.setProperty( propName, propVal );
                    else if( propType.equals(PropertyType.TYPENAME_BOOLEAN) )
                        n.setProperty( propName, Boolean.parseBoolean(propVal) );
                    else if( propType.equals(PropertyType.TYPENAME_DOUBLE) )
                        n.setProperty( propName, Double.parseDouble(propVal) );
                    else if( propType.equals(PropertyType.TYPENAME_LONG) )
                        n.setProperty( propName, Long.parseLong(propVal) );
                    else if( propType.equals(PropertyType.TYPENAME_DATE) )
                    {
                        Calendar c = Calendar.getInstance();
                        c.setTimeInMillis( Long.parseLong(propVal) );
                    }
                    else if( propType.equals(PropertyType.TYPENAME_NAME) ||
                        propType.equals(PropertyType.TYPENAME_PATH ))
                    {
                        propVal = ((NamespaceRegistryImpl)ws.getNamespaceRegistry()).fromQName( propVal );
                        n.setProperty( propName, propVal );
                    }
                    else if( propType.equals(PropertyType.TYPENAME_BINARY) )
                    {
                        InputStream input = new FileInputStream( new File(nodeDir, propVal) );
                        n.setProperty( propName, input );
                    }
                    else
                        throw new RepositoryException("Cannot deserialize property type "+propType);
                }
            }
        }
        catch( IOException e )
        {
            if( in != null ) try { in.close(); } catch(IOException ex) {}
            throw new RepositoryException("Thingy said booboo", e);
        }
        
        return n;
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
    }
    
   
}
