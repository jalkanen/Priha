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
package org.priha.nodetype;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.xml.namespace.QName;

import org.priha.core.RepositoryImpl;

/**
 *  Stores the Node Types.
 */
public class GenericNodeType
     implements NodeType
{
    protected NodeType[]           m_parents = new NodeType[0];
    protected PropertyDefinition[] m_propertyDefinitions;
    protected PropertyDefinition[] m_declaredPropertyDefinitions;
    protected NodeDefinition[]     m_childNodeDefinitions;

    protected String               m_primaryItemName = null;
    protected String               m_name;

    protected boolean              m_ismixin;
    protected boolean              m_hasOrderableChildNodes;

    //private Logger log = Logger.getLogger(getClass().getName());
    
    public GenericNodeType(String name)
    {
        m_name = name;
    }

    public boolean canAddChildNode(String childNodeName)
    {
        NodeDefinition nd = null;
        try
        {
            nd = findNodeDefinition(RepositoryImpl.getGlobalNamespaceRegistry().toQName( childNodeName ));
        }
        catch( RepositoryException e )
        {
        }
        
        if( nd == null ) return false;
        
        return true;
    }

    public boolean canAddChildNode(String childNodeName, String nodeTypeName)
    {
        // FIXME: not entirely accurate
        return canAddChildNode(childNodeName);
    }

    public boolean canRemoveItem(String itemName)
    {
        NodeDefinition nd;
        try
        {
            nd = findNodeDefinition(RepositoryImpl.getGlobalNamespaceRegistry().toQName(itemName));
        }
        catch( RepositoryException e )
        {
            return false;
        }
        
        if( nd != null )
        {
            if( nd.isMandatory() || nd.isProtected() ) return false;
        }
        
        PropertyDefinition pd = findPropertyDefinition(itemName, false);
        
        if( pd != null )
        {
            if( pd.isMandatory() || pd.isProtected() ) return false;
        }
        
        return true;
    }

    public boolean canSetProperty(String propertyName, Value value)
    {
        PropertyDefinition p = findPropertyDefinition(propertyName, false);
        
        if( p == null ) return false;
        
        if( p.isProtected() ) return false;
        
        return true;
    }

    public boolean canSetProperty(String propertyName, Value[] values)
    {
        PropertyDefinition p = findPropertyDefinition(propertyName, true);
        
        if( p == null ) return false;

        if( p.isProtected() ) return false;

        return true;
    }

    public NodeDefinition[] getChildNodeDefinitions()
    {
        return m_childNodeDefinitions;
    }

    public NodeDefinition[] getDeclaredChildNodeDefinitions()
    {
        return m_childNodeDefinitions;
    }

    public PropertyDefinition[] getDeclaredPropertyDefinitions()
    {
        return m_declaredPropertyDefinitions;
    }

    public NodeType[] getDeclaredSupertypes()
    {
        return getSupertypes();
    }

    public String getName()
    {
        return m_name;
    }

    public String getPrimaryItemName()
    {
        return m_primaryItemName;
    }

    public PropertyDefinition[] getPropertyDefinitions()
    {
        return m_propertyDefinitions;
    }

    public NodeType[] getSupertypes()
    {
        return m_parents;
    }

    public boolean hasOrderableChildNodes()
    {
        return m_hasOrderableChildNodes;
    }

    public boolean isMixin()
    {
        return m_ismixin;
    }

    public boolean isNodeType(String nodeTypeName)
    {
        if( m_name.equals(nodeTypeName) ) return true;

        for( int i = 0; i < m_parents.length; i++ )
        {
            if( m_parents[i].isNodeType(nodeTypeName) ) return true;
        }

        return false;
    }


    public NodeDefinition findNodeDefinition( QName name )
    {
        for( NodeDefinition nd : m_childNodeDefinitions )
        {
            if( nd.getName().equals(name) )
            {
                return nd;
            }
        }

        for( NodeDefinition nd : m_childNodeDefinitions )
        {
            if( nd.getName().equals("*") )
            {
                return nd;
            }
        }
        
        for( NodeType nt : m_parents )
        {
            NodeDefinition nd = ((GenericNodeType)nt).findNodeDefinition(name);
            
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
    public PropertyDefinition findPropertyDefinition( String name, boolean multiple )
    {
        for( PropertyDefinition pd : m_propertyDefinitions )
        {
            if( pd.getName().equals(name) && pd.isMultiple() == multiple )
            {
                return pd;
            }
        }

        //
        //  Attempt to find the default.
        //
        for( PropertyDefinition pd : m_propertyDefinitions )
        {
            if( pd.getName().equals("*") && pd.isMultiple() == multiple )
            {
                return pd;
            }
        }

        return null;
    }

    public String toString()
    {
        return "NodeType: "+m_name;
    }
}
