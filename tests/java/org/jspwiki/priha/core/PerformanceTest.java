package org.jspwiki.priha.core;

import java.util.*;

import javax.jcr.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jspwiki.priha.RepositoryManager;
import org.jspwiki.priha.TestUtil;

public class PerformanceTest extends TestCase
{
    /** The size of a million can be configured here. ;-) */
    
    private static final int MILLION_ITERATIONS = 2000;
    private Credentials m_creds = new SimpleCredentials("username","password".toCharArray());
   
    public void testMemoryProvider() throws Exception
    {
        Perf.setProvider("MemoryProvider, no cache");
        RepositoryImpl rep = RepositoryManager.getRepository("memorynocache.properties");
        
        millionIterationsTest( rep, m_creds , MILLION_ITERATIONS );
    }
    
    public void testFileProvider() throws Exception
    {
        Perf.setProvider("FileProvider, no cache");
        RepositoryImpl rep = RepositoryManager.getRepository("filenocache.properties");
        
        millionIterationsTest( rep, m_creds, MILLION_ITERATIONS );
    }

    public void testFileEhcacheProvider() throws Exception
    {
        Perf.setProvider("FileProvider, with Ehcache");

        RepositoryImpl rep = RepositoryManager.getRepository("fileehcache.properties");
        
        millionIterationsTest( rep, m_creds, MILLION_ITERATIONS );
    }

    
    public void testJdbcProvider() throws Exception
    {
        Perf.setProvider("JdbcProvider, no cache");
        RepositoryImpl rep = RepositoryManager.getRepository("jdbcnocache.properties");
        
        millionIterationsTest( rep, m_creds, MILLION_ITERATIONS );
    }

    public void testJdbcEhcacheProvider() throws Exception
    {
        Perf.setProvider("JdbcProvider, with Ehcache");
        RepositoryImpl rep = RepositoryManager.getRepository("jdbcehcache.properties");
        
        millionIterationsTest( rep, m_creds, MILLION_ITERATIONS );
    }
    
    public void testJackrabbit() throws Exception
    {
        Perf.setProvider("Jackrabbit");
        
        try
        {
            Class cc = Class.forName("org.apache.jackrabbit.core.TransientRepository");
            Repository rep = (Repository) cc.newInstance();
        
            millionIterationsTest(rep, m_creds, MILLION_ITERATIONS);
        }
        catch( ClassNotFoundException e )
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
    
    public void millionIterationsTest( Repository rep, Credentials creds, int numIters ) throws Exception
    {
        ArrayList<String> propertyPaths = new ArrayList<String>();

        //
        //  Test how quickly the first session can be opened.
        //
        Perf.start("FirstSess");

        Session s = rep.login(creds);
        
        Perf.stop(1);
        
        //
        //  Test how quickly subsequent sessions can be acquired.
        //
        Perf.start("NewSession");
        for( int i = 0; i < numIters; i++ )
        {
            Session s2 = rep.login(creds);
            s2.logout();
        }
        
        Perf.stop(numIters);
        
        Node nd = s.getRootNode();
        
        //
        //  Test how quickly we can save a node and a single
        //  property to it.
        //
        Perf.start("Save");

        for( int i = 0; i < numIters; i++ )
        {
            String name = "x-"+TestUtil.getUniqueID();
            
            Node n = nd.addNode( name );
            Property p = n.setProperty( "test", TestUtil.getUniqueID() );
            propertyPaths.add( p.getPath() );
        }
        
        s.save();

        Perf.stop( numIters );
        
        //
        //  Test how quickly we can read all the properties
        //  of a single node using getNodes().
        //
        Perf.start("SeqRead");
        
        nd = s.getRootNode();
        
        for( NodeIterator i = nd.getNodes(); i.hasNext(); )
        {
            Node n = i.nextNode();
            
            //  Skip nodes which weren't created in this test.
            if( n.getName().startsWith("x-") )
            {
                Property p = n.getProperty("test");
                assertEquals( p.getName(), 6, p.getString().length() );
            }
        }
        
        Perf.stop(numIters);
        
        Random rand = new Random();
        
        //
        //  Test the speed of random access to properties.  A property
        //  is chosen by random and read.
        //
        Perf.start("RandRead");
        
        for( int i = 0; i < numIters; i++ )
        {
            int item = rand.nextInt( propertyPaths.size() );
            
            Item ii = s.getItem( propertyPaths.get(item) );

            assertFalse( ii.getPath(), ii.isNode() );
            assertEquals( ii.getName(), 6, ((Property)ii).getString().length() );
        }
        
        Perf.stop(numIters);

        //
        //  Test how quickly we can then empty the repository.
        //
        Perf.start("Remove");

        TestUtil.emptyRepo(rep);
        
        Perf.stop(numIters);
    }

    public static Test suite()
    {
        return new TestSuite( PerformanceTest.class );
    }
    
    private static class Perf
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
                    System.out.printf("%12.2f",e.getValue().get(key));
                }
                System.out.print("\n");
            }
        }
    }
}
