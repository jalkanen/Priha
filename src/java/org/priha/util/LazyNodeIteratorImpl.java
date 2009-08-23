package org.priha.util;

import java.util.List;

import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.priha.core.NodeImpl;
import org.priha.core.SessionImpl;

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
            return null;
        }
    }

    public NodeImpl next()
    {
        return nextNode();
    }
    
    public NodeImpl get( int index ) throws PathNotFoundException, RepositoryException
    {
        return (NodeImpl) m_session.getItem( (Path) m_list.get(index) );
    }

}
