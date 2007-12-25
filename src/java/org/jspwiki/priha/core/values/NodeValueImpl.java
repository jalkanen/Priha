package org.jspwiki.priha.core.values;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.jspwiki.priha.core.NodeImpl;

/**
 *  Superclass of all classes which reference a Node
 *  
 */
public abstract class NodeValueImpl extends ValueImpl implements Value
{
    private int m_type;
    private String m_value;
    
    protected NodeValueImpl( NodeImpl value, int type )
    {
        try
        {
            m_value = value.getPath();
        }
        catch (RepositoryException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        m_type  = type;
    }

    protected NodeValueImpl(String value, int type)
    {
        m_value = value;
        m_type  = type;
    }

    @Override
    public String getString()
    {
        return m_value.toString();
    }
    
    @Override
    public InputStream getStream()
    {
        return new ByteArrayInputStream( m_value.toString().getBytes() );
    }
    
    public int getType()
    {
        return m_type;
    }

}
