package org.jspwiki.priha.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.jcr.*;
import javax.jcr.nodetype.NodeDefinition;

import org.jspwiki.priha.nodetype.GenericNodeType;
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
            ni = (NodeImpl) getRootNode().addNode( path.toString() );
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
        //PathIndex pi = new PathIndex(path);

        NodeImpl ni = m_nodeReferences.get(path.toString());
/*
        if( ni == null && pi.m_index == 0 )
        {
            ni = m_nodeReferences.get( path.toString()+"[1]" );
        }
 */
        return ni;
    }
    
    
    public boolean hasNode( String path )
    {
        return findNode( new Path(path) ) != null;
    }
    
    public void addNode( NodeImpl parentNode, NodeImpl node ) throws RepositoryException, InvalidPathException
    {
        if( parentNode != null )
        {
            parentNode.addChildNode( node );
         
            pullUpNode( parentNode );
            /*
            // Only used if supports samenamesiblings
            NodeIterator i = parent.getNodes(node.m_path.getLastComponent());
            long index = i.getSize();
            
            if( index > 1 )
            {
                String newpath = node.getPath()+"["+index+"]";
                node.m_path = new Path(newpath);
            }
            */
        }
        
        pullUpNode( node );
    }
    
    /**
     *  Throws away the entire changed items.
     *  
     *  @throws RepositoryException
     */
    protected void reset() throws RepositoryException
    {
        m_nodeReferences.clear();
    }

    public void remove( NodeImpl node ) throws RepositoryException
    {
        if( node.getDepth() == 0 ) throw new RepositoryException("Root cannot be removed.");

        m_nodeReferences.remove( node.getPath() );
        
        NodeImpl parent = (NodeImpl)node.getParent();
        pullUpNode( parent );
        parent.removeChildNode(node);        
    }
    
    /**
     *  This method pulls up a Node from the repository
     *  and makes sure there's a copy in our local cache.
     *  
     *  @param node
     */
    private void pullUpNode( NodeImpl node ) throws RepositoryException
    {
        m_nodeReferences.put( node.getPath(), node );
    }
    
    /*
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
                String indexInt = n.substring(s+1,n.length()-2);
                index = Integer.parseInt( indexInt ) - 1;
            }
            
            m_path  = new Path(basePath);
            m_index = index;
        }
    }
    */
    
    public NodeImpl getRootNode()
    {
        NodeImpl root = m_nodeReferences.get("/");
        
        return root;
    }

    // FIXME: Really, really slow
    public Node getNodeByUUID(String uuid) throws RepositoryException, ItemNotFoundException
    {
        for( Node nd : m_nodeReferences.values() )
        {
            try
            {
                if( uuid.equals(nd.getUUID()) )
                    return nd;
            }
            catch (UnsupportedRepositoryOperationException e)
            {
            }
        }
        
        throw new ItemNotFoundException( "No match for UUID "+uuid );
    }

    /**
     *  Returns a copy of the current node listing.
     *  
     *  @return
     */
    public List<NodeImpl> allNodes()
    {
        ArrayList<NodeImpl> ls = new ArrayList<NodeImpl>();
        
        ls.addAll( m_nodeReferences.values() );
        
        return ls;
    }
    

}
