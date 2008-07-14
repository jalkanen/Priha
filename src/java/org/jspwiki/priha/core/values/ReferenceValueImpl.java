package org.jspwiki.priha.core.values;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.jspwiki.priha.core.NodeImpl;

public class ReferenceValueImpl extends NodeValueImpl
{

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
