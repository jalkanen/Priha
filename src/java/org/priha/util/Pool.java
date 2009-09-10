package org.priha.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *  Simple object pool.
 */
public class Pool
{
    private BlockingQueue<Poolable> m_objects = new LinkedBlockingQueue<Poolable>();
    private PoolableFactory m_factory;
    private int m_size;
    
    public Pool( PoolableFactory factory )
    {
        m_factory = factory;
    }
    
    public int size()
    {
        return m_size;
    }

    public Poolable get() throws InterruptedException, PoolExhaustedException
    {
        if( m_objects.isEmpty() )
        {
            Poolable p = null;
            try
            {
                p = m_factory.newPoolable( this );
            }
            catch( Exception e )
            {
                throw new PoolExhaustedException("No more objects available in the pool");
            }
            if( p != null ) 
            {
                m_objects.offer( p );
                m_size++;
            }
            else throw new PoolExhaustedException("No more objects from the pool!");            
        }
        
        Poolable obj = m_objects.take();
        
        return obj;
    }
    
    private void release(Poolable o)
    {
        m_objects.offer( o );
    }
   
    public interface PoolableFactory
    {
        public Poolable newPoolable( Pool p ) throws Exception;
    }
    
    public static abstract class Poolable
    {
        protected Pool m_pool;
        
        public Poolable(Pool p)
        {
            m_pool = p;
        }
        
        public void release()
        {
            m_pool.release(this);
        }
        
        /**
         *  Default implementation does nothing. You should override this if your
         *  object needs special cleanup.
         */
        public void dispose()
        {}
    }
    
    public static class PoolExhaustedException extends Exception
    {
        public PoolExhaustedException(String msg)
        {
            super(msg);
        }
    }

    /**
     *  Disposes all returned objects from the pool.
     */
    public void dispose()
    {
        Poolable p;
        while( (p = m_objects.poll()) != null)
        {
            p.dispose();
        }
    }
}
