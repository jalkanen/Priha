package org.jspwiki.priha.providers;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jcr.*;

import org.jspwiki.priha.core.PropertyImpl;
import org.jspwiki.priha.core.RepositoryImpl;
import org.jspwiki.priha.core.WorkspaceImpl;
import org.jspwiki.priha.core.values.ValueFactoryImpl;
import org.jspwiki.priha.core.values.ValueImpl;
import org.jspwiki.priha.util.ConfigurationException;
import org.jspwiki.priha.util.FileUtil;
import org.jspwiki.priha.util.Path;
import org.jspwiki.priha.util.PathFactory;

public class JdbcProvider implements RepositoryProvider
{
    /** The FQN of the JDBC driver class. */
    public static final String PROP_DRIVERCLASS = "driverClass";
    
    /** The connection URL. */
    public static final String PROP_CONNECTIONURL = "connectionUrl";
    
    /** Username. */
    public static final String PROP_USERNAME = "username";
    
    /** Property for password. */
    public static final String PROP_PASSWORD = "password";
    
    private Connection m_conn;
    
    private Logger log = Logger.getLogger(JdbcProvider.class.getName());
    
    // FIXME: Requires two selects and one insert; not very efficient.
    public void addNode(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        try
        {
            PreparedStatement ps = m_conn.prepareStatement("INSERT INTO nodes (workspace,path,parent) "+
                                                           "VALUES (?,?,?);");
            
            //System.out.println("Adding node "+path);
            ps.setInt(1, getWorkspaceId(ws));
            ps.setString(2, path.toString());
            if( !path.isRoot() )
                ps.setInt( 3, getNodeId(ws, path.getParentPath()) );
            
            ps.execute();
        }
        catch (SQLException e)
        {
            throw new RepositoryException("Cannot insert a new node: "+e.getMessage());
        }
    }

    private int getNodeId(WorkspaceImpl ws, Path parentPath) throws SQLException, PathNotFoundException
    {
        PreparedStatement ps = m_conn.prepareStatement("SELECT nodes.id AS id FROM workspaces,nodes WHERE nodes.path = ? "+
                                                       "AND nodes.workspace = workspaces.id "+
                                                       "AND workspaces.name = ?");
        
        ps.setString(1, parentPath.toString());
        ps.setString(2, ws.getName());
        
        ResultSet rs = ps.executeQuery();
        
        if( rs.next() )
        {
            return rs.getInt("id");
        }
        
        throw new PathNotFoundException("No such path "+parentPath);    
    }

    private int getWorkspaceId(WorkspaceImpl ws) throws SQLException, NoSuchWorkspaceException
    {
        PreparedStatement ps = m_conn.prepareStatement("SELECT * FROM workspaces WHERE name = ?");
        
        ps.setString(1, ws.getName());
        
        ResultSet rs = ps.executeQuery();
        
        if( rs.next() )
        {
            return rs.getInt("id");
        }
        
        throw new NoSuchWorkspaceException("No such workspace "+ws.getName());    
    }



    public void start(RepositoryImpl repository, Properties properties) throws ConfigurationException
    {
        String driverClass = properties.getProperty( PROP_DRIVERCLASS );
        String connectionURL = properties.getProperty( PROP_CONNECTIONURL );
        String username = properties.getProperty( PROP_USERNAME );
        String password = properties.getProperty( PROP_PASSWORD );
        
        try
        {
            Class.forName(driverClass);
        }
        catch (ClassNotFoundException e)
        {
            throw new ConfigurationException("Cannot locate JDBC driver class "+driverClass);
        }
        
        try
        {
            m_conn = DriverManager.getConnection(connectionURL,
                                                 username,
                                                 password);
            
            initialize();
        }
        catch (SQLException e)
        {
            throw new ConfigurationException("Could not start a connection to the database: "+e.getMessage());
        }   
        catch( IOException e )
        {
            throw new ConfigurationException("Could not initialize the database: "+e.getMessage());            
        }

    }

    private void initialize() throws IOException, SQLException
    {
        InputStream in = getClass().getResourceAsStream("/setup-jdbcprovider.sql");
        
        if( in == null ) throw new IOException("Setup script not found");
        
        String sql = FileUtil.readContents(in, "UTF-8");
        
        Statement s = m_conn.createStatement();
        
        s.execute(sql);
    }
    
    public void stop(RepositoryImpl rep)
    {
        try
        {
            m_conn.close();
            m_conn = null;
        }
        catch(SQLException e)
        {
            log.log( Level.WARNING, "Unable to close database down.", e );
        }
    }


    public void open(RepositoryImpl rep, Credentials credentials, String workspaceName)
        throws RepositoryException,
               NoSuchWorkspaceException
    {
//      TODO Auto-generated method stub

    }


    public void close(WorkspaceImpl ws)
    {
        // TODO Auto-generated method stub

    }

    public void copy(WorkspaceImpl ws, Path srcpath, Path destpath) throws RepositoryException
    {
        // TODO Auto-generated method stub

    }

