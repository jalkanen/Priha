package org.priha.nodetype;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.xml.namespace.QName;

import org.priha.core.namespace.NamespaceMapper;

/**
 *  QNodeType provides non-Session -specific things of NodeTypes.
 */
public class QNodeType
{
    protected QNodeType[]           m_parents = new QNodeType[0];
    protected QPropertyDefinition[] m_propertyDefinitions;
    protected QPropertyDefinition[] m_declaredPropertyDefinitions;
    protected QNodeDefinition[]     m_childNodeDefinitions;

    protected boolean               m_ismixin;
    protected boolean               m_hasOrderableChildNodes;

    private QName                   m_name;
    QName                   m_primaryItemName = null;

    private QNodeType()
    {
    }
    
    public QNodeType( QName name )
    {
        m_name = name;
    }

    public QName getQName()
    {
        return m_name;
    }
    
    public QName getPrimaryItemQName()
    {
        return m_primaryItemName;
    }
    
    public boolean canAddChildNode(QName childNodeName)
    {
        QNodeDefinition nd = null;
        nd = findNodeDefinition( childNodeName );
        
        if( nd == null ) return false;
        
        return true;
    }

    public boolean canRemoveItem(QName itemName)
    {
        QNodeDefinition nd;
        nd = findNodeDefinition(itemName);
        
        if( nd != null )
        {
            if( nd.isMandatory() || nd.isProtected() ) return false;
        }
        
        QPropertyDefinition pd = findPropertyDefinition(itemName, false);
        
        if( pd != null )
        {
            if( pd.isMandatory() || pd.isProtected() ) return false;
        }
        
        return true;
    }
    

    public boolean canSetProperty(QName propertyName, Value value)
    {
        QPropertyDefinition p = findPropertyDefinition(propertyName, false);
        
        if( p == null ) return false;
        
        if( p.isProtected() ) return false;
        
        return true;
    }

    public boolean canSetProperty(QName propertyName, Value[] values)
    {
        QPropertyDefinition p = findPropertyDefinition(propertyName, false);
        
        if( p == null ) return false;
        
        if( p.isProtected() ) return false;
        
        return true;
    }
    
    /**
     *  Find a NodeDefinition from the children of this NodeType.
     *  It will also check the generic types (marked with "*").
     *  
     *  @param name
     *  @return
     */
    public QNodeDefinition findNodeDefinition( QName name )
    {
        for( QNodeDefinition nd : m_childNodeDefinitions )
        {
            if( nd.getQName().equals(name) )
            {
                return nd;
            }
        }

        for( QNodeDefinition nd : m_childNodeDefinitions )
        {
            if( nd.getQName().toString().equals("*") )
            {
                return nd;
            }
        }
        
        for( QNodeType nt : m_parents )
        {
            QNodeDefinition nd = nt.findNodeDefinition(name);
            
            if( nd != null ) return nd;
        }
        
        return null;
    }

    /**
     *  Finds a property definition for a child property.  If the child property definition
     *  for this node has a generic type ("*"), then that will be found as a last resort.
     *  
     *  @param name The name of the property to look for.
     *  @param multiple If true, checks only multi properties; if false, checks only single properties.
     *  @return A valid PropertyDefinition, or null, if no such beast can be located.
     */
    public QPropertyDefinition findPropertyDefinition( QName name, boolean multiple )
    {
        for( QPropertyDefinition pd : m_propertyDefinitions )
        {
            if( pd.getQName().equals(name) && pd.isMultiple() == multiple )
            {
                return pd;
            }
        }

        //
        //  Attempt to find the default.
        //
        for( QPropertyDefinition pd : m_propertyDefinitions )
        {
            if( pd.getQName().toString().equals("*") && pd.isMultiple() == multiple )
            {
                return pd;
            }
        }

        return null;
    }

    public boolean isNodeType( QName qn )
    {
        
        if( m_name.equals(qn) ) return true;

        for( int i = 0; i < m_parents.length; i++ )
        {
            if( m_parents[i].isNodeType(qn) ) return true;
        }

        return false;
    }
  

    public QPropertyDefinition[] getQPropertyDefinitions()
    {
        return m_propertyDefinitions;
    }
    
    public String toString()
    {
        return "QNodeType["+m_name+"]";
    }
    
    /**
     *  The session-specific parts of the node type.
     *  
     */
    public class Impl implements NodeType
    {
        private   NamespaceMapper      m_mapper;
    
        public Impl(NamespaceMapper mapper)
        {
            m_mapper    = mapper;
        }

