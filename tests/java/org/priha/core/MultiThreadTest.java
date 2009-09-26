package org.priha.core;

import java.util.Random;
import java.util.Vector;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import junit.framework.TestCase;

import org.priha.RepositoryManager;
import org.priha.TestUtil;
import org.priha.core.PerformanceTest.Perf;

public class MultiThreadTest extends TestCase
{
    private static final int NUM_THREADS = 10;
    
    Vector<String> propertyPaths = new Vector<String>();
    Vector<String> uuids         = new Vector<String>();

    public void testFileProvider() throws Exception
    {
        //Perf.setProvider("FileProvider, no cache");
        RepositoryImpl rep = RepositoryManager.getRepository("filenocache.properties");
        
        runRepoTest( rep, "File" );
    }

    public void testEhFileProvider() throws Exception
    {
        //Perf.setProvider("FileProvider, no cache");
        RepositoryImpl rep = RepositoryManager.getRepository("fileehcache.properties");
        
        runRepoTest( rep, "FileEh" );
    }

    public void testMemoryProvider() throws Exception
    {
        //Perf.setProvider("FileProvider, no cache");
        RepositoryImpl rep = RepositoryManager.getRepository("memorynocache.properties");
        
        runRepoTest( rep, "Memory" );
    }

    public void testJdbcProvider() throws Exception
    {
        //Perf.setProvider("FileProvider, no cache");
        RepositoryImpl rep = RepositoryManager.getRepository("jdbcnocache.properties");
        
        runRepoTest( rep, "Jdbc" );
    }

    public void testEhJdbcProvider() throws Exception
    {
        //Perf.setProvider("FileProvider, no cache");
        RepositoryImpl rep = RepositoryManager.getRepository("jdbcehcache.properties");
        
        runRepoTest( rep, "JdbcEh" );
    }

    private void runRepoTest( RepositoryImpl rep, String prefix ) throws InterruptedException, LoginException, RepositoryException
    {
        try
        {
            TestThread[] threads = new TestThread[NUM_THREADS];
        
            for( int i = 0; i < NUM_THREADS; i++ )
            {
                threads[i] = new TestThread(rep);
                threads[i].setName( prefix+"-TestThread-"+i );
                threads[i].start();
            }
        
            boolean someonestillalive = true;
            while(someonestillalive)
            {
                someonestillalive = false;
                Thread.sleep( 1000 );
                for( TestThread tt : threads )
                {
                    if( !tt.isAlive() )
                    {
                        if( tt.m_result != null )
                            fail( tt.m_result );
                    }
                    else
                    {
                        someonestillalive = true;
                    }
                }
                
                System.out.print( "." ); System.out.flush();
            }
        }
        finally
        {
            System.out.println("\nAll threads done, now emptying repository...");
            TestUtil.emptyRepo( rep );
        }
    }
        
    public class TestThread extends Thread
    {
        private Repository m_repo;
        private Session m_session;
        public  String  m_result = null;
        private int     m_numItems = 10;

        public TestThread( RepositoryImpl rep )
        {
            m_repo = rep;
        }
        
        private void createRandomNodes() throws RepositoryException
        {
            Node nd = m_session.getRootNode();
            
            //
            // Test how quickly we can save a node and a single
            // property to it.
            //
            Perf.start("Save");

            for( int i = 0; i < m_numItems; i++ )
            {
                createRandomNode(nd);
            }
        
            m_session.save();

        }
        
        private void createRandomNode(Node nd) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException
        {
            String name = TestUtil.getUniqueID(16);
            
            String hash = "x-"+name.charAt(0);
        
        
            try
            {
                if( !nd.hasNode(hash) )
                {
                    nd.addNode(hash);
                }

                Node n = nd.addNode( hash+"/"+name );
                n.addMixin("mix:referenceable");
                Property p = n.setProperty( "test", TestUtil.getUniqueID(16) );
                nd.save();

                propertyPaths.add( p.getPath() );
                
                uuids.add( n.getProperty("jcr:uuid").getString() );
                
            }
            catch( RepositoryException e )
            {
                // This is fine, since there should be some collisions
            }
            
        }
        
        public void readWriteRandomNodes() throws InterruptedException, RepositoryException
        {
            Random rand = new Random();
            
            for( int i = 0; i < 500; i++ )
            {
                Thread.sleep( rand.nextInt(20) );
                
                int item = rand.nextInt( propertyPaths.size() );
                
                try
                {
                    Item ii = m_session.getItem( propertyPaths.get(item) );

                    assertFalse( ii.getPath(), ii.isNode() );
                    assertEquals( ii.getName(), 16, ((Property)ii).getString().length() );
                
                    if( rand.nextDouble() > 0.95 )
                    {
                        createRandomNode( m_session.getRootNode() );
                    }
                }
                catch( PathNotFoundException e ) { } // OK
            }
        }

        public void readRandomNodes() throws InterruptedException, PathNotFoundException, RepositoryException
        {
            Random rand = new Random();
            
            for( int i = 0; i < 5000; i++ )
            {
                //Thread.sleep( rand.nextInt(20) );
                
                int item = rand.nextInt( propertyPaths.size() );
                
                try
                {
                    Item ii = m_session.getItem( propertyPaths.get(item) );

                    assertFalse( ii.getPath(), ii.isNode() );
                    assertEquals( ii.getName(), 16, ((Property)ii).getString().length() );
                }
                catch( PathNotFoundException e ) {} // OK
            }
        }

        public void run()
        {
            Random rnd = new Random();
            
            try
            {
                Thread.sleep( rnd.nextInt( 1000 ) );
                
                m_session = m_repo.login(new SimpleCredentials("xxx",new char[0]));
                
                createRandomNodes();

                Thread.sleep( 500 );

                readWriteRandomNodes();

                //Thread.sleep( rnd.nextInt( 1000 ) );

                readRandomNodes();
            }
            catch( Throwable t )
            {
                t.printStackTrace();
                m_result = t.getMessage();
            }
            finally
            {
                m_session.logout();
            }
            
            System.out.println("TestThread "+getName()+" finished");
        }
        
    }
}
