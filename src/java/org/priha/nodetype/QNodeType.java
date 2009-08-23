package org.priha.nodetype;

import java.util.ArrayList;
import java.util.Arrays;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.priha.core.SessionImpl;
import org.priha.core.values.ValueFactoryImpl;
import org.priha.core.values.ValueImpl;
import org.priha.util.QName;

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
        return canAddChildNode(childNodeName, null);
    }
    
    /**
     *  Determines whether a child Node can be added of a predeterminate
     *  type.
     *  
     *  @param childNodeName Name of the new child.
     *  @param type Type to check.
     *  @return True, if this node type allows adding this child of this type.
     */
    public boolean canAddChildNode(QName childNodeName,QName type)
    {
        QNodeDefinition nd = null;
        nd = findNodeDefinition( childNodeName );
        
        //System.out.println(this+", "+childNodeName+" = "+type);
        // If no node definition, means that there's no child type.
        if( nd == null ) return false;
        
        if( nd.getDefaultPrimaryType() == null && type == null ) return false;

        if( type != null )
        {
            try
            {
                QNodeType t = QNodeTypeManager.getInstance().getNodeType(type);
                for( QNodeType reqdType : nd.m_requiredPrimaryTypes )
                {
                    if( t.isNodeType(reqdType.getQName()) )
                        return true;
                }
            }
            catch( RepositoryException e )
            {
                return false;
            }
        }
             
        if( nd.getDefaultPrimaryType() != null && type == null ) return true;
        
        return false;
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
        if( pd == null ) pd = findPropertyDefinition(itemName,true);
        
        if( pd != null )
        {
            if( pd.isMandatory() || pd.isProtected() ) return false;
        }
        
        return true;
    }
    

    public boolean canSetProperty(QName propertyName, Value value)
    {
        if( value == null ) return canRemoveItem( propertyName );
        
        QPropertyDefinition p = findPropertyDefinition(propertyName, false);
        
        if( p == null ) return false;
        
        if( p.isProtected() ) return false;
        
        if( p.m_requiredType != PropertyType.UNDEFINED && p.m_requiredType != value.getType() )
        {
            // Different types, so let's see if it can be converted.
            // FIXME: This can be slow with large properties.
            try
            {
                return ValueFactoryImpl.canConvert( (ValueImpl)value, p.m_requiredType );
            }
            catch( Exception e ) 
            {
                // Obviously not since it can't even be read.
                return false; 
            }
        }
        
        return true;
    }

    public boolean canSetProperty(QName propertyName, Value[] values)
    {
        if( values == null ) return canRemoveItem( propertyName );

        QPropertyDefinition p = findPropertyDefinition(propertyName, true);
        
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
        private   SessionImpl      m_mapper;
    
        public Impl(SessionImpl mapper)
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
            try
            {
                return QNodeType.this.canAddChildNode( m_mapper.toQName( childNodeName ), 
                                                       m_mapper.toQName( nodeTypeName ) );
            }
            catch( RepositoryException e )
            {
                // FIXME: log
            }

            return false;
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
            ArrayList<NodeType> nts = new ArrayList<NodeType>();
            
            for( int i = 0; i < m_parents.length; i++ )
            {
                NodeType nt = m_parents[i].new Impl(m_mapper);
                
                nts.add( nt );
            }
                        
            return nts.toArray( new NodeType[0] );
        }

        public String getName()
        {
            return m_mapper.fromQName( m_name );
        }

        public String getPrimaryItemName()
        {
            return m_mapper.fromQName(m_primaryItemName);
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
            ArrayList<NodeType> nts = new ArrayList<NodeType>();
            
            for( int i = 0; i < m_parents.length; i++ )
            {
                NodeType nt = m_parents[i].new Impl(m_mapper);
                
                nts.add( nt );
                nts.addAll( Arrays.asList( nt.getSupertypes() ) );
            }
                        
            return nts.toArray( new NodeType[0] );
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