        public boolean canAddChildNode(String childNodeName)
        {
            try
            {
                return QNodeType.this.canAddChildNode( m_mapper.toQName( childNodeName ) );
            }
            catch( RepositoryException e )
            {
                // FIXME: log
            }

            return false;
        }

        public boolean canAddChildNode(String childNodeName, String nodeTypeName)
        {
            // FIXME: not entirely accurate
            return canAddChildNode(childNodeName);
        }

        public boolean canRemoveItem(String itemName)
        {
            try
            {
                return QNodeType.this.canRemoveItem( m_mapper.toQName( itemName ) );
            }
            catch( RepositoryException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return false;
        }

        public boolean canSetProperty(String propertyName, Value value)
        {
            QName qn;
            try
            {
                qn = m_mapper.toQName( propertyName );
            }
            catch( RepositoryException e )
            {
                return false; // FIXME: LOG
            }
            
            return QNodeType.this.canSetProperty(qn, value);
        }

        public boolean canSetProperty(String propertyName, Value[] values)
        {
            QName qn;
            try
            {
                qn = m_mapper.toQName( propertyName );
            }
            catch( RepositoryException e )
            {
                return false; // FIXME: LOG
            }
            return QNodeType.this.canSetProperty(qn, values);
        }

        public NodeDefinition[] getChildNodeDefinitions()
        {
            NodeDefinition[] defs = new NodeDefinition[m_childNodeDefinitions.length];
            
            for( int i = 0; i < m_childNodeDefinitions.length; i++ )
            {
                defs[i] = m_childNodeDefinitions[i].new Impl(m_mapper);
            }
            
            return defs;
        }

        public NodeDefinition[] getDeclaredChildNodeDefinitions()
        {
            NodeDefinition[] defs = new NodeDefinition[m_childNodeDefinitions.length];
            
            for( int i = 0; i < m_childNodeDefinitions.length; i++ )
            {
                defs[i] = m_childNodeDefinitions[i].new Impl(m_mapper);
            }
            
            return defs;
        }

        public PropertyDefinition[] getDeclaredPropertyDefinitions()
        {
            PropertyDefinition[] defs = new PropertyDefinition[m_declaredPropertyDefinitions.length];
            
            for( int i = 0; i < m_declaredPropertyDefinitions.length; i++ )
            {
                defs[i] = m_declaredPropertyDefinitions[i].new Impl(m_mapper);
            }
            
            return defs;
        }

        public NodeType[] getDeclaredSupertypes()
        {
            return getSupertypes();
        }

        public String getName()
        {
            try
            {
                return m_mapper.fromQName( m_name );
            }
            catch( NamespaceException e )
            {
                // FIXME
            }
            return null;
        }

        public String getPrimaryItemName()
        {
            try
            {
                return m_mapper.fromQName(m_primaryItemName);
            }
            catch( NamespaceException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }

        public PropertyDefinition[] getPropertyDefinitions()
        {
            PropertyDefinition[] defs = new PropertyDefinition[m_propertyDefinitions.length];
            
            for( int i = 0; i < m_propertyDefinitions.length; i++ )
            {
                defs[i] = m_propertyDefinitions[i].new Impl(m_mapper);
            }
            
            return defs;
        }

        public NodeType[] getSupertypes()
        {
            NodeType[] nts = new NodeType[m_parents.length];
            
            for( int i = 0; i < m_parents.length; i++ )
            {
                nts[i] = m_parents[i].new Impl(m_mapper);
            }
            
            return nts;
        }

        public boolean isNodeType(String nodeTypeName)
        {
            QName qn;
            try
            {
                qn = m_mapper.toQName( nodeTypeName );
            }
            catch( RepositoryException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return false;
            }
            
            return QNodeType.this.isNodeType(qn);
        }


        public String toString()
        {
            return "NodeType: "+m_name;
        }

        public boolean hasOrderableChildNodes()
        {
            return m_hasOrderableChildNodes;
        }

        public boolean isMixin()
        {
            return m_ismixin;
        }
      
        public NodeDefinition findNodeDefinition(String string) throws RepositoryException
        {
            QNodeDefinition qnd = QNodeType.this.findNodeDefinition( m_mapper.toQName(string) );
            
            return qnd.new Impl(m_mapper);
        }

        /**
         *  Returns a reference to the parent QNodeType.
         *  @return
         */
        public QNodeType getQNodeType()
        {
            return QNodeType.this;
        }
    }
}
