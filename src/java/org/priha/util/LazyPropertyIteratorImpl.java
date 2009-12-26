package org.priha.util;

import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.priha.core.PropertyImpl;
import org.priha.core.SessionImpl;
import org.priha.path.Path;

public class LazyPropertyIteratorImpl extends GenericIterator implements PropertyIterator
{
    private SessionImpl m_session;
    
    public LazyPropertyIteratorImpl( SessionImpl session, List<Path> paths )
    {
        super( paths );
        m_session = session;
    }

    public PropertyImpl nextProperty()
    {
        Path p = (Path)super.next();

        try
        {
            return (PropertyImpl)m_session.getItem( p );
        }
        catch( RepositoryException e )
        {
            e.printStackTrace();
            throw new NoSuchElementException("No next property is available: "+e.getMessage());
        }
    }


    public PropertyImpl previousProperty()
    {
        Path p = (Path)super.previous();
        
        try
        {
            return (PropertyImpl)m_session.getItem( p );
        }
        catch( RepositoryException e )
        {
            e.printStackTrace();
            throw new NoSuchElementException("No previous property is available: "+e.getMessage());
        }
    }
    
    public PropertyImpl next()
    {
        return nextProperty();
    }
    
    public PropertyImpl previous()
    {
        return previousProperty();
    }
    
    public PropertyImpl get( int index ) throws PathNotFoundException, RepositoryException
    {
        return (PropertyImpl) m_session.getItem( (Path) m_list.get(index) );
    }

}
