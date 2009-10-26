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
import org.priha.path.Path;
import org.priha.path.Path.Component;
import org.priha.util.ConfigurationException;
import org.priha.util.QName;

/**
 *  Holds the contents in memory only.   It's very fast, though creation
 *  of the initial Session may take a while.
 *  <p>
 *  The contents of this provider will disappear once you shut down the JVM.
 *  <p>
 *  Most of the operations in this provider are O(log N).
 *  <p>
 *  This providers supports a single workspace only.  You may set it up with e.g.
 *  <pre>
 *     priha.provider.memory.workspaces = myworkspace
 *  </pre>
 *  The property is aligned with other RepositoryProviders, even though it will only
 *  support a single one.  By default, this workspace will be called "default".
 */
// FIXME: Does not yet support orderable child nodes
public class MemoryProvider implements RepositoryProvider
{
    private static final int         INITIAL_SIZE = 1024;
    private static final String      PROP_WORKSPACES = "workspaces";
    
    private Map<Path,TreeNode>       m_nodePaths = new HashMap<Path,TreeNode>();
    private Map<String,Path>         m_uuids     = new Hashtable<String,Path>(INITIAL_SIZE);
    private String                   m_workspace = "default";
    
    public void addNode(StoreTransaction tx, Path path, QNodeDefinition def) throws RepositoryException
    {
        //System.out.println("Node++ "+path);
        if( !m_nodePaths.containsKey(path) )
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

    // TODO: This is fairly slow, as it's O(N).
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
        return Arrays.asList( new String[] { m_workspace } );
    }

    public boolean nodeExists(WorkspaceImpl ws, Path path)
    {
        return m_nodePaths.containsKey(path);
    }

    public void open(RepositoryImpl rep, Credentials credentials, String workspaceName)
        throws RepositoryException,NoSuchWorkspaceException
    {
        if( !workspaceName.equals(m_workspace) ) throw new NoSuchWorkspaceException();
    }

    public void putPropertyValue(StoreTransaction tx, Path path, ValueContainer vc) throws RepositoryException
    {
        if( vc.isMultiple() )
        {
            //
            //  Needed to fix a casting issue: ValueImpl[] is not a subtype of Value[], even though
            //  ValueImpl implements Value[].
            //
            vc = vc.deepClone(tx.getWorkspace().getSession());
        }
        else
        {
            ValueImpl value = vc.getValue();
            if( path.getLastComponent().equals(JCRConstants.Q_JCR_UUID) )
            {
                m_uuids.put( value.getString(), path.getParentPath() );
            }
         
            vc = vc.deepClone(tx.getWorkspace().getSession());
        }
        
        TreeNode parent = m_nodePaths.get(path.getParentPath());
        
        if( parent == null ) throw new PathNotFoundException("Parent path not found "+path.getParentPath());
        
        parent.properties.put(path.getLastComponent(), vc);                                            
       
//        System.out.println("Stored "+path);
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
        
        if( nd.children != null )
        {
            for( Iterator<Path> i = nd.children.iterator(); i.hasNext(); )
            {
                Path child = i.next();
                remove( tx, child );
            }
        }

        m_nodePaths.remove(path);
    }

    public void start(RepositoryImpl repository, Properties properties) throws ConfigurationException
    {
        String wsnames = properties.getProperty( PROP_WORKSPACES, m_workspace );
        String[] wsn = wsnames.split( "\\s" );
        if( wsn.length == 0 )
        {
            throw new ConfigurationException("Empty "+PROP_WORKSPACES+" -property found!");
        }
        else if( wsn.length > 1 )
        {
            throw new ConfigurationException("MemoryProvider only supports a single workspace. If you need multiple workspaces, please set up a separate MemoryProvider for each.");
        }
        
        m_workspace = wsn[0];
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

    /**
     *  Stores a Node in the memory, including parent, all children and all properties.
     */
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

    public void reorderNodes(StoreTransaction tx, Path internalPath, List<Path> childOrder) throws RepositoryException
    {
        throw new UnsupportedRepositoryOperationException();
    }
    
    public void rename(StoreTransaction tx, Path path, Component newName) throws RepositoryException
    {
        throw new UnsupportedRepositoryOperationException();
    }

}
