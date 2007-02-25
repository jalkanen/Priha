package org.jspwiki.priha.util;

import java.util.ArrayList;

import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.jspwiki.priha.core.PropertyImpl;

public class PropertyList extends ArrayList<PropertyImpl>
{
    private static final long serialVersionUID = 1L;

    public PropertyImpl find( String name )
    {
        for( PropertyImpl p : this )
        {
            try
            {
                if( p.getName().equals(name) )
                    return p;
            }
            catch (RepositoryException e)
            {
                // Skip this - should never happen with Priha
            }
        }
        
        return null;
    }
    
    public boolean hasProperty( String name )
    {
        return find(name) != null;
    }
    
    /**
     *  Returns a PropertyIterator for this list.
     *  @return
     */
    public PropertyIterator propertyIterator()
    {
        return new PropertyIteratorImpl(this);
    }
}
