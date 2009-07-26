package org.priha.core;

import java.io.ByteArrayInputStream;
import java.util.*;

import javax.jcr.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.priha.Release;
import org.priha.RepositoryManager;
import org.priha.TestUtil;
import org.priha.core.RepositoryImpl;

public class PerformanceTest extends TestCase
{
    /** The size of a million can be configured here. ;-) */
    
    private static final int DEFAULT_ITERATIONS = 100;
    private int m_iterations = DEFAULT_ITERATIONS;
    
    private static int BLOB_SIZE = 1024*100;
    
    private static int NODENAMELEN = 16;
    private static int PROPERTYLEN = 16;
    
    private Credentials m_creds = new SimpleCredentials("username","password".toCharArray());
    
    public void setUp()
    {
        String iters = System.getProperty("perftest.iterations");
        if( iters != null ) m_iterations = Integer.parseInt(iters);
    }
    
    public void testMemoryProvider() throws Exception
    {
        Perf.setProvider("MemoryProvider, no cache");
        RepositoryImpl rep = RepositoryManager.getRepository("memorynocache.properties");
        
        millionIterationsTest( rep, m_creds , m_iterations );
    }
    
    public void testFileProvider() throws Exception
    {
        Perf.setProvider("FileProvider, no cache");
        RepositoryImpl rep = RepositoryManager.getRepository("filenocache.properties");

        millionIterationsTest( rep, m_creds, m_iterations );
    }

    public void testFileEhcacheProvider() throws Exception
    {
        Perf.setProvider("FileProvider, with Ehcache");

        RepositoryImpl rep = RepositoryManager.getRepository("fileehcache.properties");
        
        millionIterationsTest( rep, m_creds, m_iterations );
    }

    
    public void testJdbcProvider() throws Exception
    {
        Perf.setProvider("JdbcProvider, no cache");
        RepositoryImpl rep = RepositoryManager.getRepository("jdbcnocache.properties");
        
        millionIterationsTest( rep, m_creds, m_iterations );
    }

    public void testJdbcEhcacheProvider() throws Exception
    {
        Perf.setProvider("JdbcProvider, with Ehcache");
        RepositoryImpl rep = RepositoryManager.getRepository("jdbcehcache.properties");
        
        millionIterationsTest( rep, m_creds, m_iterations );
    }
    
    public static Repository getJackrabbitRepository()
    {
        try
        {
            Class<?> cc = Class.forName("org.apache.jackrabbit.core.TransientRepository");
            return (Repository) cc.newInstance();
        }
        catch( Exception e )
        {
            return null;
        }
    }
    
    public void testJackrabbit() throws Exception
    {
        Perf.setProvider("Jackrabbit");

        Repository rep = getJackrabbitRepository();
        
        if( rep != null )
        {
            millionIterationsTest(rep, m_creds, m_iterations);
        }
        else
        {
            System.out.println("Skipping Jackrabbit comparison tests; classes not found in classpath");
        }
    }

    /** Just a dummy test which is executed last and it will print out the
     *  test result.
     */
    public void testPrint()
    {
        Perf.print();
    }
    
    public static class TesterClass
    {
        private Repository m_repository;
        private Credentials m_creds;
        private int m_numNodes;
        private int m_readIters;

        ArrayList<String> propertyPaths = new ArrayList<String>();
        ArrayList<String> uuids = new ArrayList<String>();

        public TesterClass(Repository rep, Credentials creds, int numIters)
        {
            m_repository = rep;
            m_creds = creds;
            m_numNodes = numIters;
            m_readIters = numIters * 100;
        }
        
        public void testNewSession() throws LoginException, RepositoryException
        {
            //
            //  Test how quickly subsequent sessions can be acquired.
            //
            Perf.start("NewSession");
            for( int i = 0; i < m_numNodes; i++ )
            {
                Session s2 = m_repository.login(m_creds);
                s2.logout();
            }
            
            Perf.stop(m_numNodes);
        }
        
        public void testSave() throws LoginException, RepositoryException
        {
            Session s = m_repository.login(m_creds);
            
            try
            {
                Node nd = s.getRootNode();
            
                //
                // Test how quickly we can save a node and a single
                // property to it.
                //
                Perf.start("Save");

                for( int i = 0; i < m_numNodes; i++ )
                {
                    String name = TestUtil.getUniqueID(NODENAMELEN);
                
                    String hash = "x-"+name.charAt(0);
                
                    if( !nd.hasNode(hash) )
                    {
                        nd.addNode(hash);
                    }
                
                    Node n = nd.addNode( hash+"/"+name );
                    n.addMixin("mix:referenceable");
                    Property p = n.setProperty( "test", TestUtil.getUniqueID(PROPERTYLEN) );
                    propertyPaths.add( p.getPath() );
                    
                    nd.save();
                    
                    uuids.add( n.getProperty("jcr:uuid").getString() );
                }
            
                s.save();

                Perf.stop( m_numNodes );
            }
            finally
            {
                s.logout();
            }

        }

