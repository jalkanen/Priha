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
package org.priha.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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

    public Poolable get(int milliseconds) throws InterruptedException, PoolExhaustedException
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
                throw new PoolExhaustedException("New poolable object creation failed horrendously",e);
            }
            if( p != null ) 
            {
                m_objects.offer( p );
                m_size++;
            }
            //else throw new PoolExhaustedException("No more objects from the pool!");            
        }
        
        Poolable obj = m_objects.poll(milliseconds,TimeUnit.MILLISECONDS);
        
        if( obj == null ) throw new PoolExhaustedException("Could not get an object from pool in "+milliseconds+" ms, assuming pool is exhausted.");
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
         * @throws Exception 
         */
        public void dispose() throws Exception
        {}
    }
    
    public static class PoolExhaustedException extends Exception
    {
        
        public PoolExhaustedException(String msg, Throwable reason)
        {
            super(msg,reason);
        }

        public PoolExhaustedException(String string)
        {
            super(string);
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
            try
            {
                p.dispose();
            }
            catch (Exception e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
