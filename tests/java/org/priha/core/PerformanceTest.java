/*
    Priha - A JSR-170 implementation library.

    Copyright (C) 2007-2009 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

public class PerformanceTest extends TestCase
{
    /** The size of a million can be configured here. ;-) */
    
    private static final int DEFAULT_ITERATIONS = 2000;
    private int m_iterations = DEFAULT_ITERATIONS;
    
    private static final int BLOB_SIZE = 1024*10;
    private int m_blobsize = BLOB_SIZE;
    
    private static final int NODENAMELEN = 16;
    private static final int PROPERTYLEN = 16;
    
    private Credentials m_creds = new SimpleCredentials("username","password".toCharArray());
    private boolean m_memoryProviderTest = true;
    
    private boolean m_jackrabbitTest     = true;
    
    private boolean m_jdbcProviderTest   = true;
    private boolean m_jdbcProviderEhTest = true;

    private boolean m_fileProviderTest   = true;
    private boolean m_fileProviderEhTest = true;
    
    private boolean m_largeTests         = true;
    
    
    public void setUp()
    {
        String iters = System.getProperty("perftest.iterations");
        if( iters != null ) m_iterations = Integer.parseInt(iters);
        
        String blobsize = System.getProperty("perftest.blobsize");
        if( blobsize != null ) m_blobsize = Integer.parseInt(blobsize)*1024;
        
    }
    
    public void testMemoryProvider() throws Exception
    {
        if( !m_memoryProviderTest ) return;
        Perf.setTestable("MemoryProvider, no cache");
        RepositoryImpl rep = RepositoryManager.getRepository("memorynocache.properties");
        
        millionIterationsTest( rep, m_creds , m_iterations );
    }
    
    public void testFileProvider() throws Exception
    {
        if( !m_fileProviderTest ) return;
        Perf.setTestable("FileProvider, no cache");
        RepositoryImpl rep = RepositoryManager.getRepository("filenocache.properties");

        millionIterationsTest( rep, m_creds, m_iterations );
    }

    public void testFileEhcacheProvider() throws Exception
    {
        if( !m_fileProviderEhTest ) return;
        Perf.setTestable("FileProvider, with Ehcache");

        RepositoryImpl rep = RepositoryManager.getRepository("fileehcache.properties");
        
        millionIterationsTest( rep, m_creds, m_iterations );
    }

    public void testJdbcProvider() throws Exception
    {
        if( !m_jdbcProviderTest ) return;
        Perf.setTestable("JdbcProvider, no cache");
        RepositoryImpl rep = RepositoryManager.getRepository("jdbcnocache.properties");
        
        millionIterationsTest( rep, m_creds, m_iterations );
    }

    public void testJdbcEhcacheProvider() throws Exception
    {
        if( !m_jdbcProviderEhTest ) return;
        
        Perf.setTestable("JdbcProvider, with Ehcache");
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
        if( !m_jackrabbitTest ) return;

        Perf.setTestable("Jackrabbit");

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
        Perf.print(this);
    }
    
    public static class TesterClass
    {
        private Repository m_repository;
        private Credentials m_creds;
        private int m_numNodes;
        private int m_readIters;
        private byte[] m_blob;
        
        ArrayList<String> propertyPaths = new ArrayList<String>();
        ArrayList<String> uuids = new ArrayList<String>();
        private boolean m_testNewSession = true;
        private boolean m_testSeqRead    = true;
        private boolean m_testRandRead   = true;
        private boolean m_testUUIDRead   = true;
        private boolean m_testLargeRead  = true;
        private boolean m_testCachedNode = true;
        private boolean m_testUpdate     = true;
        private boolean m_testExists     = true;
        
        public TesterClass(Repository rep, Credentials creds, int numIters, byte[] blob)
        {
            m_repository = rep;
            m_creds = creds;
            m_numNodes = numIters;
            m_readIters = numIters * 10;
            m_blob = blob;
        }
        
        public void testNewSession() throws LoginException, RepositoryException
        {
            if( !m_testNewSession ) return;

            // We want to make sure that we don't get the penalty of the first
            // session here.
            Session prime = m_repository.login(m_creds);
            
            try
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
            finally
            {
                prime.logout();
            }
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
                    Property p = n.setProperty( "test", new ByteArrayInputStream(m_blob) );
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
            if( !m_testSeqRead ) return;

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
            if( !m_testRandRead ) return;

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

        /**
         *  Tests existing/non-existing nodes in a 50:50 ratio.
         * 
         *  @throws Exception
         */
        public void testExists() throws Exception
        {
            if( !m_testExists ) return;
            
            Session s = m_repository.login(m_creds);
            
            try
            {
                Random rand = new Random();
                
                Perf.start("exists");
                
                for( int i = 0; i < m_readIters; i++ )
                {
                    if( i % 2 == 0 )
                    {
                        int item = rand.nextInt(propertyPaths.size());
                        
                        String p = propertyPaths.get( item );
                        assertTrue( p, s.itemExists( p ) );
                    }
                    else
                    {
                        String p = "/nonexisting/path";
                        assertFalse( p, s.itemExists(p) );
                    }
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
            if( !m_testUUIDRead ) return;

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
            if( !m_testLargeRead ) return;
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
                    assertEquals( ii.getName(), m_blob.length, ((Property)ii).getLength());
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
            if( !m_testCachedNode ) return;
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
            if( !m_testUpdate ) return;

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
        TesterClass tc = new TesterClass( rep, creds, numIters, getBlob() );
        
        try
        {
            //
            //  Test how quickly the first session can be opened.
            //
            Perf.start("FirstSess");

            Session s = rep.login(creds);
        
            Perf.stop(1);

            s.logout();
            
            TestUtil.emptyRepo(rep);

            tc.testNewSession();
        
            tc.testSave();
        
            tc.testSeqRead();
 
            tc.testRandRead();
        
            tc.testExists();
            
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
        
            if( m_largeTests )
            {
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
    private final byte[] getBlob()
    {
        if( blob == null)
        {
            blob = new byte[m_blobsize]; // 1 Meg
            for( int i = 0; i < m_blobsize; i++ )
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
        private static HashMap<String, HashMap<String, Double>> results = new LinkedHashMap<String,HashMap<String,Double>>();
        
        private static long startTime;
        private static String currTest;
        private static String currTestable;
        
        public static void setTestable(String p)
        {
            currTestable = p;
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
            
            double itersSec = iters/(time/1e9);
            
            HashMap<String,Double> hm;
            
            hm = results.get(currTestable);
            
            if( hm == null )
            {
                hm = new HashMap<String,Double>();
                results.put(currTestable, hm);
            }
            
            hm.put(currTest, itersSec);
            
            //TestUtil.printSpeed(currProvider+": "+currTest, iters, startTime, stop);
        }
        
        public static void print(PerformanceTest test)
        {
            System.out.println("Test results.  The number is operations/seconds - larger means faster.");
            System.out.println("Blob size "+test.m_blobsize/1024+" kB");
            System.out.println("Repository size "+test.m_iterations+" nodes");
            System.out.println("Priha version "+Release.VERSTR);
            
            Repository jr = getJackrabbitRepository();
            if( jr != null )
                System.out.println("Jackrabbit version "+jr.getDescriptor( Repository.REP_VERSION_DESC ));
          
            print();
        }

        public static void print()
        {
            ArrayList<String> keys = new ArrayList<String>();
            
            keys.addAll( results.values().iterator().next().keySet() );
            Collections.sort(keys);

            Map.Entry<String, HashMap<String,Double>>[] array = new Map.Entry[0];
                        
            array = results.entrySet().toArray(array);

            int startIdx = 0;
            int numColumns = 8;
            while( startIdx < keys.size() )
            {
                System.out.printf("%-30s","");
                
                // Column titles
                for( int i = startIdx; i < keys.size() && i < startIdx+numColumns; i++ )
                {
                    System.out.printf("%12s",keys.get( i ));
                }
                System.out.print("\n");
                
                // Values for a single test
                for( int j = 0; j < array.length; j++ )
                {
                    System.out.printf( "%-30s", array[j].getKey() );
                
                    for( int i = startIdx; i < keys.size() && i < startIdx+numColumns; i++ )
                    {
                        String key = keys.get( i );
                        Double val = array[j].getValue().get(key);
                   
                        if( val != null && val < 1 ) System.out.printf("%12.2f",val);
                        else System.out.printf("%12.0f", val != null ? val : Double.NaN );
                    }
                    System.out.print("\n");
                }
                
                startIdx += numColumns;
                
                System.out.print( "\n\n" );
            }
        }
    }
}
