package org.jspwiki.priha.core;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

import org.jspwiki.priha.core.values.ValueFactoryImpl;
import org.jspwiki.priha.core.values.ValueImpl;
import org.jspwiki.priha.nodetype.PropertyDefinitionImpl;
import org.jspwiki.priha.util.Path;

public class PropertyImpl extends ItemImpl implements Property, Comparable<PropertyImpl>
{
    private enum Multi { UNDEFINED, SINGLE, MULTI };

    private Value[]            m_value;
    private Multi              m_multi = Multi.UNDEFINED;
    PropertyDefinition         m_definition;

    public PropertyImpl( SessionImpl session, Path path, PropertyDefinition propDef )
    {
        super( session, path );

        setDefinition( propDef );
    }

    /**
     *  Creates a  deep clone of a PropertyImpl, using the given session.
     *  
     * @param pi
     * @param session
     * @throws ValueFormatException
     * @throws IllegalStateException
     * @throws RepositoryException
     */
    public PropertyImpl(PropertyImpl pi, SessionImpl session) throws ValueFormatException, IllegalStateException, RepositoryException
    {
        super( pi, session );
        
        m_value = ValueFactoryImpl.getInstance().cloneValues( pi.m_value );
        m_multi = pi.m_multi;
        m_definition = pi.m_definition;
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

        return getLength( m_value[0] );
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
            return -1; // FIXME: Not yet supported

        return getValue().getString().length();
    }

    public long[] getLengths() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.MULTI ) throw new ValueFormatException();

        long[] lengths = new long[m_value.length];

        for( int i = 0; i < m_value.length; i++ )
        {
            lengths[i] = getLength( m_value[i] );
        }

        return lengths;
    }

    public long getLong() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.SINGLE ) throw new ValueFormatException();

        return getValue().getLong();
    }

    public Node getNode() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.SINGLE )
            throw new ValueFormatException("Attempted to get a SINGLE reference value from a MULTI property "+m_path);

        if( getValue().getType() != PropertyType.REFERENCE ) throw new ValueFormatException();

        String uuid = getValue().getString();

        Node nd = (Node)m_session.getNodeByUUID( uuid );

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
        return m_value[0].getType();
    }

    public Value getValue() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.SINGLE )
            throw new ValueFormatException("Attempted to get a SINGLE Value object from a MULTI property "+m_path);

        //
        //  Clones the value as per the Javadoc
        //
        return ValueFactoryImpl.getInstance().createValue( m_value[0] );
    }

    public Value[] getValues() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.MULTI )
            throw new ValueFormatException("Attempted to get a MULTI Value object from a SINGLE property "+m_path);

        return ValueFactoryImpl.getInstance().cloneValues(m_value);
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
    public void loadValue( Value value ) throws VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        if( m_multi == Multi.MULTI )
            throw new ValueFormatException("Attempted to set a SINGLE Value object to a MULTI property "+m_path);

        if( value == null )
        {
            remove();
        }

        m_value = new Value[1];

        m_value[0] = value;
        m_multi = Multi.SINGLE;
    }

    public void setValue(Value value)
                                     throws ValueFormatException,
                                         VersionException,
                                         LockException,
                                         ConstraintViolationException,
                                         RepositoryException
    {
        loadValue( value );
        markModified();
    }

    public void setValue(Value[] values)
                                        throws ValueFormatException,
                                            VersionException,
                                            LockException,
                                            ConstraintViolationException,
                                            RepositoryException
    {
        loadValue(values);
        
        markModified();
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
    public void loadValue(Value[] values)
                                          throws ValueFormatException,
                                              VersionException,
                                              LockException,
                                              ConstraintViolationException,
                                              RepositoryException
    {
        if( m_multi == Multi.SINGLE )
            throw new ValueFormatException("Attempted to set a MULTI Value object to a SINGLE property "+m_path);

        if( values == null || values.length == 0 )
        {
            remove();
        }
        
        // Clean away null values from the array
        
        ArrayList<Value> ls = new ArrayList<Value>();
        for( int i = 0; i < values.length; i++ )
        {
            if( values[i] != null )
                ls.add(ValueFactoryImpl.getInstance().createValue( values[i] ));
        }
        m_value = ls.toArray( new Value[ls.size()] );

        m_multi = Multi.MULTI;
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
        }
        setValue( ValueFactoryImpl.getInstance().createValue(value) );
    }

    public void setValue(String[] values)
                                         throws ValueFormatException,
                                             VersionException,
                                             LockException,
                                             ConstraintViolationException,
                                             RepositoryException
    {
        if( values == null || values.length == 0)
        {
            remove();
        }

        ArrayList<Value> ls = new ArrayList<Value>();
        for( int i = 0; i < values.length; i++ )
        {
            if( values[i] != null )
                ls.add(ValueFactoryImpl.getInstance().createValue( values[i] ));
        }
        setValue( ls.toArray( new Value[ls.size()] ) );
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
        }

        setValue( ValueFactoryImpl.getInstance().createValue(value) );
    }

    public void setValue(long value)
                                    throws ValueFormatException,
                                        VersionException,
                                        LockException,
                                        ConstraintViolationException,
                                        RepositoryException
    {
        setValue( ValueFactoryImpl.getInstance().createValue(value) );
    }

    public void setValue(double value)
                                      throws ValueFormatException,
                                          VersionException,
                                          LockException,
                                          ConstraintViolationException,
                                          RepositoryException
    {
        setValue( ValueFactoryImpl.getInstance().createValue(value) );
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
        setValue( ValueFactoryImpl.getInstance().createValue(value) );
    }

    public void setValue(boolean value)
                                       throws ValueFormatException,
                                           VersionException,
                                           LockException,
                                           ConstraintViolationException,
                                           RepositoryException
    {
        setValue( ValueFactoryImpl.getInstance().createValue(value) );
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

        setValue( ValueFactoryImpl.getInstance().createValue(value.getUUID(), PropertyType.REFERENCE) );
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
            setValue( ValueFactoryImpl.getInstance().createValue(value,type) );
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
        return "Property("+m_multi+")["+m_path+","+m_name+"="+((m_multi == Multi.SINGLE ) ? m_value[0].toString() : m_value)+"]";
    }

    public void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        //
        // Sanity check - the primary type cannot be deleted.
        //
        if( getName().equals("jcr:primaryType") ) return;
        		
        NodeImpl nd = (NodeImpl)getParent();

        nd.removeProperty(this);
        
        m_state = ItemState.REMOVED;
        markModified();
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
        
        if( m_value.length != pi.m_value.length ) return false;
        
        for( int i = 0; i < m_value.length; i++ )
        {
            if( !m_value[i].equals(pi.m_value[i]) ) return false;
        }
        
        return true;
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
}
