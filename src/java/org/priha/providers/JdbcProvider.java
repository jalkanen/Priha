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
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jcr.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.rowset.serial.SerialBlob;

import org.priha.core.PropertyImpl;
import org.priha.core.RepositoryImpl;
import org.priha.core.WorkspaceImpl;
import org.priha.core.binary.MemoryBinarySource;
import org.priha.core.values.ValueImpl;
import org.priha.nodetype.QNodeDefinition;
import org.priha.util.*;
import org.priha.util.Pool.Poolable;
import org.priha.util.Pool.PoolableFactory;

/**
 *  A basic implementation of a Provider which stores the contents to a database.
 */
//FIXME: Does not yet support orderable child nodes
public class JdbcProvider implements RepositoryProvider, PoolableFactory
{
    /** The FQN of the JDBC driver class. */
    public static final String PROP_DRIVERCLASS = "driverClass";
    
    /** The FQN of the DataSource class. */
    public static final String PROP_DATASOURCE = "datasource";
    
    /** The connection URL. */
    public static final String PROP_CONNECTIONURL = "connectionUrl";
    
    /** Username. */
    public static final String PROP_USERNAME = "username";
    
    /** Property for password. */
    public static final String PROP_PASSWORD = "password";
    
    public static final String PROP_MAXCONNECTIONS = "maxConnections";

    public static final String PROP_WORKSPACES = "workspaces";

    private Logger log = Logger.getLogger(JdbcProvider.class.getName());
    
    private DataSource m_dataSource;
    private String     m_connectionURL;
    private String     m_userName;
    private String     m_password;
    private Pool       m_connections = new Pool(this);
    private int        m_maxConnections;
    
    private PoolableConnection getConnection() throws RepositoryException
    {
        PoolableConnection pc;
        try
        {
            pc = (PoolableConnection) m_connections.get(5000);
        }
        catch( Exception e )
        {
            throw new RepositoryException("Connection trouble! "+e.getMessage(),e);
        }
        
        return pc;
    }
    
    // FIXME: Requires two selects and one insert; not very efficient.
    public void addNode(StoreTransaction tx, Path path, QNodeDefinition def) throws RepositoryException
    {
        Connection c = ((JDBCTransaction)tx).getConnection();
        PreparedStatement ps = null;
        WorkspaceImpl ws = tx.getWorkspace();
        
        try
        {
            try
            {
                // Check whether the Node exists. If it does not, then insert
                // a new one.
                long nodeId = getNodeId( c, ws, path );
            }
            catch( PathNotFoundException e )
            {
            
            ps = c.prepareStatement("INSERT INTO nodes (workspace,path,parent) "+
                                    "VALUES (?,?,?);");
            
            //System.out.println("Adding node "+path);
            ps.setInt(1, getWorkspaceId(ws));
            ps.setString(2, path.toString());
            if( !path.isRoot() )
                ps.setInt( 3, getNodeId(c, ws, path.getParentPath()) );
            else
                ps.setNull( 3, Types.INTEGER );
            
            ps.execute();
            }
        }
        catch (SQLException e)
        {
            throw new RepositoryException("Cannot insert a new node: "+path,e);
        }
        finally
        {
            try
            {
                if( ps != null ) ps.close();
            }
            catch( SQLException e )
            {
                throw new RepositoryException("Unable to close JDBC PreparedStatement",e);
            }
        }
    }

