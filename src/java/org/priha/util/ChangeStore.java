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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;

import org.priha.core.ItemImpl;
import org.priha.core.ItemState;
import org.priha.core.PropertyImpl;
import org.priha.path.Path;
import org.priha.providers.ValueContainer;

/**
 *  Provides a list of changes, which can be both played back
 *  one by one, as well as searched rapidly.
 *  <p>
 *  Internally, this class stores both a List of Change objects,
 *  as well as a HashMap pointing at the latest change.  It can be
 *  iterated both forwards (using peek() and remove() and iterator()) as well
 *  as backwards (using values()).
 */
public class ChangeStore implements Iterable<ChangeStore.Change>
{
    private ArrayList<Change>   m_changes = new ArrayList<Change>();
    private boolean              m_useHashMap;
    private HashMap<Path,Change> m_latest = new HashMap<Path,Change>();
    
    /**
     *  Create a ChangeStore.
     *  
     *  @param useHashMap If true, uses a HashMap to speed things up internally.
     */
    public ChangeStore(boolean useHashMap)
    {
        m_useHashMap = useHashMap;
    }
    
    /**
     *  Create a ChangeStore without the HashMap.
     */
    public ChangeStore()
    {}
    
    /**
     *  Returns the newest ItemImpl that corresponds to the Path given.
     *  
     *  @param path Path to search for
     *  @return Newest Item or null, if no such thing is found.
     */
    public ItemImpl getLatestItem( Path path )
    {
        Change c = getLatestChange( path );
        
        if( c != null ) return c.getItem();
        
        return null;
    }
    
    /**
     *  Finds the latest change.
     *  
     *  @param path Path to search for
     *  @return The Change or null, if no such thing found.
     */
    public Change getLatestChange( Path path )
    {
        if( m_useHashMap )
        {
            Change c = m_latest.get( path );
            return c;
        }
        
        for( ListIterator<Change> i = m_changes.listIterator(m_changes.size()); i.hasPrevious(); )
        {
            Change c = i.previous();
            
            if( c.getPath().equals(path) ) return c;
        }
        
        return null;        
    }
    
    /**
     *  Adds a new Item with given ItemState to the end of the Change List.
     *  
     *  @param newState New state
     *  @param ii The Item
     */
    public void add( ItemState newState, ItemImpl ii )
    {
        Change c = new Change( newState, ii );
        
        add( c );
    }
    
    /**
     *  Adds a whole Change object at the end of the Change List.
     *  
     *  @param c The Change to add
     *  @return True, at the moment.
     */
    public boolean add( Change c )
    {        
        if( c.getState() == ItemState.UNDEFINED )
        {
            dump();
            throw new IllegalStateException("You cannot add an UNDEFINED Item "+c.getItem().getInternalPath());
        }
        
        m_changes.add(c);
        
        if( m_useHashMap )
            m_latest.put( c.getPath(), c );
        
        return true;
    }
    
    /**
     *  Gets the first change from the change list.  Returns null, if there are no more
     *  changes.
     *  
     *  @return The first change from the list, or null, if the list was empty.
     */
    public Change peek()
    {
        if( m_changes.size() > 0 )
            return m_changes.get(0);
        
        return null;
    }
    
    /**
     *  Removes the first change from the change list.
     *  
     *  @return The first change from the list, or null, if the list was empty. 
     */
    public Change remove() 
    {
        Change c = null;
        if( m_changes.size() > 0 )
        {
            c = m_changes.remove(0);
            if( m_useHashMap )
            {
                m_latest.remove( c.getPath() );
            }
        }
        return c;
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
        m_latest.clear();
        return numChanges;
    }

    /**
     *  Returns a forward iterator for the Changes.
     *  
     *  @return A forward iterator for the Changes.
     */
    public Iterator<Change> iterator()
    {
        return m_changes.iterator();
    }

    /**
     *  Returns a <b>backward</b> iterator for the Items in the change list.  The first
     *  value you get is the newest value on the stack. This means
     *  that the iteration order for iterator() and values() is reversed.
     * 
     *  @return A reverse iterator for the values.
     */
    public Iterator<ItemImpl> values()
    {
        return new ItemIterator();
    }
    
    /**
     *  Returns true, if there are no changes.
     *  
     *  @return True, if there are no changes.
     */
    public boolean isEmpty()
    {
        return m_changes.isEmpty();
    }
    
    /**
     *  Implements a backwards iterator through the Items in the list.
     */
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
    
    /**
     *  Dumps the store contents for debugging to System.out.
     */
    public void dump()
    {
        System.out.println("DUMP OF CHANGESTORE @"+Integer.toHexString(this.hashCode()));
        System.out.println(this);
    }
    
    /**
     *  Outputs a human-readable description of the contents of the ChangeStore.
     *  
     *  @return Somethign human-readable.
     */
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
        
        /**
         *  Create a new Change for the given ItemState and item.  If the
         *  Item is a property, also the Value is copied internally.
         *  
         *  @param newState The new state
         *  @param item The item
         */
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
        
        /**
         *  Returns the Item for this Change.
         *  
         *  @return The Item.
         */
        public ItemImpl getItem()
        {
            return m_item;
        }
        
        /**
         *  Returns the ItemState for this Change.
         *  
         *  @return The ItemState.
         */
        public ItemState getState()
        {
            return m_state;
        }
        
        /**
         *  Returns the Path for this Change.
         *  
         *  @return The Path.
         */
        public Path getPath()
        {
            return m_path;
        }

        /**
         *  Return the ValueContainer for the Change.
         *  
         *  @return The ValueContainer, or null, if the Change concerned a Node.
         */
        public ValueContainer getValue()
        {
            return m_valueContainer;
        }
    }
}
