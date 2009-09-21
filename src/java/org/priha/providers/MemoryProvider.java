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
import org.priha.core.values.ValueImpl;
import org.priha.nodetype.QNodeDefinition;
import org.priha.util.ConfigurationException;
import org.priha.util.Path;
import org.priha.util.QName;

/**
 *  Holds the contents in memory only.   It's very fast, though creation
 *  of the initial Session may take a while.
 *  
 */
public class MemoryProvider implements RepositoryProvider
{
    private static final int INITIAL_SIZE = 1024;
    private Map<Path,TreeNode>       m_nodePaths = new HashMap<Path,TreeNode>();
    private Map<String,Path>         m_uuids     = new Hashtable<String,Path>(INITIAL_SIZE);
    
    public void addNode(StoreTransaction tx, Path path, QNodeDefinition def) throws RepositoryException
    {
        //System.out.println("Node++ "+path);
        m_nodePaths.put( path, new TreeNode(path.isRoot() ? null : path.getParentPath(), null) );
    }

    public void close(WorkspaceImpl ws)
    {
    }

    public Path findByUUID(WorkspaceImpl ws, String uuid) throws RepositoryException
    {
        Path p = m_uuids.get( uuid );
        
        if( p == null )
            throw new ItemNotFoundException("No Node for uuid "+uuid);
        
        return p;
    }

    public List<Path> findReferences(WorkspaceImpl ws, String uuid) throws RepositoryException
    {
        ArrayList<Path> res = new ArrayList<Path>();
        
        for( Map.Entry<Path,TreeNode> e : m_nodePaths.entrySet() )
        {
            ValueContainer vc = e.getValue().properties.get(JCRConstants.Q_JCR_UUID);
            
            if( vc != null )
            {
                if( vc.getValue().getString().equals(uuid) )
                {
                    res.add( e.getKey() );
                }
            }
        }
        
        return res;
    }

    public ValueContainer getPropertyValue(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        TreeNode o = m_nodePaths.get(path.getParentPath());
        
        if( o == null ) throw new PathNotFoundException(path.toString());
        
        ValueContainer vc = o.properties.get(path.getLastComponent());
        
        if( vc == null ) throw new PathNotFoundException(path.toString());
        
        return vc.sessionInstance( ws.getSession() );
    }

    public List<Path> listNodes(WorkspaceImpl ws, Path parentpath) throws RepositoryException
    {
        ArrayList<Path> res = new ArrayList<Path>();
        
        TreeNode nd = m_nodePaths.get(parentpath);
        
        if( nd != null && nd.children != null ) res.addAll(nd.children);
                
        return res;
    }

    public List<QName> listProperties(WorkspaceImpl ws, Path path) throws RepositoryException
    {
        ArrayList<QName> res = new ArrayList<QName>();
        
        TreeNode tn = m_nodePaths.get(path);
        
        if( tn == null ) throw new ItemNotFoundException(path.toString(ws.getSession()));
        
        for( QName qn : tn.properties.keySet() )
        {
            res.add(qn);
        }
        
        return res;
    }

    public Collection<String> listWorkspaces()
    {
        return Arrays.asList( new String[] { "default" } );
    }

    public boolean nodeExists(WorkspaceImpl ws, Path path)
    {
        return m_nodePaths.containsKey(path);
    }

    public void open(RepositoryImpl rep, Credentials credentials, String workspaceName)
        throws RepositoryException,NoSuchWorkspaceException
    {
        if( !workspaceName.equals("default") ) throw new NoSuchWorkspaceException();
    }

    public void putPropertyValue(StoreTransaction tx, PropertyImpl property) throws RepositoryException
    {
        ValueContainer vc;
        
        if( property.getDefinition().isMultiple() )
        {
            //
            //  Needed to fix a casting issue: ValueImpl[] is not a subtype of Value[], even though
            //  ValueImpl implements Value[].
            //
            Value[] values = property.getValues();
            ValueImpl[] pseudovals = new ValueImpl[values.length];
            for( int i = 0; i < values.length; i++ ) pseudovals[i] = (ValueImpl)values[i];
            
            vc = new ValueContainer(pseudovals, property.getType());
        }
        else
        {
            ValueImpl value = property.getValue();
            if( property.getQName().equals(JCRConstants.Q_JCR_UUID) )
            {
                m_uuids.put( value.getString(), property.getInternalPath().getParentPath() );
            }
         
            vc = new ValueContainer(value);
        }
        
        TreeNode parent = m_nodePaths.get(property.getInternalPath().getParentPath());
        
        if( parent == null ) throw new PathNotFoundException("Parent path not found "+property.getInternalPath().getParentPath());
        
        parent.properties.put(property.getQName(), vc);                                            
        
        //System.out.println("Stored "+property.getInternalPath());
    }

    public void remove(StoreTransaction tx, Path path) throws RepositoryException
    {
        TreeNode nd = m_nodePaths.get(path);
        
        if( nd == null ) return; // Already removed
        
        ValueContainer uuidvc = nd.properties.get(JCRConstants.Q_JCR_UUID);
        
        if( uuidvc != null )
        {
            m_uuids.remove(uuidvc.getValue().getString());
        }
        
        for( Iterator<Path> i = nd.children.iterator(); i.hasNext(); )
        {
            Path child = i.next();
            remove( tx, child );
        }

        m_nodePaths.remove(path);
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

    private static class TreeNode
    {
        Path                          parent;
        ArrayList<Path>               children;
        HashMap<QName,ValueContainer> properties = new HashMap<QName,ValueContainer>();
        
        public TreeNode(Path p,ArrayList<Path> c)
        {
            parent   = p;
            children = c;
        }
    }
}