    private int getNodeId(Connection c, WorkspaceImpl ws, Path parentPath) throws SQLException, RepositoryException
    {
        PreparedStatement ps = null;
        try
        {
            ps = c.prepareStatement("SELECT nodes.id AS id FROM workspaces,nodes WHERE nodes.path = ? "+
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
        finally
        {
            if(ps != null) ps.close();
        }
    }

    private int getWorkspaceId(WorkspaceImpl ws) throws SQLException, RepositoryException
    {
        PoolableConnection pc = getConnection();
        
        try
        {
            Connection c = pc.getConnection();
            PreparedStatement ps = c.prepareStatement("SELECT * FROM workspaces WHERE name = ?");
        
            ps.setString(1, ws.getName());
        
            ResultSet rs = ps.executeQuery();
        
            if( rs.next() )
            {
                return rs.getInt("id");
            }
        
            throw new NoSuchWorkspaceException("No such workspace "+ws.getName());
        }
        finally
        {
            pc.close();
        }
    }



    public void start(RepositoryImpl repository, Properties properties) throws ConfigurationException
    {
        String driverClass = properties.getProperty( PROP_DRIVERCLASS );
        String dataSource = properties.getProperty( PROP_DATASOURCE );
        m_userName = properties.getProperty( PROP_USERNAME );
        m_password = properties.getProperty( PROP_PASSWORD );
        m_connectionURL = properties.getProperty( PROP_CONNECTIONURL );
        m_maxConnections = Integer.parseInt( properties.getProperty( PROP_MAXCONNECTIONS, "15" ) );
        String[] workspaces = properties.getProperty( PROP_WORKSPACES, "default" ).split( "\\s" ); 
            
        if( dataSource != null )
        {
            try
            {
                Class<DataSource> dsClass = (Class<DataSource>) Class.forName( dataSource );
                m_dataSource = dsClass.newInstance();
            }
            catch( ClassNotFoundException e )
            {
                throw new ConfigurationException("Unable to located DataSource class "+dataSource);
            }
            catch( InstantiationException e )
            {
                throw new ConfigurationException("Unable to instantiate DataSource class "+dataSource);
            }
            catch( IllegalAccessException e )
            {
                throw new ConfigurationException("Unable to access DataSource class "+dataSource);
            }
        }
        else if( driverClass != null )
        {
            try
            {
                Class.forName(driverClass);
            }
            catch (ClassNotFoundException e)
            {
                throw new ConfigurationException("Cannot locate JDBC driver class "+driverClass);
            }    
        }
        else
        {
            InitialContext ic;
            try
            {
                ic = new InitialContext();
                m_dataSource = (DataSource) ic.lookup("java:comp/env/jdbc/prihaDB");
            }
            catch( NamingException e )
            {
                throw new ConfigurationException("Unable to get JDBC from JNDI",e);
            }
            
        }

        if( m_dataSource == null && m_connectionURL == null )
            throw new ConfigurationException("No DataSource nor a connection URL found!");

        PoolableConnection pc = null;
        try
        {
            pc = getConnection(); 
            Connection c = pc.getConnection();
        
            initialize(c);
            setupWorkspaces(c,workspaces);
        }
        catch (SQLException e)
        {
            throw new ConfigurationException("Could not start a connection to the database: "+e.getMessage());
        }   
        catch( IOException e )
        {
            throw new ConfigurationException("Could not initialize the database: "+e.getMessage());            
        }
        catch (RepositoryException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally
        {
            if( pc != null )
            {
                pc.close();
            }
        }

    }

    private void setupWorkspaces(Connection c, String[] workspaces) throws SQLException
    {
        PreparedStatement ps = c.prepareStatement( "SELECT * from workspaces" );
        ResultSet rs = ps.executeQuery();
        ArrayList<String> existingWorkspaces = new ArrayList<String>();
        
        while( rs.next() )
        {
            existingWorkspaces.add( rs.getString( "name" ) );
        }
        
        ArrayList<String> wstoAdd = new ArrayList<String>();
        wstoAdd.addAll( Arrays.asList( workspaces ) );
        
        wstoAdd.removeAll( existingWorkspaces );
        
        ps = c.prepareStatement( "INSERT INTO workspaces ( name ) VALUES ( ? )" );
        
        for( String ws : workspaces )
        {
            log.fine( "Adding new workspace "+ws );
            ps.setString( 1, ws );
            ps.execute();
        }
        
        c.commit();
    }
    
    private void initialize(Connection c) throws IOException, SQLException
    {
        try
        {
            ResultSet rs = c.createStatement().executeQuery("SELECT * FROM nodes");
            if( rs.next() )
                return;
        }
        catch( SQLException e )
        {
            // Either something is really screwed up, or the nodes don't exist.
            // So we skip this and try to set up the database.
        }
        
        log.info( "It appears that there are no entries in the nodes database, so recreating the tables." );
        
        String productName = c.getMetaData().getDatabaseProductName();
        
        String setupFile = "/jdbc/"+productName+"/setup.sql";
        
        InputStream in = getClass().getResourceAsStream(setupFile);
        
        if( in == null ) throw new IOException("Setup script not found: "+setupFile);
        
        String sql = FileUtil.readContents(in, "UTF-8");
        
        Statement s = null;
        try
        {
            s = c.createStatement();
            s.execute(sql);
            c.commit();
        }
        finally
        {
            if( s != null ) s.close();
            in.close();
        }
    }
    
    public void stop(RepositoryImpl rep)
    {
        m_dataSource = null;
        m_connectionURL = null;
        m_connections.dispose();
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
        PreparedStatement ps;
        PoolableConnection pc = getConnection();
        try
        {
            Connection c = pc.getConnection();
            ps = c.prepareStatement( "SELECT path FROM nodes where uuid = ?" );
            ps.setString(1, uuid);
            
            ResultSet rs = ps.executeQuery();
            
            if( rs.next() )
            {
                return PathFactory.getPath( ws.getSession(), rs.getString( "path" ) );
            }
            
            throw new ItemNotFoundException("No UUID by this name found "+uuid);
        }
        catch (SQLException e)
        {
            throw new RepositoryException( e.getMessage() );
        }
        finally
        {
            pc.close();
        }
    }

    //
    //  FIXME: This is really slow because it's using a blob as a primary key...
    //  FIXME: Does not peek into multireferences.
    //
    public List<Path> findReferences(WorkspaceImpl ws, String uuid) throws RepositoryException
    {
        PoolableConnection pc = getConnection();
        ArrayList<Path> result = new ArrayList<Path>();
        
        try
        {
            Connection c = pc.getConnection();
            
            PreparedStatement ps = c.prepareStatement( "SELECT path,name FROM nodes,propertyvalues WHERE propval = ? AND type = 9 AND propertyvalues.parent = nodes.id" );
            ps.setBlob( 1, new SerialBlob(uuid.getBytes("UTF-8")) );
            
            ResultSet rs = ps.executeQuery();
            
            while( rs.next() )
            {
                String name = rs.getString( "name" );
                String parentPath = rs.getString( "path" );
                
                Path p = PathFactory.getPath( ws.getSession(), parentPath+"/"+name );
                
                result.add( p );
            }
        }
        catch( SQLException e )
        {
            throw new RepositoryException("Can't find refs", e);
        }
        catch( UnsupportedEncodingException e )
        {
            throw new RepositoryException("No UTF-8?", e);
        }
        finally
        {
            pc.close();
        }
        
        return result;
    }

    public ValueContainer getPropertyValue(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        PoolableConnection pc = getConnection();
        
        try
        {
            Connection c = pc.getConnection();
            PreparedStatement ps = c.prepareStatement("SELECT type,propval,multi FROM propertyvalues WHERE parent = ? AND name = ?");
            
            ps.setLong(1, getNodeId(c, ws, path.getParentPath()));
            ps.setString(2, path.getLastComponent().toString() );
            
            ResultSet rs = ps.executeQuery();
            
            if( rs.next() )
            {
                int type   = rs.getInt("type");
                Blob value = rs.getBlob("propval");
                boolean multi = rs.getBoolean("multi");
                
                if( multi )
                {
                    ObjectInputStream in = new ObjectInputStream( value.getBinaryStream() );
                    
                    int numObjects = in.readInt();
                    
                    ValueImpl[] v = new ValueImpl[numObjects];
                    
                    for( int i = 0; i < numObjects; i++ )
                    {
                        int length = in.readInt();
                        byte[] ba = new byte[length];
                        
                        in.read(ba);
                        
                        v[i] = ws.getSession().getValueFactory().createValue( new MemoryBinarySource(ba).getStream(), type );
                    }
                    
                    return new ValueContainer(v, type);
                }
                
                ValueImpl v = ws.getSession().getValueFactory().createValue( value.getBinaryStream(), type );
         
                return new ValueContainer(v);
            }
            
            throw new PathNotFoundException("No such property "+path);
        }
        catch( SQLException e )
        {
            throw new PathNotFoundException("No such item "+path);
        }
        catch (IOException e)
        {
            throw new RepositoryException("Deserialization of value failed for "+path,e);
        }
        finally
        {
            pc.close();
        }
    }

    public List<Path> listNodes(WorkspaceImpl ws, Path parentpath) throws RepositoryException
    {
        PoolableConnection pc = getConnection();

        try
        {
            Connection c = pc.getConnection();
            PreparedStatement ps = c.prepareStatement("SELECT N2.path AS path "+
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
                result.add( PathFactory.getPath(ws.getSession(),rs.getString("path")) );
            }
        
            return result;
        }
        catch( SQLException e )
        {
            throw new PathNotFoundException("SQL error "+e.getMessage());
        }
        finally
        {
            pc.close();
        }
    }

    public List<QName> listProperties(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        PoolableConnection pc = getConnection();
        try
        {
            Connection c = pc.getConnection();
            PreparedStatement ps = c.prepareStatement("SELECT propertyvalues.name AS name FROM workspaces,nodes,propertyvalues WHERE "+
                                                           "workspaces.name = ? "+
                                                           "AND workspaces.id = nodes.workspace "+
                                                           "AND nodes.path = ? "+
                                                           "AND nodes.id = propertyvalues.parent "+
                                                           "");
        
            ps.setString(1, ws.getName());
            ps.setString(2, path.toString());
        
            ResultSet rs = ps.executeQuery();
        
            ArrayList<QName> result = new ArrayList<QName>();
            while( rs.next() )
            {
                String qname = rs.getString("name");
                result.add( QName.valueOf(qname) );
            }
        
            return result;
        }
        catch( SQLException e )
        {
            throw new RepositoryException("SQL error "+e.getMessage());
        }
        finally
        {
            pc.close();
        }
    }

    public Collection<String> listWorkspaces() throws RepositoryException
    {
        ArrayList<String> workspaces = new ArrayList<String>();
        PoolableConnection pc = getConnection();
        
        try
        {
            Connection c = pc.getConnection();
            PreparedStatement ps = c.prepareStatement("select * from workspaces");
            
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
        finally
        {
            pc.close();
        }
        return workspaces;
    }

    public void move(WorkspaceImpl ws, Path srcpath, Path destpath) throws RepositoryException
    {
        // TODO Auto-generated method stub

    }

    public boolean nodeExists(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        PoolableConnection pc = getConnection();
        try
        {
            Connection c = pc.getConnection();
            PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) AS rowcount FROM workspaces,nodes WHERE "+
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
        finally
        {
            pc.close();
        }
        return false;
    }

    //
    //  Serialization format:
    //  Single : just Value.getStream();
    //  Multi  : <int numValues> [ <int length> <byte... content> ]
    //
    
    private byte[] serialize( Property property ) throws ValueFormatException, IllegalStateException, RepositoryException, IOException
    {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();

        if( property.getDefinition().isMultiple() )
        {
            Value[] vals = property.getValues();
            
            ObjectOutputStream oo = new ObjectOutputStream(ba);
            
            oo.writeInt( vals.length );
            
            for( Value v : vals )
            {
                ByteArrayOutputStream vba = new ByteArrayOutputStream();
                FileUtil.copyContents(v.getStream(),vba);
                
                oo.writeInt( vba.size() );
                vba.writeTo( oo );
            }
            
            oo.flush();
        }
        else
        {
            FileUtil.copyContents( property.getStream(), ba );
        }
        
        return ba.toByteArray();
    }
    
    public void putPropertyValue(StoreTransaction tx, PropertyImpl property) throws RepositoryException
    {
        Connection c = ((JDBCTransaction)tx).getConnection();
  
        try
        {
            byte[] bytes = serialize(property);
            PreparedStatement ps;
            long id = getNodeId(c,tx.getWorkspace(),property.getInternalPath().getParentPath());
                        
            if( property.isNew() )
            {
                ps = c.prepareStatement("INSERT INTO propertyvalues "+
                                             "(parent,name,type,len,propval,multi) "+
                                             "VALUES (?,?,?,?,?,?)");
            
                ps.setLong(1, id);
                ps.setString(2, property.getQName().toString());
                ps.setInt(3, property.getType());
                ps.setInt(4, bytes.length);
                ps.setBytes(5, bytes);
                ps.setBoolean( 6, property.getDefinition().isMultiple() );
            }            
            else
            {
                ps = c.prepareStatement("UPDATE propertyvalues SET len = ?, propval = ? WHERE parent = ? AND name = ?");
                
                ps.setInt(1, bytes.length);
                ps.setBytes(2, bytes);
                ps.setLong(3, id);
                ps.setString(4, property.getQName().toString());
            }
            
            int result = ps.executeUpdate();
            
            if( result == 0 ) throw new RepositoryException("Update failed!?!");
            
            if( property.getName().equals("jcr:uuid") )
            {
                ps = c.prepareStatement("UPDATE nodes SET uuid = ? WHERE id = ?");
                ps.setString(1, property.getString());
                ps.setLong(2,id);
                
                ps.executeUpdate();
            }
            
        }
        catch( SQLException e )
        {
            throw new PathNotFoundException("SQL error "+e.getMessage());
        }
        catch (IllegalStateException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            throw new RepositoryException("Serialization failed "+e.getMessage());
        }
    }

    public void remove(StoreTransaction tx, Path path) throws RepositoryException
    {
        PreparedStatement ps;
        Connection c = ((JDBCTransaction)tx).getConnection();
        
        try
        {
            ps = c.prepareStatement("DELETE FROM propertyvalues WHERE "+
                                    "parent = ? AND "+
                                    "name = ?");

            ps.setLong( 1, getNodeId(c,tx.getWorkspace(), path.getParentPath()));
            ps.setString(2, path.getLastComponent().toString() );
            int numRows = ps.executeUpdate();
            
            if( numRows == 0 )
            {
                //
                //  There was no property value removed, so let's remove the parent.
                //
                ps = c.prepareStatement("DELETE from propertyvalues where parent = ?");
                ps.setLong(1, getNodeId(c,tx.getWorkspace(),path) );
                ps.executeUpdate();
               
                ps = c.prepareStatement("DELETE FROM nodes WHERE path = ?");
                ps.setString(1, path.toString());
            
                ps.executeUpdate();
            }
        }
        catch( SQLException e )
        {
            log.log(Level.SEVERE, "Creation of SQL query failed", e );
            
            throw new RepositoryException("Did not delete "+path);
        }
    }

    public void storeFinished( StoreTransaction tx ) throws RepositoryException
    {
        JDBCTransaction jtx = (JDBCTransaction)tx;
        
        try
        {
            jtx.getConnection().commit();
        }
        catch( SQLException e )
        {
            throw new RepositoryException("Unable to commit transaction", e);
        }
        finally
        {
            jtx.close();
        }
    }

    public void storeCancelled( StoreTransaction tx ) throws RepositoryException
    {
        JDBCTransaction jtx = (JDBCTransaction)tx;
        
        try
        {
            jtx.getConnection().rollback();
        }
        catch( SQLException e )
        {
            throw new RepositoryException("Unable to commit transaction", e);
        }
        finally
        {
            jtx.close();
        }
    }

    public StoreTransaction storeStarted( WorkspaceImpl ws ) throws RepositoryException
    {
        JDBCTransaction tx = new JDBCTransaction( ws, getConnection() );
        return tx;
    }

    public Poolable newPoolable( Pool p ) throws SQLException
    {
        if( p.size() >= m_maxConnections ) return null;
        
        return new PoolableConnection(p);
    }

    /**
     *  Provides rollback support.
     */
    private class JDBCTransaction extends BaseStoreTransaction
    {
        private PoolableConnection m_conn;
        
        public JDBCTransaction( WorkspaceImpl ws, PoolableConnection p ) throws RepositoryException
        {
            super( ws );
            m_conn = p;
            try
            {
                m_conn.getConnection().setAutoCommit( false );
            }
            catch( SQLException e )
            {
                throw new RepositoryException("Cannot set autocommit=false",e);
            }
        }
        
        public Connection getConnection()
        {
            return m_conn.getConnection();
        }
        
        public void close() throws RepositoryException
        {
            try
            {
                m_conn.getConnection().setAutoCommit( true );
            }
            catch( SQLException e )
            {
                throw new RepositoryException( "Cannot set autocommit=true",e);
            }
            m_conn.close();
        }
    }
    
    /**
     *  Wraps around a JDBC Connection.
     */
    private class PoolableConnection extends Pool.Poolable
    {
        Connection m_conn;
        
        public PoolableConnection(Pool p) throws SQLException
        {
            super(p);
            
            m_conn = DriverManager.getConnection( m_connectionURL, m_userName, m_password );
        }
        
        public Connection getConnection()
        {
            return m_conn;
        }
        
        public void clearWarnings() throws SQLException
        {
            m_conn.clearWarnings();
        }

        public void dispose() throws RepositoryException
        {
            try
            {
                m_conn.close();
            }
            catch( SQLException e )
            {
                throw new RepositoryException(e);
            }
        }
        
        public void close()
        {
            release();
        }
        
    }

    public void reorderNodes(StoreTransaction tx, Path internalPath, List<Path> childOrder) throws RepositoryException
    {
        throw new UnsupportedRepositoryOperationException();
    }
}