        public void testLargeSave() throws LoginException, RepositoryException
        {
            Session s = m_repository.login(m_creds);
            
            try
            {
                Node nd = s.getRootNode();
            
                //
                // Test how quickly we can save a node and a single
                // property to it.
                //
                Perf.start("LargeSave");

                for( int i = 0; i < m_numNodes; i++ )
                {
                    String name = TestUtil.getUniqueID( NODENAMELEN );
                
                    String hash = "x-"+name.charAt(0);
                
                    if( !nd.hasNode(hash) )
                    {
                        nd.addNode(hash);
                    }
                
                    Node n = nd.addNode( hash+"/"+name );
                    Property p = n.setProperty( "test", new ByteArrayInputStream(getBlob()) );
                    propertyPaths.add( p.getPath() );
                    
                    nd.save();
                }
            
                s.save();

                Perf.stop( m_numNodes );
            }
            finally
            {
                s.logout();
            }

        }

        public void testSeqRead() throws LoginException, RepositoryException
        {
            Session s = m_repository.login(m_creds);
            
            try
            {
                //
                //  Test how quickly we can read all the properties
                //  of a single node using getNodes().
                //
                Perf.start("SeqRead");
                
                Node nd = s.getRootNode();
                
                for( NodeIterator i = nd.getNodes(); i.hasNext(); )
                {
                    Node n = i.nextNode();
                    
                    //  Skip nodes which weren't created in this test.
                    if( n.getName().startsWith("x-") )
                    {
                        for( NodeIterator i2 = n.getNodes(); i2.hasNext(); )
                        {
                            Node n2 = i2.nextNode();
                        
                            Property p = n2.getProperty("test");
                            assertEquals( p.getName(), PROPERTYLEN, p.getString().length() );
                        }
                    }
                }
                
                Perf.stop(m_numNodes);
                
            }
            finally
            {
                s.logout();
            }

        }
        
        public void testRandRead() throws LoginException, RepositoryException
        {
            Session s = m_repository.login(m_creds);
            
            try
            {
                Random rand = new Random();
                
                //
                //  Test the speed of random access to properties.  A property
                //  is chosen by random and read.
                //
                Perf.start("RandRead");
                
                for( int i = 0; i < m_readIters; i++ )
                {
                    int item = rand.nextInt( propertyPaths.size() );
                    
                    Item ii = s.getItem( propertyPaths.get(item) );

                    assertFalse( ii.getPath(), ii.isNode() );
                    assertEquals( ii.getName(), PROPERTYLEN, ((Property)ii).getString().length() );
                }
                
                Perf.stop(m_readIters);
            }
            finally
            {
                s.logout();
            }
        }

        public void testUUIDRead() throws LoginException, RepositoryException
        {
            Session s = m_repository.login(m_creds);
            
            try
            {
                Random rand = new Random();
                
                //
                //  Test the speed of random access to properties.  A property
                //  is chosen by random and read.
                //
                Perf.start("UUID");
                
                for( int i = 0; i < m_readIters; i++ )
                {
                    int item = rand.nextInt( uuids.size() );
                    
                    Node ni = s.getNodeByUUID( uuids.get(item) );

                    assertEquals( ni.getName(), PROPERTYLEN, ni.getProperty("test").getString().length() );
                }
                
                Perf.stop(m_readIters);
            }
            finally
            {
                s.logout();
            }
        }
        
        public void testLargeRead() throws LoginException, RepositoryException
        {
            Session s = m_repository.login(m_creds);
            
            try
            {
                Random rand = new Random();
                
                //
                //  Test the speed of random access to properties.  A property
                //  is chosen by random and read.
                //
                Perf.start("LargeRead");
                
                for( int i = 0; i < m_readIters; i++ )
                {
                    int item = rand.nextInt( propertyPaths.size() );
                    
                    Item ii = s.getItem( propertyPaths.get(item) );

                    assertFalse( ii.getPath(), ii.isNode() );
                    assertEquals( ii.getName(), BLOB_SIZE, ((Property)ii).getLength());
                }
                
                Perf.stop(m_readIters);
            }
            finally
            {
                s.logout();
            }
        }

        /**
         *  Produces two figures: getProperty and getItem.  The same item
         *  is fetched through both methods, and the performance is measured.
         */
        public void testCachedNode() throws RepositoryException
        {
            Session s = m_repository.login(m_creds);
            
            try
            {
                String ss = propertyPaths.get( 0 ); 

                Item ii = s.getItem(ss);
                Node nd = ii.getParent();
                String propName = ii.getName();
                
                //
                //  First, access directly with getProperty()
                //
                Perf.start("getProperty");
                
                for( int i = 0; i < m_readIters; i++ )
                {
                    Property prop = nd.getProperty( propName );
                    
                    assertNotNull( prop.getString() );
                }
                
                Perf.stop(m_readIters);
                
                //
                //  Then, with getItem()
                //
                Perf.start("getItem");
                
                for( int i = 0; i < m_readIters; i++ )
                {
                    Property prop = (Property) s.getItem( ss );
                    
                    assertNotNull( prop.getString() );
                }                
                
                Perf.stop(m_readIters);
                
                // Finally, with getByUUID()
                
                String uuid = uuids.get(0);
                Perf.start("propUUID");
                
                for( int i = 0; i < m_readIters; i++ )
                {
                    Property prop = s.getNodeByUUID( uuid ).getProperty( propName );
                    assertNotNull( prop.getString() );
                }
                
                Perf.stop(m_readIters);
            }
            finally
            {
                s.logout();
            }
        }
        
