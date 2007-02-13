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

import org.jspwiki.priha.nodetype.PropertyDefinitionImpl;

public class PropertyImpl extends ItemImpl implements Property
{
    private enum Multi { UNDEFINED, SINGLE, MULTI };

    private Value[]            m_value;
    private Multi              m_multi = Multi.UNDEFINED;
    PropertyDefinition         m_definition;
    
    public PropertyImpl( SessionImpl session, String path, PropertyDefinition propDef )
    {
        super( session, path );
        m_definition = propDef;
    }
    
    public boolean getBoolean() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.SINGLE ) throw new ValueFormatException();
        return getValue().getBoolean();
    }

    public Calendar getDate() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.SINGLE ) throw new ValueFormatException();
        return getValue().getDate();
    }

    public PropertyDefinition getDefinition() throws RepositoryException
    {
        return m_definition;
    }

    public double getDouble() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.SINGLE ) throw new ValueFormatException();

        return getValue().getDouble();
    }

    public long getLength() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.SINGLE ) throw new ValueFormatException();

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
        throw new UnsupportedRepositoryOperationException();
    }

    public InputStream getStream() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.SINGLE ) throw new ValueFormatException();

        return getValue().getStream();       
    }

    public String getString() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.SINGLE ) throw new ValueFormatException();

        return getValue().getString();
    }

    public int getType() throws RepositoryException
    {
        return m_value[0].getType();
    }

    public Value getValue() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.SINGLE ) throw new ValueFormatException();

        //
        //  Clones the value as per the Javadoc
        //
        return new ValueImpl(m_value[0]);
    }

    public Value[] getValues() throws ValueFormatException, RepositoryException
    {
        if( m_multi != Multi.MULTI ) throw new ValueFormatException();
        
        return m_value;
    }

    public void setValue(Value value)
                                     throws ValueFormatException,
                                         VersionException,
                                         LockException,
                                         ConstraintViolationException,
                                         RepositoryException
    {
        if( m_multi == Multi.MULTI ) throw new ValueFormatException();
        
        if( value == null )
        {
            remove();
        }
        
        m_value = new Value[1];
        
        m_value[0] = value;
        m_multi = Multi.SINGLE;
    }

    public void setValue(Value[] values)
                                        throws ValueFormatException,
                                            VersionException,
                                            LockException,
                                            ConstraintViolationException,
                                            RepositoryException
    {
        if( m_multi == Multi.SINGLE ) throw new ValueFormatException();

        if( values == null )
        {
            remove();
        }
        
        m_value = values;
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
        setValue( new ValueImpl(value) );
    }

    public void setValue(String[] values)
                                         throws ValueFormatException,
                                             VersionException,
                                             LockException,
                                             ConstraintViolationException,
                                             RepositoryException
    {
        if( values == null )
        {
            remove();
        }
        
        ArrayList ls = new ArrayList<Value>();
        for( int i = 0; i < values.length; i++ )
        {
            if( m_value[i] == null )
                ls.add(new ValueImpl( values[i] ));
        }
        setValue( (Value[])ls.toArray() );
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
        
        setValue( new ValueImpl(value) );
    }

    public void setValue(long value)
                                    throws ValueFormatException,
                                        VersionException,
                                        LockException,
                                        ConstraintViolationException,
                                        RepositoryException
    {
        setValue( new ValueImpl(value) );
    }

    public void setValue(double value)
                                      throws ValueFormatException,
                                          VersionException,
                                          LockException,
                                          ConstraintViolationException,
                                          RepositoryException
    {
        setValue( new ValueImpl(value) );
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
        setValue( new ValueImpl(value) );
    }

    public void setValue(boolean value)
                                       throws ValueFormatException,
                                           VersionException,
                                           LockException,
                                           ConstraintViolationException,
                                           RepositoryException
    {
        setValue( new ValueImpl(value) );
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
        // TODO Auto-generated method stub
        throw new UnsupportedRepositoryOperationException();
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
        return "Property("+m_multi+")["+m_name+"="+((m_multi == Multi.SINGLE ) ? m_value[0].toString() : m_value)+"]";
    }

    public void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException
    {
        NodeImpl nd = (NodeImpl)getParent();
        
        nd.removeProperty(this);
    }
}