    public Path findByUUID(WorkspaceImpl ws, String uuid) throws RepositoryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public List<Path> findReferences(WorkspaceImpl ws, String uuid) throws RepositoryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getPropertyValue(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        try
        {
            PreparedStatement ps = m_conn.prepareStatement("SELECT type,propval FROM workspaces,propertyvalues,nodes WHERE "+
                                                           "nodes.path = ? "+
                                                           "AND workspaces.name = ? "+
                                                           "AND nodes.workspace = workspaces.id " +
                                                           "AND workspaces.id = nodes.workspace "+
                                                           "AND nodes.id = propertyvalues.parent "+
                                                           "AND propertyvalues.name = ?");
            
            ps.setString(1, path.getParentPath().toString());
            ps.setString(2, ws.getName());
            ps.setString(3, path.getLastComponent());
            
            ResultSet rs = ps.executeQuery();
            
            if( rs.next() )
            {
                int type   = rs.getInt("type");
                Blob value = rs.getBlob("propval");
            
                ValueImpl v = ValueFactoryImpl.getInstance().createValue( value.getBinaryStream(), type );
            
                return v;
            }
            else
            {
                throw new ItemNotFoundException("No such property "+path);
            }
        }
        catch( SQLException e )
        {
            throw new ItemNotFoundException("No such item "+path);
        }
    }

    public List<Path> listNodes(WorkspaceImpl ws, Path parentpath) throws RepositoryException
    {
        try
        {
            PreparedStatement ps = m_conn.prepareStatement("SELECT N2.path AS path "+
                                                           "FROM workspaces,nodes AS N1 INNER JOIN nodes AS N2 ON "+
                                                           "workspaces.name = ? "+
                                                           "AND workspaces.id = N1.workspace "+
                                                           "AND N1.path = ? "+
                                                           "AND N1.id = N2.parent");
        
            ps.setString(1, ws.getName());
            ps.setString(2, parentpath.toString());
        
            ResultSet rs = ps.executeQuery();
        
            ArrayList<Path> result = new ArrayList<Path>();
            while( rs.next() )
            {
                result.add( PathFactory.getPath(rs.getString("path")) );
            }
        
            return result;
        }
        catch( SQLException e )
        {
            throw new PathNotFoundException("SQL error "+e.getMessage());
        }
    }

    public List<String> listProperties(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        try
        {
            PreparedStatement ps = m_conn.prepareStatement("SELECT propertyvalues.name AS name FROM workspaces,nodes,propertyvalues WHERE "+
                                                           "workspaces.name = ? "+
                                                           "AND workspaces.id = nodes.workspace "+
                                                           "AND nodes.path = ? "+
                                                           "AND nodes.id = propertyvalues.parent "+
                                                           "");
        
            ps.setString(1, ws.getName());
            ps.setString(2, path.toString());
        
            ResultSet rs = ps.executeQuery();
        
            ArrayList<String> result = new ArrayList<String>();
            while( rs.next() )
            {
                String n = rs.getString("name");
                
                result.add( n );
            }
        
            return result;
        }
        catch( SQLException e )
        {
            throw new RepositoryException("SQL error "+e.getMessage());
        }

    }

    public Collection<String> listWorkspaces()
    {
        ArrayList<String> workspaces = new ArrayList<String>();
        try
        {
            PreparedStatement ps = m_conn.prepareStatement("select * from workspaces");
            
            ResultSet rs = ps.executeQuery();
            
            for( ; rs.next(); )
            {
                String wsname = rs.getString("name");
                
                workspaces.add(wsname);
            }
        }
        catch( SQLException e )
        {
            
        }
        
        return workspaces;
    }

    public void move(WorkspaceImpl ws, Path srcpath, Path destpath) throws RepositoryException
    {
        // TODO Auto-generated method stub

    }

    public boolean nodeExists(WorkspaceImpl ws, Path path)
    {
        try
        {
            PreparedStatement ps = m_conn.prepareStatement("SELECT COUNT(*) AS rowcount FROM workspaces,nodes WHERE "+
                                                           "nodes.workspace = workspaces.id " +
                                                           "AND workspaces.name = ? "+
                                                           "AND workspaces.id = nodes.workspace "+
                                                           "AND path = ? ");
        
            ps.setString(1, ws.getName());
            ps.setString(2, path.toString());
        
            ResultSet rs = ps.executeQuery();
        
            rs.next();
            int resultcount = rs.getInt("rowcount");

            //System.out.println("Search for "+path+ "="+(resultcount != 0));

            return resultcount != 0;
        }
        catch( SQLException e )
        {
            log.log(Level.SEVERE, "Creation of SQL query failed", e );
        }
        
        return false;
    }

    public void putPropertyValue(WorkspaceImpl ws, PropertyImpl property) throws RepositoryException
    {
        try
        {
            PreparedStatement ps = m_conn.prepareStatement("INSERT INTO propertyvalues "+
                                                           "(parent,name,type,len,propval) "+
                                                           "VALUES (?,?,?,?,?)");
        
            ps.setLong(1, getNodeId(ws,property.getInternalPath().getParentPath()));
            ps.setString(2, property.getName());
            ps.setInt(3, property.getType());
            ps.setLong(4, property.getLength());
            ps.setBinaryStream(5, property.getStream(), (int) property.getLength());
        
            int result = ps.executeUpdate();

            if( result == 0 ) throw new RepositoryException("Update failed!?!");
        }
        catch( SQLException e )
        {
            throw new PathNotFoundException("SQL error "+e.getMessage());
        }
    }

    public void remove(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        PreparedStatement ps;
        
        try
        {
            ps = m_conn.prepareStatement("DELETE FROM propertyvalues WHERE "+
                                         "parent = ? AND "+
                                         "name = ?");

            ps.setLong( 1, getNodeId(ws, path.getParentPath()));
            ps.setString(2, path.getLastComponent());
            ps.executeUpdate();
            
            ps = m_conn.prepareStatement("DELETE FROM nodes WHERE path = ?");
            ps.setString(1, path.toString());
            
            ps.executeUpdate();
        }
        catch( SQLException e )
        {
            log.log(Level.SEVERE, "Creation of SQL query failed", e );
            
            throw new RepositoryException("Did not delete "+path);
        }
    }

}
