package org.priha.providers;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.jcr.*;

import me.prettyprint.cassandra.service.CassandraClient;
import me.prettyprint.cassandra.service.CassandraClientPool;
import me.prettyprint.cassandra.service.CassandraClientPoolFactory;
import me.prettyprint.cassandra.service.Keyspace;

import org.apache.cassandra.thrift.*;
import org.apache.thrift.TException;
import org.priha.core.ItemType;
import org.priha.core.RepositoryImpl;
import org.priha.core.WorkspaceImpl;
import org.priha.nodetype.QNodeDefinition;
import org.priha.path.Path;
import org.priha.path.PathFactory;
import org.priha.util.ConfigurationException;
import org.priha.util.QName;

/**
 *  Provides backing store using Apache Cassandra NoSQL solution.
 *  <p>
 *  Uses the following ColumnFamily configuration:
 *  
 *  ColumnFamily : UUIDMap
 *     name = uuid
 *     comparewith = BytesType
 *     "path" : path
 *     
 *  SuperColumnFamily : Content
 *     name = Workspace
 *     columntype = super
 *     comparewith = UTF8Type
 *     comparesubcolumnswith = UTF8Type
 *     
 *     key : value[] = Path : Properties[]
 *     
 *     special key "_childnodes" lists all names of childnodes in order
 *     
 *  ColumnFamily : Config
 *     name = <Workspaces>
 *     comparewith = UTF8Type
 *     key : value = workspace : name
 */
public class CassandraProvider implements RepositoryProvider
{
    private static final String DEFAULT_KEYSPACE = "Priha";
    private static final String PROP_WORKSPACES = "workspaces";
    private static final String PROP_CONNECTIONS = "connections";
    private Logger log = Logger.getLogger( getClass().getName() );
    
    // These are just temporary and assume a single connection
    private String m_connection;
    private int    m_port;
    
    private CassandraClientPool m_clientPool = CassandraClientPoolFactory.INSTANCE.get();

    private Keyspace getKeyspace() throws RepositoryException
    {
        try
        {
            CassandraClient c = m_clientPool.borrowClient(m_connection,m_port);
            
            return c.getKeyspace(DEFAULT_KEYSPACE);
        }
        catch( Exception e )
        {
            throw new RepositoryException("Unable to get Cassandra client",e);
        }
    }
    
    private void returnKeyspace( Keyspace c )
    {
        try
        {
            if( c != null ) m_clientPool.releaseClient(c.getClient());
        }
        catch( Exception e ){} // FIXME: Shouldn't.
    }
    
    public void addNode(StoreTransaction tx, Path path, QNodeDefinition definition) throws RepositoryException
    {
        Keyspace ks = ((CassandraTransaction)tx).keyspace;
        
//        ColumnPath columnPath = ThriftGlue.createColumnPath("Content", 
//                                                            superColumnName, columnName);
    }

    public void close(WorkspaceImpl ws)
    {
        // TODO Auto-generated method stub

    }

    public Path findByUUID(WorkspaceImpl ws, String uuid) throws ItemNotFoundException, RepositoryException
    {
        Keyspace ks = null;
        
        try
        {
            ks = getKeyspace();
            
            Column c = ks.getColumn("path", ThriftGlue.createColumnPath("UUIDMap", null, uuid.getBytes("UTF-8")) );

            return PathFactory.getPath( new String(c.value,"UTF-8") );
        }
        catch( NotFoundException e )
        {
            throw new ItemNotFoundException("There is no such UUID "+uuid);
        }
        catch (Exception e)
        {
            throw new RepositoryException("Unable to get column ",e);
        }
        finally
        {
            returnKeyspace(ks);
        }
    }
    
    public List<Path> findReferences(WorkspaceImpl ws, String uuid) throws RepositoryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ValueContainer getPropertyValue(WorkspaceImpl ws, Path path) throws PathNotFoundException, RepositoryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean itemExists(WorkspaceImpl ws, Path path, ItemType type) throws RepositoryException
    {
        // TODO Auto-generated method stub
        return false;
    }

    public List<Path> listNodes(WorkspaceImpl ws, Path parentpath) throws RepositoryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public List<QName> listProperties(WorkspaceImpl ws, Path path) throws PathNotFoundException, RepositoryException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<String> listWorkspaces() throws RepositoryException
    {
        Keyspace ks = null;
        
        try
        {
            ks = getKeyspace();
            
            //ks.getRangeSlice("path", ThriftGlue.createColumnParent( "Workspaces", null );

            return null;
        }
        catch (Exception e)
        {
            throw new RepositoryException("Unable to get column ",e);
        }
        finally
        {
            returnKeyspace(ks);
        }
    }

    public void open(RepositoryImpl rep, Credentials credentials, String workspaceName)
                                                                                       throws RepositoryException,
                                                                                           NoSuchWorkspaceException
    {
        // TODO Auto-generated method stub

    }

    public void putPropertyValue(StoreTransaction tx, Path path, ValueContainer property) throws RepositoryException
    {
        // TODO Auto-generated method stub

    }

    public void remove(StoreTransaction tx, Path path) throws RepositoryException
    {
        // TODO Auto-generated method stub

    }

    public void reorderNodes(StoreTransaction tx, Path path, List<Path> childOrder) throws RepositoryException
    {
        // TODO Auto-generated method stub

    }

    public void start(RepositoryImpl repository, Properties properties) throws ConfigurationException
    {
        String[] workspaces = properties.getProperty( PROP_WORKSPACES, "default" ).split( "\\s" );
        String[] connections = properties.getProperty( PROP_CONNECTIONS, "localhost:9160" ).split("\\s");        
        
        // FIXME: Should make this run across multiple connections; now just one
        
        if( connections.length > 1 )
        {
            throw new ConfigurationException("Currently only a single connection is allowed");
        }
        
        String[] host = connections[0].split(":");
        m_connection = host[0];
        if( host.length > 1 ) m_port = Integer.parseInt(host[1]);
        
        log.info("Connecting to Cassandra service on host '"+m_connection+"', port "+m_port);
        
        Keyspace ks = null;
        
        //
        //  Write the workspaces into the config part.
        //
        try
        {
            ks = getKeyspace();
            
            for( String ws : workspaces )
            {
                ks.insert(ws, 
                          ThriftGlue.createColumnPath("Config", null, "Workspaces".getBytes("UTF-8")), 
                          ws.getBytes("UTF-8"));
            }
        }
        catch (Exception e)
        {
            throw new ConfigurationException("Failed to start Cassandra", e);
        }
        finally
        {
            returnKeyspace(ks);
        }
    }

    public void stop(RepositoryImpl rep)
    {
    }

    public void storeCancelled(StoreTransaction tx) throws RepositoryException
    {
        ((CassandraTransaction)tx).close();
    }

    public void storeFinished(StoreTransaction tx) throws RepositoryException
    {
        ((CassandraTransaction)tx).close();
    }

    public StoreTransaction storeStarted(WorkspaceImpl ws) throws RepositoryException
    {
        return new CassandraTransaction(ws);
    }

    private class CassandraTransaction extends BaseStoreTransaction
    {
        public Keyspace keyspace;
        
        public CassandraTransaction(WorkspaceImpl ws) throws RepositoryException
        {
            super(ws);
            keyspace = getKeyspace();
        }
     
        public void close()
        {
            returnKeyspace(keyspace);
        }
    }
}
