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
