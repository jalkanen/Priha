/*
    Priha - A JSR-170 implementation library.

    Copyright (C) 2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package org.priha.providers;

import java.util.*;

import javax.jcr.*;

import org.priha.core.JCRConstants;
import org.priha.core.PropertyImpl;
import org.priha.core.RepositoryImpl;
import org.priha.core.WorkspaceImpl;
import org.priha.core.values.QValue;
import org.priha.core.values.ValueImpl;
import org.priha.nodetype.QNodeDefinition;
import org.priha.util.ConfigurationException;
import org.priha.util.Path;
import org.priha.util.QName;

/**
 *  Holds the contents in memory only.   It's very fast, though creation
 *  of the initial Session may take a while.
 *  
 *  Missing: QNames
 */
public class MemoryProvider implements RepositoryProvider
{
    private static final int INITIAL_SIZE = 1024;
    private Map<Path,ValueContainer> m_values    = new Hashtable<Path,ValueContainer>(INITIAL_SIZE);
    private Set<Path>        m_nodePaths = new TreeSet<Path>();
    private Map<String,Path> m_uuids     = new Hashtable<String,Path>(INITIAL_SIZE);
    
    public void addNode(StoreTransaction tx, Path path, QNodeDefinition def) throws RepositoryException
    {
        //System.out.println("Node++ "+path);
        m_nodePaths.add( path );
    }

    public void close(WorkspaceImpl ws)
    {
    }

    public void copy(WorkspaceImpl ws, Path srcpath, Path destpath) throws RepositoryException
    {
        throw new UnsupportedRepositoryOperationException();
    }

    public Path findByUUID(WorkspaceImpl ws, String uuid) throws RepositoryException
    {
        return m_uuids.get( uuid );
    }

    public List<Path> findReferences(WorkspaceImpl ws, String uuid) throws RepositoryException
    {
        ArrayList<Path> res = new ArrayList<Path>();
        
        for( Map.Entry<Path,ValueContainer> e : m_values.entrySet() )
        {
            if( !e.getValue().isMultiple() && 
                e.getKey().getLastComponent().equals(JCRConstants.Q_JCR_UUID) )
            {
                if( e.getValue().getValue().getString().equals(uuid) )
                {
                    res.add( e.getKey() );
                }
            }
        }
        
        return res;
    }

    public ValueContainer getPropertyValue(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        ValueContainer o = m_values.get(path);
        
        if( o == null ) throw new PathNotFoundException(path.toString());
        
        return o.sessionInstance( ws.getSession() );
    }

    public List<Path> listNodes(WorkspaceImpl ws, Path parentpath) throws RepositoryException
    {
        ArrayList<Path> res = new ArrayList<Path>();
        
        for( Path p : m_nodePaths )
        {
            if( parentpath.isParentOf(p) && parentpath.depth() == p.depth()-1 ) // Only direct parents.
            {
                res.add(p);
            }
        }
        
        return res;
    }

    public List<QName> listProperties(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        ArrayList<QName> res = new ArrayList<QName>();
        
        for( Path p : m_values.keySet() )
        {
            if( path.isParentOf(p) && path.depth() == p.depth()-1 )
            {
                res.add(p.getLastComponent());
            }
        }
        
        return res;
    }

    public Collection<String> listWorkspaces()
    {
        return Arrays.asList( new String[] { "default" } );
    }

    public void move(WorkspaceImpl ws, Path srcpath, Path destpath) throws RepositoryException
    {
        throw new UnsupportedRepositoryOperationException();
    }

    public boolean nodeExists(WorkspaceImpl ws, Path path)
    {
        return m_nodePaths.contains(path);
    }

    public void open(RepositoryImpl rep, Credentials credentials, String workspaceName)
        throws RepositoryException,NoSuchWorkspaceException
    {
        if( !workspaceName.equals("default") ) throw new NoSuchWorkspaceException();
    }

    public void putPropertyValue(StoreTransaction tx, PropertyImpl property) throws RepositoryException
    {
        if( property.getDefinition().isMultiple() )
        {
            //
            //  Needed to fix a casting issue: ValueImpl[] is not a subtype of Value[], even though
            //  ValueImpl implements Value[].
            //
            Value[] values = property.getValues();
            ValueImpl[] pseudovals = new ValueImpl[values.length];
            for( int i = 0; i < values.length; i++ ) pseudovals[i] = (ValueImpl)values[i];
            
            m_values.put( property.getInternalPath(), new ValueContainer(pseudovals,
                                                                         property.getType()) );
        }
        else
        {
            ValueImpl value = property.getValue();
            if( property.getQName().equals(JCRConstants.Q_JCR_UUID) )
            {
                m_uuids.put( value.getString(), property.getInternalPath().getParentPath() );
            }
            
            m_values.put( property.getInternalPath(), new ValueContainer(value) );    
        }
        
        //System.out.println("Stored "+property.getInternalPath());
    }

    public void remove(StoreTransaction tx, Path path) throws RepositoryException
    {
        for( Iterator<Path> i = m_nodePaths.iterator(); i.hasNext(); )
        {
            Path p = i.next();
            
            if( path.isParentOf(p) || path.equals( p ))
                i.remove();
        }
        
        for( Iterator<Map.Entry<Path,ValueContainer>> i = m_values.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry<Path, ValueContainer> e = i.next();
            
            if( path.isParentOf(e.getKey()) || path.equals(e.getKey()) )
                i.remove();
        }
        
        for( Map.Entry<String,Path> e : m_uuids.entrySet() )
        {
            if( path.isParentOf(e.getValue()) || path.equals(e.getValue()))
            {
                m_uuids.remove(e.getKey());
                break;
            }
        }
    }

    public void start(RepositoryImpl repository, Properties properties) throws ConfigurationException
    {
    }

    public void stop(RepositoryImpl rep)
    {
    }

    //  The MemoryProvider assumes it never fails.
    
    public void storeFinished( StoreTransaction tx )
    {
    }

    public StoreTransaction storeStarted( WorkspaceImpl ws )
    {
        return new BaseStoreTransaction(ws);
    }

    public void storeCancelled( StoreTransaction tx ) throws RepositoryException
    {
    }

}
