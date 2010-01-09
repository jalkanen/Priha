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
package org.priha.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

import org.priha.core.values.StreamValueImpl;
import org.priha.core.values.StringValueImpl;
import org.priha.core.values.ValueImpl;
import org.priha.nodetype.QNodeType;
import org.priha.nodetype.QPropertyDefinition;
import org.priha.path.Path;
import org.priha.providers.ValueContainer;

public class PropertyImpl extends ItemImpl implements Property, Comparable<PropertyImpl>
{
    private enum Multi { UNDEFINED, SINGLE, MULTI }

    private ValueContainer     m_value = ValueContainer.UNDEFINED_CONTAINER;
    private Multi              m_multi = Multi.UNDEFINED;
    PropertyDefinition         m_definition;
    private boolean            m_transient = false;
    
    public PropertyImpl( SessionImpl session, Path path, QPropertyDefinition propDef )
    {
        super( session, path );

        if( propDef != null ) 
        {
            setDefinition( propDef.new Impl(session) );
//            m_type = propDef.getRequiredType();
        }
    }

    public boolean getBoolean() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.SINGLE )
            throw new ValueFormatException("Attempted to get a SINGLE boolean value from a MULTI property "+m_path);

        return getValue().getBoolean();
    }

    public Calendar getDate() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.SINGLE )
            throw new ValueFormatException("Attempted to get a SINGLE date value from a MULTI property "+m_path);

        return getValue().getDate();
    }

    public PropertyDefinition getDefinition() throws RepositoryException
    {
        return m_definition;
    }

    public double getDouble() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.SINGLE )
            throw new ValueFormatException("Attempted to get a SINGLE double value from a MULTI property "+m_path);

        return getValue().getDouble();
    }

    public long getLength() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.SINGLE )
            throw new ValueFormatException("Attempted to get a SINGLE length value from a MULTI property "+m_path);

        return getLength( m_value.getValue() );
    }

    /**
     *  Returns the length of a single Value.
     * @param v
     * @return
     * @throws ValueFormatException
     * @throws IllegalStateException
     * @throws RepositoryException
     */
    private long getLength( Value v ) throws ValueFormatException, IllegalStateException, RepositoryException
    {
        if( v.getType() == PropertyType.BINARY )
        {
            try
            {
                return ((StreamValueImpl)v).getLength();
            }
            catch (IOException e)
            {
                throw new RepositoryException("Unable to get length "+e.getMessage());
            }
        }
        
        return v.getString().length();
    }

    public long[] getLengths() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.MULTI ) throw new ValueFormatException();

        long[] lengths = new long[getValues().length];

        for( int i = 0; i < getValues().length; i++ )
        {
            lengths[i] = getLength( getValues()[i] );
        }

        return lengths;
    }

    public long getLong() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.SINGLE ) throw new ValueFormatException();

        return getValue().getLong();
    }

    public NodeImpl getNode() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.SINGLE )
            throw new ValueFormatException("Attempted to get a SINGLE reference value from a MULTI property "+m_path);

        if( getValue().getType() != PropertyType.REFERENCE ) throw new ValueFormatException();

        String uuid = getValue().getString();

        NodeImpl nd = m_session.getNodeByUUID( uuid );

        return nd;
    }

    public InputStream getStream() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.SINGLE ) throw new ValueFormatException();

        return getValue().getStream();
    }

    public String getString() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.SINGLE )
            throw new ValueFormatException("Attempted to get a SINGLE string value from a MULTI property "+m_path);

        return getValue().getString();
    }

    public int getType() throws RepositoryException
    {
        return m_value.getType();
    }

    public ValueImpl getValue() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.SINGLE )
            throw new ValueFormatException("Attempted to get a SINGLE Value object from a MULTI property "+m_path);

        if( m_value == null ) 
            throw new RepositoryException("Internal error: Value is null for "+m_path);

        //
        //  Clones the value as per the Javadoc
        //
        ValueContainer vc = m_value.deepClone(getSession());
        
        return vc.getValue();
    }

    public Value[] getValues() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.MULTI )
            throw new ValueFormatException("Attempted to get a MULTI Value object from a SINGLE property "+m_path);
        
        return m_session.getValueFactory().cloneValues(m_value.getValues());
    }

    /**
     *  Differs from setValue(), as it does not mark the item modified.
     *  
     * @param value
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    public void loadValue( ValueImpl value ) throws VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        if( m_multi == Multi.MULTI )
            throw new ValueFormatException("Attempted to set a SINGLE Value object to a MULTI property "+getPath());

        if( value == null )
        {
            remove();
            return;
        }

        m_value = new ValueContainer(value);
        m_multi = Multi.SINGLE;
    }

    public void setValue(Value value)
                                     throws ValueFormatException,
                                         VersionException,
                                         LockException,
                                         ConstraintViolationException,
                                         RepositoryException
    {
        QNodeType parentType = getParent().getPrimaryQNodeType();
        
        if( !m_session.isSuper() )
        {
            if( !parentType.canSetProperty( getQName(), value ) )
                throw new ConstraintViolationException("Setting of this property is forbidden");
            
            if( !getParent().isCheckedOut() )
                throw new VersionException("Parent node is not checked out");
            
            if( getParent().isLockedWithoutToken() )
                throw new LockException("Parent node is locked");
        }

        if( getType() != PropertyType.UNDEFINED && value != null && getType() != value.getType() )
        {
            throw new ValueFormatException("Attempt to set a different type value to this property");
        }

        loadValue( (ValueImpl)value );
        
        if( !isNew() ) 
        {
            enterState( ItemState.UPDATED );
        }
        else 
        {
            enterState( ItemState.NEW );
            getParent().enterState( ItemState.UPDATED );
        }
        
    }

    public void setValue(Value[] values) 
       throws ValueFormatException, 
              VersionException, 
              LockException, 
              ConstraintViolationException, 
              RepositoryException
    {
//        if( getType() == PropertyType.UNDEFINED )
//            m_type = propertyType;
        int propertyType = getType();
        
        if( propertyType == PropertyType.UNDEFINED )
        {
            if( values.length > 0 && values[0] != null )
            {
                int type = values[0].getType();
                
                propertyType = type;
            }
            else
            {
                //
                // This is the difficult bit.  The system could not figure out an explicit
                // node type for this Property (based on the parent node), and the
                // values array is empty, so we can't use that one either.  So we do the
                // only thing we can - assume something.
                //
                
                propertyType = PropertyType.STRING;
                //throw new ValueFormatException("Cannot add an empty Value array when there is no explicit type defined in the parent Node."+getInternalPath());
            }
        }
        
        if( propertyType != PropertyType.UNDEFINED && values != null && values.length >= 1 && values[0] != null && propertyType != values[0].getType() )
        {
            throw new ValueFormatException("Attempt to set a different type value to this property");
        }        
        
        setValue( values, propertyType );   
    }
    
    public void setValue(Value[] values, int propertyType)
                                        throws ValueFormatException,
                                            VersionException,
                                            LockException,
                                            ConstraintViolationException,
                                            RepositoryException
    {
        QNodeType parentType = getParent().getPrimaryQNodeType();
        
        if( !m_session.isSuper() )
        {
            if( !parentType.canSetProperty( getQName(), values ) )
                throw new ConstraintViolationException("Setting of this property is forbidden:");
            
            if( !getParent().isCheckedOut() )
                throw new VersionException("Parent node is not checked out");

            if( getParent().isLockedWithoutToken() )
                throw new LockException("Parent node is locked");
        }
        
        if( values == null )
        {
            remove();
            return;
        }

        values = compactValueArray( values );

        loadValue(values, propertyType);

        if( m_value != null )
        {
            if( isNew() )
            {
                // The Property has just been added.
                getParent().enterState( ItemState.UPDATED );
            }
            
            enterState( ItemState.UPDATED );
        }
        else
        {
            getParent().enterState( ItemState.UPDATED );
        }
    }

    /**
     *  Differs from setValue() in the sense that it does not mark it modified.
     *  
     * @param values
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    public void loadValue(Value[] values, int propertyType)
                                          throws ValueFormatException,
                                              VersionException,
                                              LockException,
                                              ConstraintViolationException,
                                              RepositoryException
    {
        if( m_multi == Multi.SINGLE )
            throw new ValueFormatException("Attempted to set a MULTI Value object to a SINGLE property "+m_path);

        /*
        if( propertyType == PropertyType.UNDEFINED )
            throw new ValueFormatException("Cannot load an UNDEFINED value");
        */
        if( values == null )
        {
            remove();
            return;
        }
        m_value = new ValueContainer(values, propertyType);
       
        m_multi = Multi.MULTI;
    }

    /**
     *  Compacts away the null values of the array.  It also ensures that all
     *  the Values in the array are of the same type.
     *  
     *  @param values
     *  @return
     *  @throws ValueFormatException
     */
    private Value[] compactValueArray(Value[] values) throws ValueFormatException
    {
        ArrayList<Value> ls = new ArrayList<Value>();

        int type = PropertyType.UNDEFINED;
        for( int i = 0; i < values.length; i++ )
        {
            if( values[i] != null )
            {
                if( type == PropertyType.UNDEFINED )
                    type = values[i].getType();
                else if( values[i].getType() != type )
                    throw new ValueFormatException("All Values must be of the same type!");

                ls.add( values[i] );
            }
        }
        return ls.toArray( new Value[ls.size()] );
    }
    
    public void setValue(String value)
                                      throws ValueFormatException,
                                          VersionException,
                                          LockException,
                                          ConstraintViolationException,
                                          RepositoryException
    {
        if( value == null )
        {
            remove();
            return;
        }
        setValue( m_session.getValueFactory().createValue(value, getType() == PropertyType.UNDEFINED ? PropertyType.STRING : getType() ) );
    }

    public void setValue(String[] values)
                                         throws ValueFormatException,
                                             VersionException,
                                             LockException,
                                             ConstraintViolationException,
                                             RepositoryException
    {
        QNodeType parentType = getParent().getPrimaryQNodeType();

        if( !m_session.isSuper() )
        {
            if( !parentType.canSetProperty( getQName(), new StringValueImpl("") ) )
                throw new ConstraintViolationException("Setting of this property is forbidden:");
            
            if( !getParent().isCheckedOut() )
                throw new VersionException("Parent node is not checked out");
        }

        if( values == null )
        {
            remove();
            return;
        }

        if( getType() != PropertyType.UNDEFINED && getType() != PropertyType.STRING )
        {
            throw new ValueFormatException("Attempt to set a different type value to this property");
        }  
        
        ArrayList<Value> ls = new ArrayList<Value>();
        for( int i = 0; i < values.length; i++ )
        {
            if( values[i] != null )
                ls.add( m_session.getValueFactory().createValue( values[i] ));
        }
        setValue( ls.toArray( new Value[ls.size()] ), PropertyType.STRING );
    }

    public void setValue(InputStream value)
                                           throws ValueFormatException,
                                               VersionException,
                                               LockException,
                                               ConstraintViolationException,
                                               RepositoryException
    {
        if( value == null )
        {
            remove();
            return;
        }

        setValue( m_session.getValueFactory().createValue(value, getType() == PropertyType.UNDEFINED ? PropertyType.BINARY : getType() ) );
    }

    public void setValue(long value)
                                    throws ValueFormatException,
                                        VersionException,
                                        LockException,
                                        ConstraintViolationException,
                                        RepositoryException
    {
        setValue( m_session.getValueFactory().createValue(value) );
    }

    public void setValue(double value)
                                      throws ValueFormatException,
                                          VersionException,
                                          LockException,
                                          ConstraintViolationException,
                                          RepositoryException
    {
        setValue( m_session.getValueFactory().createValue(value) );
    }

    public void setValue(Calendar value)
                                        throws ValueFormatException,
                                            VersionException,
                                            LockException,
                                            ConstraintViolationException,
                                            RepositoryException
    {
        if( value == null )
        {
            remove();
        }
        setValue( m_session.getValueFactory().createValue(value) );
    }

    public void setValue(boolean value)
                                       throws ValueFormatException,
                                           VersionException,
                                           LockException,
                                           ConstraintViolationException,
                                           RepositoryException
    {
        setValue( m_session.getValueFactory().createValue(value, getType() == PropertyType.UNDEFINED ? PropertyType.BOOLEAN : getType() ) );
    }

    public void setValue(Node value)
                                    throws ValueFormatException,
                                        VersionException,
                                        LockException,
                                        ConstraintViolationException,
                                        RepositoryException
    {
        if( value == null )
        {
            remove();
        }
        else
        {
            try
            {
                setValue( m_session.getValueFactory().createValue(value.getUUID(), PropertyType.REFERENCE) );
            }
            catch( UnsupportedRepositoryOperationException e )
            {
                throw new ValueFormatException("Node is not referenceable");
            }
        }
    }

    public void setValue( String value, int type ) throws ValueFormatException,
        VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        if( value == null )
        {
            remove();
        }
        else
        {
            setValue( m_session.getValueFactory().createValue(value,type) );
        }
    }

    public void save() throws AccessDeniedException,
                              ItemExistsException,
                              ConstraintViolationException,
                              InvalidItemStateException,
                              ReferentialIntegrityException,
                              VersionException,
                              LockException,
                              NoSuchNodeTypeException,
                              RepositoryException
    {
        Node parent = getParent();

        parent.save();
    }

    public String toString()
    {
        String path;
        
        try
        {
            path = getInternalPath().toString(m_session);
        }
        catch( Exception e )
        {
            path = getInternalPath().toString();
        }
        
        try
        {
            return "Property("+m_multi+")["+path+"="+((m_multi == Multi.SINGLE && m_value != null) ? getValue().toString() : (PropertyType.nameFromValue(getType())+"["+getValues().length+"]"))+"]";
        }
        catch (RepositoryException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "Error?";
    }

    public void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        //
        //  Sanity check - the primary type cannot be deleted unless this is a super session.
        //
        if( !getSession().isSuper() && 
            getName().equals("jcr:primaryType") && 
            getInternalPath().getParentPath().isRoot() ) return;
        		
        NodeImpl nd = getParent();

        nd.removeProperty(this);
        
        enterState( ItemState.REMOVED );
    }

    public void setDefinition(PropertyDefinition pd)
    {
        m_definition = pd;

        if( m_definition != null )
        {
            m_multi = m_definition.isMultiple() ? Multi.MULTI : Multi.SINGLE;
        }
    }

    /**
     *  A PropertyImpl is equal to another PropertyImpl if
     *  <ul>
     *   <li>Paths are equal</li>
     *   <li>Definitions are equal</li>
     *   <li>All values are equal (in case of a multi-valued object)</li>
     *  </ul>
     */
    @Override
    public boolean equals( Object obj )
    {
        if( !(obj instanceof PropertyImpl) ) return false;
        
        if( obj == this ) return true;
        
        PropertyImpl pi = (PropertyImpl) obj;
        
        if( !pi.m_path.equals(m_path) ) return false;
        
        try
        {
            if( m_multi == Multi.SINGLE && pi.m_multi == Multi.SINGLE )
            {
                return getValue().equals(pi.getValue());
            }
            else if( m_multi == Multi.MULTI && pi.m_multi == Multi.MULTI )
            {
                if( getValues().length == pi.getValues().length )
                {
                    for( int i = 0; i < getValues().length; i++ )
                    {
                        if( !getValues()[i].equals(pi.getValues()[i]) ) return false;
                    }
                    return true;
                }
            }
        }
        catch( RepositoryException e )
        {
            // TODO: WHat to do here?
        }
        
        return false;
    }

    // FIXME: Is not consistent with equals.
    public int compareTo(PropertyImpl o)
    {
        int res = m_path.toString().compareTo(o.m_path.toString());
        
        if( res == 0 )
        {
            res = m_session.getWorkspace().getName().compareTo( o.m_session.getWorkspace().getName() );
        }
        
        return res;
    }

    /**
     *  Transient properties are never saved - they just live within the Session.
     *  
     *  @param b True, if you want to turn this property into a transient property.
     */
    public void setTransient( boolean b )
    {
        m_transient = true;
    }
    
    /**
     *  Returns true, if this property is transient.
     *  
     *  @return True or false.
     */
    public boolean isTransient()
    {
        return m_transient;
    }
    
    @Override
    protected void preSave() throws RepositoryException
    {
        super.preSave();
        
        if( getType() == PropertyType.UNDEFINED && !m_definition.isMultiple() ) 
            throw new ConstraintViolationException("Property must not be of type UNDEFINED, unless it's a multiproperty: "+getInternalPath());
    }

    public ValueContainer getValueContainer()
    {
        return m_value;
    }


}
