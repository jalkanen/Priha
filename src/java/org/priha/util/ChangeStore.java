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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import org.priha.core.ItemImpl;
import org.priha.core.ItemState;
import org.priha.core.PropertyImpl;
import org.priha.path.Path;
import org.priha.providers.ValueContainer;

// FIXME: This could be a lot faster when doing get()s for example.

public class ChangeStore implements Iterable<ChangeStore.Change>
{
    private LinkedList<Change> m_changes = new LinkedList<Change>();
    
    public ItemImpl get( Path path )
    {
        Change c = getChange( path );
        
        if( c != null ) return c.getItem();
        
        return null;
    }
    
    /**
     *  Finds the latest change.
     *  
     *  @param path
     *  @return
     */
    public Change getChange( Path path )
    {
        for( ListIterator<Change> i = m_changes.listIterator(m_changes.size()); i.hasPrevious(); )
        {
            Change c = i.previous();
            
            if( c.getPath().equals(path) ) return c;
        }
        
        return null;        
    }
    
    public void add( ItemState newState, ItemImpl ii )
    {
        Change c = new Change( newState, ii );
        
        add( c );
    }
    
    public boolean add( Change c )
    {
//        Change prev = getChange( c.getPath() );
        
//        if( prev != null ) System.out.println("Adding to old: "+prev.getState()+", "+c.getState());
//        if( prev != null )
//        {
//            if( prev.getState().equals(c.getState()) || (prev.getState().equals(ItemState.REMOVED) && c.getState().equals(ItemState.EXISTS) ) )
//            {
//                // We are repeating what already happened.  This happens because of setModified(),
//                // so we're ignoring it here.
//                return false;
//            }
//        }
        
        if( c.getState() == ItemState.UNDEFINED )
        {
            dump();
            throw new IllegalStateException("You cannot add an UNDEFINED Item "+c.getItem().getInternalPath());
        }
        
        m_changes.add(c);
        
        return true;
    }
    
    /**
     *  Gets the first change from the change list.  Returns null, if there are no more
     *  changes.
     *  
     *  @return
     */
    public Change peek()
    {
        return m_changes.peek();
    }
    
    /**
     *  Removes the first change from the change list.
     * @return
     */
    public Change remove() 
    {
        return m_changes.poll();
    }
    
    /**
     *  Clears the changes.
     *  
     *  @return The number of changes removed from the queue.
     */
    public int clear()
    {
        int numChanges = m_changes.size();
        m_changes.clear();
        
        return numChanges;
    }

    public Iterator<Change> iterator()
    {
        return m_changes.iterator();
    }

    public Iterator<ItemImpl> values()
    {
        return new ItemIterator();
    }
    
    public boolean isEmpty()
    {
        return m_changes.isEmpty();
    }
    
    private class ItemIterator implements Iterator<ItemImpl>
    {
        int m_position;
        
        protected ItemIterator()
        {
            m_position = m_changes.size()-1;
        }
        
        public boolean hasNext()
        {
            return m_position > 0;
        }

        public ItemImpl next()
        {
            return m_changes.get(m_position--).getItem();
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
        
    }
    
    public void dump()
    {
        System.out.println("DUMP OF CHANGESTORE @"+Integer.toHexString(this.hashCode()));
        System.out.println(this);
    }
    
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        for( Change c : this )
        {
            sb.append( c.getState().toString() + ": " + c.getPath() + ": " + c.getItem() + "\n" );
        }
        return sb.toString();
    }
    
    /**
     *  Stores a single change.
     */
    public static class Change
    {
        private ItemState m_state;
        private ItemImpl  m_item;
        private Path      m_path;
        private ValueContainer m_valueContainer;
        
        public Change( ItemState newState, ItemImpl item )
        {
            m_state = newState;
            m_item  = item;
            m_path  = item.getInternalPath();
            if( !item.isNode() )
            {
                m_valueContainer = ((PropertyImpl)item).getValueContainer();
                if( m_valueContainer.isEmpty() ) throw new IllegalArgumentException("Null value in valuecontainer for "+m_path);
            }
        }
        
        public ItemImpl getItem()
        {
            return m_item;
        }
        
        public ItemState getState()
        {
            return m_state;
        }
        
        public Path getPath()
        {
            return m_path;
        }

        public ValueContainer getValue()
        {
            return m_valueContainer;
        }
    }
}
