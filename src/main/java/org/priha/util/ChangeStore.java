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

import java.util.*;

import org.priha.core.ItemImpl;
import org.priha.core.ItemState;
import org.priha.core.PropertyImpl;
import org.priha.path.InvalidPathException;
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
 *  <p>
 *  In addition, it stores a separate reference to all the changes relating
 *  to a particular parent path (making it fairly fast to filter based on a path).
 */
public class ChangeStore implements Iterable<ChangeStore.Change>
{
    private ArrayList<Change>    m_changes = new ArrayList<Change>();
    private HashMap<Path,Change> m_latest = new HashMap<Path,Change>();
    private HashMap<Path,List<Change>> m_childChanges = new HashMap<Path,List<Change>>();
        
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
        Change c = m_latest.get( path );
        return c;
    }
    
    /**
     *  Get all the latest changes, ignoring any duplicate changes.
     *  
     *  @return A list of latest changes.
     */
    public List<Change> getLatestChanges()
    {
        ArrayList<Change> changes = new ArrayList<Change>();
        
        changes.addAll( m_latest.values() );
        
        return changes;
    }
    
    /**
     *  Returns a list of Changes filtered by a parent path.
     *  
     *  @param parent The path to filter against
     *  @return A list of Changes.
     */
    public List<Change> getLatestChangesForParent( Path parent )
    {
        ArrayList<Change> changes = new ArrayList<Change>();

        List<Change> c = m_childChanges.get(parent);
        
        if( c != null )
            changes.addAll( c );
        
        return changes;
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
        
        m_latest.put( c.getPath(), c );
        
        if( !c.getPath().isRoot() )
        {
            try
            {
                List<Change> pc = m_childChanges.get( c.getPath().getParentPath() );
                if( pc == null )
                {
                    pc = new ArrayList<Change>();
                    m_childChanges.put( c.getPath().getParentPath(), pc );
                }
            
                pc.add( c );
            }
            catch( InvalidPathException e )
            {
                throw new IllegalStateException("Cannot add path "+c.getPath());
            }
        }
        
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
    
    public boolean remove( Change c )
    {
        if( m_changes.remove(c) )
        {
            internalRemove(c);
            
            return true;
        }
        
        return false;
    }

    private void internalRemove(Change c)
    {
        m_latest.remove(c.getPath());
        
        if( !c.getPath().isRoot() )
        {
            try
            {
                Path parent = c.getPath().getParentPath();
            
                List<Change> pc = m_childChanges.get( parent );
        
                pc.remove( c ); // FIXME: repeated with remove().
            
                if( pc.isEmpty() )
                {
                    m_childChanges.remove( parent );
                }        
            }
            catch( InvalidPathException e )
            {
                throw new IllegalStateException("Cannot remove "+c.getPath());
            }
        }
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
            internalRemove(c);   
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
        m_childChanges.clear();
        return numChanges;
    }

    /**
     *  Returns a forward iterator for the Changes.
     *  
     *  @return A forward iterator for the Changes.
     */
    public Iterator<Change> iterator()
    {
        return new ForwardIterator();
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
    public final boolean isEmpty()
    {
        return m_changes.isEmpty();
    }
    
    public void addAll( ChangeStore store )
    {
        for( Change c : store )
        {
            add( c );
        }
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
        
        public final boolean hasNext()
        {
            return m_position > 0;
        }

        public final ItemImpl next()
        {
            return m_changes.get(m_position--).getItem();
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
        
    }

    private class ForwardIterator implements Iterator<Change>
    {
        int m_position;
        
        protected ForwardIterator()
        {
            m_position = 0;
        }
        
        public final boolean hasNext()
        {
            return m_position < m_changes.size();
        }

        public final Change next()
        {
            return m_changes.get(m_position++);
        }

        public void remove()
        {
            if( m_position == 0 ) throw new IllegalStateException();
            
            Change c = m_changes.remove( --m_position );
            m_latest.remove( c.getPath() );
            
            if( !c.getPath().isRoot() )
            {
                try
                {
                    Path parent = c.getPath().getParentPath();
                
                    List<Change> pc = m_childChanges.get( parent );
                
                    pc.remove( c ); // FIXME: SLow.
                    
                    if( pc.isEmpty() )
                    {
                        m_childChanges.remove( parent );
                    }
                }
                catch( InvalidPathException e )
                {
                    throw new IllegalStateException("Cannot remove "+c.getPath());
                }
            }

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
    public static final class Change
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
                if( m_valueContainer.isEmpty() && newState != ItemState.REMOVED ) throw new IllegalArgumentException("Null value in valuecontainer for "+m_path);
            }
        }
        
        /**
         *  Returns the Item for this Change.
         *  
         *  @return The Item.
         */
        public final ItemImpl getItem()
        {
            return m_item;
        }
        
        /**
         *  Returns the ItemState for this Change.
         *  
         *  @return The ItemState.
         */
        public final ItemState getState()
        {
            return m_state;
        }
        
        /**
         *  Returns the Path for this Change.
         *  
         *  @return The Path.
         */
        public final Path getPath()
        {
            return m_path;
        }

        /**
         *  Return the ValueContainer for the Change.
         *  
         *  @return The ValueContainer, or null, if the Change concerned a Node.
         */
        public final ValueContainer getValue()
        {
            return m_valueContainer;
        }
        
        public String toString()
        {
            return m_state.toString()+" "+m_item.getInternalPath();
        }
    }
}