        public void testUpdate() throws RepositoryException
        {
            Session s = m_repository.login(m_creds);
            
            try
            {
                Random rand = new Random();
                
                //
                //  Test the speed of random access to properties.  A property
                //  is chosen by random and updated
                //
                Perf.start("Update");
                
                for( int i = 0; i < m_numNodes; i++ )
                {
                    int item = rand.nextInt( propertyPaths.size() );
                    
                    Property ii = (Property)s.getItem( propertyPaths.get(item) );

                    ii.setValue( "modified result" );
                    
                    s.save();
                }
                
                Perf.stop(m_numNodes);
            }
            finally
            {
                s.logout();
            }
           
        }
    }
    
    public void millionIterationsTest( Repository rep, Credentials creds, int numIters ) throws Exception
    {
        TesterClass tc = new TesterClass( rep, creds, numIters );
        
        try
        {
            //
            //  Test how quickly the first session can be opened.
            //
            Perf.start("FirstSess");

            Session s = rep.login(creds);
        
            Perf.stop(1);

            TestUtil.emptyRepo(rep);

            tc.testNewSession();
        
            tc.testSave();
        
            tc.testSeqRead();
 
            tc.testRandRead();
        
            tc.testUUIDRead();
            
            tc.testUpdate();
            
            tc.testCachedNode();
            
            //
            //  Test how quickly we can then empty the repository.
            //
            Perf.start("Remove");

            TestUtil.emptyRepo(rep);
            tc.propertyPaths.clear();
        
            Perf.stop(numIters);
        
            //
            //  Large objects
            //
            getBlob();
            tc.testLargeSave();
            tc.testLargeRead();
        
            Perf.start("LargeRemove");
        
            TestUtil.emptyRepo(rep);
            tc.propertyPaths.clear();
        
            Perf.stop(numIters);
        }
        catch( Exception t )
        {
            t.printStackTrace();
            throw t;
        }
        finally
        {
            TestUtil.emptyRepo(rep);
        }
    }

    private static byte[] blob;
    private static final byte[] getBlob()
    {
        if( blob == null)
        {
            blob = new byte[BLOB_SIZE]; // 1 Meg
            for( int i = 0; i < BLOB_SIZE; i++ )
                blob[i] = (byte) (i % 255);
        }
        
        return blob;
    }
    
    public static Test suite()
    {
        return new TestSuite( PerformanceTest.class );
    }
    
    /**
     *  This class stores all results to a local hashmap, which can then be pretty-printed.
     *
     */
    public static class Perf
    {
        private static HashMap<String, HashMap<String, Double>> results = new HashMap<String,HashMap<String,Double>>();
        
        private static long startTime;
        private static String currTest;
        private static String currProvider;
        
        public static void setProvider(String p)
        {
            currProvider = p;
        }
        
        public static void start( String test )
        {
            currTest = test;
            startTime = System.nanoTime();
        }
        
        public static void stop( int iters )
        {
            long stop = System.nanoTime();
            
            long time = stop-startTime;
            
            double itersSec = iters/((double)time/1e9);
            
            HashMap<String,Double> hm;
            
            hm = results.get(currProvider);
            
            if( hm == null )
            {
                hm = new HashMap<String,Double>();
                results.put(currProvider, hm);
            }
            
            hm.put(currTest, itersSec);
            
            //TestUtil.printSpeed(currProvider+": "+currTest, iters, startTime, stop);
        }
        
        public static void print()
        {
            System.out.println("Test results.  The number is operations/seconds - larger means faster.");
            System.out.println("Blob size "+BLOB_SIZE/1024+" kB");
            System.out.println("Priha version "+Release.VERSTR);
            
            Repository jr = getJackrabbitRepository();
            if( jr != null )
                System.out.println("Jackrabbit version "+jr.getDescriptor( Repository.REP_VERSION_DESC ));
            
            ArrayList<String> keys = new ArrayList<String>();
            
            keys.addAll( results.values().iterator().next().keySet() );
            Collections.sort(keys);
            
            System.out.printf("%-30s","");
            for( String key : keys )
            {
                System.out.printf("%12s",key);
            }
            System.out.print("\n");
            
            for( Map.Entry<String,HashMap<String,Double>> e : results.entrySet() )
            {
                System.out.printf( "%-30s", e.getKey() );
                
                for( String key : keys )
                {
                    Double val = e.getValue().get(key);
                    System.out.printf("%12.2f", val != null ? val : Double.NaN );
                }
                System.out.print("\n");
            }
        }
    }
}
