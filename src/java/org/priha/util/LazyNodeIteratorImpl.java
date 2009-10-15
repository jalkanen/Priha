package org.priha.util;

import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.priha.core.NodeImpl;
import org.priha.core.SessionImpl;
import org.priha.path.Path;

public class LazyNodeIteratorImpl extends GenericIterator implements NodeIterator
{
    private SessionImpl m_session;
    
    public LazyNodeIteratorImpl( SessionImpl session, List<Path> paths )
    {
        super(paths);
        m_session = session;
    }
    
    public NodeImpl nextNode()
    {
        Path p = (Path)super.next();

        try
        {
            return (NodeImpl)m_session.getItem( p );
        }
        catch( RepositoryException e )
        {
            e.printStackTrace();
            throw new NoSuchElementException("No next node is available: "+e.getMessage());
        }
    }

    public NodeImpl previousNode()
    {
        Path p = (Path)super.previous();
        
        try
        {
            return (NodeImpl)m_session.getItem( p );
        }
        catch( RepositoryException e )
        {
            e.printStackTrace();
            throw new NoSuchElementException("No next node is available: "+e.getMessage());
        }
    }
    
    public NodeImpl next()
    {
        return nextNode();
    }
    
    public NodeImpl previous()
    {
        return previousNode();
    }
    
    public NodeImpl get( int index ) throws PathNotFoundException, RepositoryException
    {
        return (NodeImpl) m_session.getItem( (Path) m_list.get(index) );
    }

}
