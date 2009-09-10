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

import org.priha.core.PropertyImpl;
import org.priha.core.RepositoryImpl;
import org.priha.core.WorkspaceImpl;
import org.priha.core.binary.MemoryBinarySource;
import org.priha.core.values.ValueImpl;
import org.priha.util.*;
import org.priha.util.Pool.Poolable;
import org.priha.util.Pool.PoolableFactory;

/**
 *  A basic implementation of a Provider which stores the contents to a database.
 *  <p>
 *  This particular implementation is designed for HSQLDB, and not yet tested
 *  with any other databases.
 *
 */
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
    
    private Logger log = Logger.getLogger(JdbcProvider.class.getName());
    
    private DataSource m_dataSource;
    private String     m_connectionURL;
    private String     m_userName;
    private String     m_password;
    private Pool       m_connections = new Pool(this);
    private int        m_maxConnections;
    
    private Connection getConnection() throws SQLException
    {
        PoolableConnection pc;
        try
        {
            pc = (PoolableConnection) m_connections.get();
        }
        catch( Exception e )
        {
            throw new SQLException("Connection trouble! "+e.getMessage());
        }
        
        return pc;
    }
    
    // FIXME: Requires two selects and one insert; not very efficient.
    public void addNode(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        Connection c         = null;
        PreparedStatement ps = null;
        
        try
        {
            c = getConnection();
            ps = c.prepareStatement("INSERT INTO nodes (workspace,path,parent) "+
                                    "VALUES (?,?,?);");
            
            //System.out.println("Adding node "+path);
            ps.setInt(1, getWorkspaceId(ws));
            ps.setString(2, path.toString());
            if( !path.isRoot() )
                ps.setInt( 3, getNodeId(ws, path.getParentPath()) );
            else
                ps.setNull( 3, Types.INTEGER );
            
            ps.execute();
        }
        catch (SQLException e)
        {
            throw new RepositoryException("Cannot insert a new node: "+e.getMessage());
        }
        finally
        {
            try
            {
                try
                {
                    if( ps != null ) ps.close();
                }
                finally
                {
                    if( c != null ) c.close();
                }
            }
            catch( SQLException e )
            {
                throw new RepositoryException("Unable to close JDBC connections",e);
            }
        }
    }

    private int getNodeId(WorkspaceImpl ws, Path parentPath) throws SQLException, PathNotFoundException
    {
        Connection c = getConnection();
        
        try
        {
            PreparedStatement ps = c.prepareStatement("SELECT nodes.id AS id FROM workspaces,nodes WHERE nodes.path = ? "+
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
            c.close();
        }
    }

    private int getWorkspaceId(WorkspaceImpl ws) throws SQLException, NoSuchWorkspaceException
    {
        Connection c = getConnection();
        
        try
        {
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
            c.close();
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

        Connection c = null;
        try
        {
            c = getConnection();
        
            initialize(c);
        }
        catch (SQLException e)
        {
            throw new ConfigurationException("Could not start a connection to the database: "+e.getMessage());
        }   
        catch( IOException e )
        {
            throw new ConfigurationException("Could not initialize the database: "+e.getMessage());            
        }
        finally
        {
            if( c != null )
            {
                try
                {
                    c.close();
                }
                catch( SQLException e )
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

    }

    private void initialize(Connection c) throws IOException, SQLException
    {
        String productName = c.getMetaData().getDatabaseProductName();
        
        String setupFile = "/jdbc/"+productName+"/setup.sql";
        
        InputStream in = getClass().getResourceAsStream(setupFile);
        
        if( in == null ) throw new IOException("Setup script not found: "+setupFile);
        
        String sql = FileUtil.readContents(in, "UTF-8");
        
        Statement s = null;
        try
        {
            s= c.createStatement();
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
        Connection c = null;
        try
        {
            c = getConnection();
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
            try
            {
                c.close();
            }
            catch( SQLException e )
            {
                throw new RepositoryException(e);
            }
        }
    }

    public List<Path> findReferences(WorkspaceImpl ws, String uuid) throws RepositoryException
    {
        // FIXME Auto-generated method stub
        return null;
    }

    public Object getPropertyValue(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        Connection c = null;
        try
        {
            c = getConnection();
            PreparedStatement ps = c.prepareStatement("SELECT type,propval,multi FROM propertyvalues WHERE parent = ? AND name = ?");
            
            ps.setLong(1, getNodeId(ws, path.getParentPath()));
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
                    
                    int numObjects = in.readByte();
                    
                    ValueImpl[] v = new ValueImpl[numObjects];
                    
                    for( int i = 0; i < numObjects; i++ )
                    {
                        int length = in.readInt();
                        byte[] ba = new byte[length];
                        
                        in.read(ba);
                        
                        v[i] = ws.getSession().getValueFactory().createValue( new MemoryBinarySource(ba).getStream(), type );
                    }
                    
                    return v;
                }
                
                ValueImpl v = ws.getSession().getValueFactory().createValue( value.getBinaryStream(), type );
         
                return v;
            }
            
            throw new PathNotFoundException("No such property "+path);
        }
        catch( SQLException e )
        {
            throw new PathNotFoundException("No such item "+path);
        }
        catch (IOException e)
        {
            throw new RepositoryException("Deserialization of value failed "+e.getMessage());
        }
        finally
        {
            try
            {
                c.close();
            }
            catch( SQLException e )
            {
                throw new RepositoryException(e);
            }   
        }
    }

    public List<Path> listNodes(WorkspaceImpl ws, Path parentpath) throws RepositoryException
    {
        Connection c = null;
        try
        {
            c = getConnection();
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
            try
            {
                c.close();
            }
            catch( SQLException e )
            {
                throw new RepositoryException(e);
            }

        }
    }

    public List<QName> listProperties(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        Connection c = null;
        try
        {
            c = getConnection();
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
            try
            {
                c.close();
            }
            catch( SQLException e )
            {
                throw new RepositoryException(e);
            }

        }
    }

    public Collection<String> listWorkspaces()
    {
        ArrayList<String> workspaces = new ArrayList<String>();
        Connection c = null;
        try
        {
            c = getConnection();
            PreparedStatement ps = getConnection().prepareStatement("select * from workspaces");
            
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
            try
            {
                c.close();
            }
            catch( SQLException e )
            {
                // FIXME
            }

        }
        return workspaces;
    }

    public void move(WorkspaceImpl ws, Path srcpath, Path destpath) throws RepositoryException
    {
        // TODO Auto-generated method stub

    }

    public boolean nodeExists(WorkspaceImpl ws, Path path)
    {
        Connection c = null;
        try
        {
            c = getConnection();
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
            try
            {
                c.close();
            }
            catch( SQLException e )
            {
            }

        }
        return false;
    }

    //
    //  Serialization format:
    //  Single : just Value.getStream();
    //  Multi  : <byte numValues> [ <int length> <byte... content> ]
    //
    
    private byte[] serialize( Property property ) throws ValueFormatException, IllegalStateException, RepositoryException, IOException
    {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();

        if( property.getDefinition().isMultiple() )
        {
            Value[] vals = property.getValues();
            
            ObjectOutputStream    oo = new ObjectOutputStream(ba);
            
            oo.writeByte( vals.length );
            
            for( Value v : vals )
            {
                ByteArrayOutputStream vba = new ByteArrayOutputStream();
                FileUtil.copyContents(v.getStream(),vba);
                
                oo.writeInt( vba.size() );
                vba.writeTo( oo );
            }
            
        }
        else
        {
            FileUtil.copyContents( property.getStream(), ba );
        }
        
        return ba.toByteArray();
    }
    
    public void putPropertyValue(WorkspaceImpl ws, PropertyImpl property) throws RepositoryException
    {
        Connection c = null;
        try
        {
            c = getConnection();
            byte[] bytes = serialize(property);
            PreparedStatement ps;
            long id = getNodeId(ws,property.getInternalPath().getParentPath());
                        
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
        finally
        {
            try
            {
                c.close();
            }
            catch( SQLException e )
            {
                throw new RepositoryException(e);
            }

        }
    }

    public void remove(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        PreparedStatement ps;
        Connection c = null;
        
        try
        {
            c = getConnection();
            ps = c.prepareStatement("DELETE FROM propertyvalues WHERE "+
                                    "parent = ? AND "+
                                    "name = ?");

            ps.setLong( 1, getNodeId(ws, path.getParentPath()));
            ps.setString(2, path.getLastComponent().toString() );
            int numRows = ps.executeUpdate();
            
            if( numRows == 0 )
            {
                //
                //  There was no property value removed, so let's remove the parent.
                //
                ps = c.prepareStatement("DELETE from propertyvalues where parent = ?");
                ps.setLong(1, getNodeId(ws,path) );
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
        finally
        {
            if( c != null ) 
            {
                try
                {
                    c.close();
                }
                catch( SQLException x )
                {
                    throw new RepositoryException("Unable to close ",x);
                }
            }
        }
    }

    public void storeFinished( WorkspaceImpl ws )
    {
        // TODO Auto-generated method stub
        
    }

    public void storeStarted( WorkspaceImpl ws )
    {
        // TODO Auto-generated method stub
        
    }

    public Poolable newPoolable( Pool p ) throws SQLException
    {
        if( p.size() >= m_maxConnections ) return null;
        
        return new PoolableConnection(p);
    }

    /**
     *  Wraps around a JDBC Connection.
     */
    private class PoolableConnection extends Pool.Poolable implements Connection
    {
        Connection m_conn;
        
        public PoolableConnection(Pool p) throws SQLException
        {
            super(p);
            
            m_conn = DriverManager.getConnection( m_connectionURL, m_userName, m_password );
            System.out.println(p+" Created new connection");
        }
        
        public void clearWarnings() throws SQLException
        {
            m_conn.clearWarnings();
        }

        public void dispose()
        {
            try
            {
                m_conn.close();
            }
            catch( SQLException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.out.println(m_pool+" Closed connection");
        }
        
        public void close() throws SQLException
        {
            release();
        }

        public void commit() throws SQLException
        {
            m_conn.commit();
        }

        public Statement createStatement() throws SQLException
        {
            return m_conn.createStatement();
        }

        public Statement createStatement( int resultSetType, int resultSetConcurrency ) throws SQLException
        {
            return m_conn.createStatement( resultSetType, resultSetConcurrency );
        }

        public Statement createStatement( int resultSetType, int resultSetConcurrency, int resultSetHoldability )
                                                                                                                 throws SQLException
        {
            return m_conn.createStatement( resultSetType, resultSetConcurrency, resultSetHoldability );
        }

        public boolean getAutoCommit() throws SQLException
        {
            return m_conn.getAutoCommit();
        }

        public String getCatalog() throws SQLException
        {
            return m_conn.getCatalog();
        }

        public int getHoldability() throws SQLException
        {
            return m_conn.getHoldability();
        }

        public DatabaseMetaData getMetaData() throws SQLException
        {
            return m_conn.getMetaData();
        }

        public int getTransactionIsolation() throws SQLException
        {
            return m_conn.getTransactionIsolation();
        }

        public Map<String, Class<?>> getTypeMap() throws SQLException
        {
            return m_conn.getTypeMap();
        }

        public SQLWarning getWarnings() throws SQLException
        {
            return m_conn.getWarnings();
        }

        public boolean isClosed() throws SQLException
        {
            return m_conn.isClosed();
        }

        public boolean isReadOnly() throws SQLException
        {
            return m_conn.isReadOnly();
        }

        public String nativeSQL( String sql ) throws SQLException
        {
            return m_conn.nativeSQL( sql );
        }

        public CallableStatement prepareCall( String sql ) throws SQLException
        {
            return m_conn.prepareCall( sql );
        }

        public CallableStatement prepareCall( String sql, int resultSetType, int resultSetConcurrency ) throws SQLException
        {
            return m_conn.prepareCall( sql, resultSetType, resultSetConcurrency );
        }

        public CallableStatement prepareCall( String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability )
                                                                                                                                 throws SQLException
        {
            return m_conn.prepareCall( sql, resultSetType, resultSetConcurrency, resultSetHoldability );
        }

        public PreparedStatement prepareStatement( String sql ) throws SQLException
        {
            return m_conn.prepareStatement( sql );
        }

        public PreparedStatement prepareStatement( String sql, int autoGeneratedKeys ) throws SQLException
        {
            return m_conn.prepareStatement( sql, autoGeneratedKeys );
        }

        public PreparedStatement prepareStatement( String sql, int[] columnIndexes ) throws SQLException
        {
            return m_conn.prepareStatement( sql, columnIndexes );
        }

        public PreparedStatement prepareStatement( String sql, String[] columnNames ) throws SQLException
        {
            return m_conn.prepareStatement( sql, columnNames );
        }

        public PreparedStatement prepareStatement( String sql, int resultSetType, int resultSetConcurrency ) throws SQLException
        {
            return m_conn.prepareStatement( sql, resultSetType, resultSetConcurrency );
        }

        public PreparedStatement prepareStatement( String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability )
                                                                                                                                      throws SQLException
        {
            return m_conn.prepareStatement( sql, resultSetType, resultSetConcurrency, resultSetHoldability );
        }

        public void releaseSavepoint( Savepoint savepoint ) throws SQLException
        {
            m_conn.releaseSavepoint( savepoint );
        }

        public void rollback() throws SQLException
        {
            m_conn.rollback();
        }

        public void rollback( Savepoint savepoint ) throws SQLException
        {
            m_conn.rollback( savepoint );
        }

        public void setAutoCommit( boolean autoCommit ) throws SQLException
        {
            m_conn.setAutoCommit( autoCommit );
        }

        public void setCatalog( String catalog ) throws SQLException
        {
            m_conn.setCatalog( catalog );
        }

        public void setHoldability( int holdability ) throws SQLException
        {
            m_conn.setHoldability( holdability );
        }

        public void setReadOnly( boolean readOnly ) throws SQLException
        {
            m_conn.setReadOnly( readOnly );
        }

        public Savepoint setSavepoint() throws SQLException
        {
            return m_conn.setSavepoint();
        }

        public Savepoint setSavepoint( String name ) throws SQLException
        {
            return m_conn.setSavepoint( name );
        }

        public void setTransactionIsolation( int level ) throws SQLException
        {
            m_conn.setTransactionIsolation( level );
        }

        public void setTypeMap( Map<String, Class<?>> map ) throws SQLException
        {
            m_conn.setTypeMap( map );
        }
        
    }
}
