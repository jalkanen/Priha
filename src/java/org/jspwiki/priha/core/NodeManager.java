package org.jspwiki.priha.core;

import java.util.HashMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.jspwiki.priha.util.InvalidPathException;
import org.jspwiki.priha.util.Path;

/**
 *  Is responsible for storing the nodes in memory.
 *  
 *  @author jalkanen
 *
 */
public class NodeManager
{
    private HashMap<String,NodeImpl> m_nodeReferences = new HashMap<String,NodeImpl>();
    
    private SessionImpl m_session;
    
    public NodeManager( SessionImpl session )
        throws RepositoryException
    {
        m_session = session;
        
        reset();
    }
    
    /**
     *  This is a factory method for creating new nodes.
     *  @param path
     *  @return
     * @throws InvalidPathException 
     */
    public NodeImpl getOrCreateNode( Path path )
        throws RepositoryException, InvalidPathException
    {
        NodeImpl ni = findNode( path );
        
        if( ni == null )
        {
            ni = new NodeImpl( m_session, path );
            addNode(ni);
        }
        
        return ni;
    }
    
    /**
     *  Finds a node.  Returns null in case a node is not found.
     * @param path
     * @return
     */
    public NodeImpl findNode( Path path )
    {
        PathIndex pi = new PathIndex(path);

        NodeImpl ni = m_nodeReferences.get(path.toString());

        if( ni == null && pi.m_index == 0 )
        {
            ni = m_nodeReferences.get( path.toString()+"[1]" );
        }
        return ni;
    }
    
    
    public boolean hasNode( String path )
    {
        return findNode( new Path(path) ) != null;
    }
    
    public void addNode( NodeImpl node ) throws RepositoryException, InvalidPathException
    {
        if( !node.m_path.isRoot() )
        {
            Path parentPath = node.m_path.getParentPath();
            NodeImpl parent = findNode(parentPath);
            parent.addChildNode( node );
            node.m_parent = parent;
            
            NodeIterator i = parent.getNodes(node.m_path.getLastComponent());
            long index = i.getSize();
            
            if( index > 1 )
            {
                String newpath = node.getPath()+"["+index+"]";
                node.m_path = new Path(newpath);
            }
        }
        
        m_nodeReferences.put( node.getPath(), node );
    }
    
    protected void reset() throws RepositoryException
    {
        m_nodeReferences.clear();
        
        NodeImpl ni = new NodeImpl(m_session,"/");
        
        ni.sanitize();
        
        m_nodeReferences.put( "/",ni );
    }

    public void remove( NodeImpl node ) throws RepositoryException
    {
        if( node.getDepth() == 0 ) throw new RepositoryException("Root cannot be removed.");
        
        m_nodeReferences.remove( node.getPath() );
        node.m_parent.removeChildNode(node);
        
        for( NodeIterator ndi = node.getNodes(); ndi.hasNext(); )
        {
            Node nd = ndi.nextNode();
            
            nd.remove();
        }
    }
    
    private static class PathIndex
    {
        public Path m_path;
        public int  m_index;
        
        public PathIndex( Path path )
        {
            String n = path.getLastComponent();
            String basePath = path.toString();

            int s, index = 0;
            if( (s = n.indexOf('[')) != -1 )
            {
                basePath = basePath.substring( 0, basePath.lastIndexOf('[') );
                String indexInt = n.substring(s+1,n.length()-1);
                index = Integer.parseInt( indexInt ) - 1;
            }
            
            m_path  = new Path(basePath);
            m_index = index;
        }
    }
    

}
