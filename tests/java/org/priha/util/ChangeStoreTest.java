package org.priha.util;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.priha.AbstractTest;
import org.priha.core.ItemImpl;
import org.priha.core.ItemState;
import org.priha.core.SessionImpl;
import org.priha.core.PerformanceTest.Perf;
import org.priha.path.Path;
import org.priha.util.ChangeStore.Change;

public class ChangeStoreTest extends AbstractTest
{
    ChangeStore m_store = new ChangeStore();
    SessionImpl m_session;
    
    public void setUp() throws Exception
    {
        super.setUp();
        m_session = m_repository.login();
    }
    
    public void tearDown() throws Exception
    {
        m_session.logout();
        super.tearDown();
    }
    
    public void testGet() throws Exception
    {
        Path p = new Path( m_session, "/foo/bar/gobble" );
        ItemImpl ii1 = new DummyItem(m_session,p);
        
        m_store.add( new Change(ItemState.NEW,ii1) );
        m_store.add( new Change(ItemState.REMOVED,ii1) );
        
        assertEquals( ItemState.REMOVED, m_store.getLatestChange( p ).getState() );
    }
    
    private void speedTest(ChangeStore cs) throws Exception
    {
        int numItems = 1000;
        //
        //  Add one thousand items.
        //
        for( int i = 0; i < numItems; i++ )
        {
            Path p = new Path( m_session, "/foo/bar/gobble"+i );
            ItemImpl ii = new DummyItem(m_session,p);
        
            cs.add( new Change(ItemState.NEW,ii) );
        }

        Perf.start( "readhead" );
        int iters = 10000;
        
        Path p = new Path(m_session, "/foo/bar/gobble"+0 );
        for( int i = 0; i < iters; i++ )
        {
            ItemImpl ii = cs.getLatestItem( p );
            assertNotNull(ii);
        }
        
        Perf.stop( iters );
        
        Perf.start( "readmid" );

        p = new Path(m_session, "/foo/bar/gobble"+numItems/2 );
        for( int i = 0; i < iters; i++ )
        {
            ItemImpl ii = cs.getLatestItem( p );
            assertNotNull(ii);
        }
        
        Perf.stop( iters );
        
        Perf.start( "readtail" );

        p = new Path(m_session, "/foo/bar/gobble"+(numItems-1) );
        for( int i = 0; i < iters; i++ )
        {
            ItemImpl ii = cs.getLatestItem( p );
            assertNotNull(ii);
        }
        
        Perf.stop( iters );
        
        Perf.start( "readmiss" );

        p = new Path(m_session, "/foo/bar/gobblexyzzy" );
        for( int i = 0; i < iters; i++ )
        {
            ItemImpl ii = cs.getLatestItem( p );
            assertNull(ii);
        }
        
        Perf.stop( iters );

        Perf.start( "remove" );

        for( int i = 0; i < numItems; i++ )
        {
            Change c = cs.remove();
            assertNotNull(c);
        }
        
        Perf.stop( numItems );

    }
    
    public void testGetSpeed() throws Exception
    {
        Perf.setTestable( "HashMap" );
        speedTest( new ChangeStore() );
        
        Perf.print();
    }
    
    public static Test suite()
    {
        return new TestSuite( ChangeStoreTest.class );
    }
    
    private class DummyItem extends ItemImpl
    {
      
        public DummyItem( SessionImpl s, Path path )
        {
            super( s, path );
        }

        @Override
        public void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException
        {
        }

        @Override
        public void save()
                          throws AccessDeniedException,
                              ItemExistsException,
                              ConstraintViolationException,
                              InvalidItemStateException,
                              ReferentialIntegrityException,
                              VersionException,
                              LockException,
                              NoSuchNodeTypeException,
                              RepositoryException
        {
        }
     
        public boolean isNode()
        {
            return true;
        }
    }
}
