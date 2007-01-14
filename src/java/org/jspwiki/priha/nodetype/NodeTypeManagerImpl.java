package org.jspwiki.priha.nodetype;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

public class NodeTypeManagerImpl implements NodeTypeManager
{
    private Workspace m_workspace;
    private SortedMap<String,NodeType> m_primaryTypes = new TreeMap<String,NodeType>();
    private SortedMap<String,NodeType> m_mixinTypes   = new TreeMap<String,NodeType>();
    
    private static final NodeType BASE_NODETYPE = new BaseNodeType();
    
    public NodeTypeManagerImpl(Workspace ws)
    {
        m_workspace = ws;
        
        addPrimaryNodeType( BASE_NODETYPE ); // Is always available
    }
    
    // FIXME: Should really return only a singleton per Repository.  Now, clashes are possible,
    //        when multiple sessions are opened.
    public static NodeTypeManagerImpl getInstance( Workspace ws )
    {
        return new NodeTypeManagerImpl( ws );
    }
    
    public void addPrimaryNodeType( NodeType nt )
    {
        m_primaryTypes.put( nt.getName(), nt );
    }
    
    public NodeTypeIterator getAllNodeTypes() throws RepositoryException
    {
        List<NodeType> ls = new ArrayList<NodeType>();
        
        ls.addAll( m_primaryTypes.values() );
        ls.addAll( m_mixinTypes.values() );
        
        return new NodeTypeIteratorImpl(ls);
    }

    public NodeTypeIterator getMixinNodeTypes() throws RepositoryException
    {
        List<NodeType> ls = new ArrayList<NodeType>();

        ls.addAll( m_mixinTypes.values() );
        
        return new NodeTypeIteratorImpl(ls);
    }

    public NodeType getNodeType(String nodeTypeName) throws NoSuchNodeTypeException, RepositoryException
    {
        NodeType n = m_primaryTypes.get(nodeTypeName);
        if( n == null )
        {
            n = m_mixinTypes.get(nodeTypeName);
        }
        
        if( n == null ) throw new NoSuchNodeTypeException("No such node type: "+nodeTypeName);
        
        return n;
    }

    public NodeTypeIterator getPrimaryNodeTypes() throws RepositoryException
    {
        List<NodeType> ls = new ArrayList<NodeType>();

        ls.addAll( m_primaryTypes.values() );
        
        return new NodeTypeIteratorImpl(ls);
    }

}
