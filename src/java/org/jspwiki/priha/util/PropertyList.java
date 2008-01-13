package org.jspwiki.priha.util;

import java.util.ArrayList;

import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.jspwiki.priha.core.PropertyImpl;
import org.jspwiki.priha.core.SessionImpl;

public class PropertyList extends ArrayList<PropertyImpl>
{
    private static final long serialVersionUID = 1L;

    /**
     *  Creates a clone of a PropertyList using a given session.
     *  
     *  @param original
     *  @param session
     * @throws RepositoryException 
     * @throws IllegalStateException 
     * @throws ValueFormatException 
     */
    public PropertyList(PropertyList original, SessionImpl session) throws ValueFormatException, IllegalStateException, RepositoryException
    {
        for( PropertyImpl pi : original )
        {
            PropertyImpl pi_new = new PropertyImpl( pi, session );
            add( pi_new );
        }
    }

    public PropertyList()
    {
        super();
    }
    
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
