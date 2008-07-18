package org.jspwiki.priha.core.values;

import java.io.Serializable;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;

import org.jspwiki.priha.core.NodeImpl;

public class ReferenceValueImpl extends NodeValueImpl implements Value, Serializable
{
    private static final long serialVersionUID = -3852563143401195069L;

    public ReferenceValueImpl(String value)
    {
        super( value,PropertyType.REFERENCE );
    }

    public ReferenceValueImpl(NodeImpl impl) 
        throws UnsupportedRepositoryOperationException, RepositoryException
    {
        super( impl.getUUID(), PropertyType.REFERENCE );
    }
    
    @Override
    public String toString()
    {
        return "REF="+m_value.toString();
    }
}
