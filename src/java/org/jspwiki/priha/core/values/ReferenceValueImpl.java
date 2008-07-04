package org.jspwiki.priha.core.values;

import javax.jcr.PropertyType;

import org.jspwiki.priha.core.NodeImpl;

public class ReferenceValueImpl extends NodeValueImpl
{

    public ReferenceValueImpl(String value)
    {
        super( value,PropertyType.REFERENCE );
    }

    public ReferenceValueImpl(NodeImpl impl)
    {
        super( impl,PropertyType.REFERENCE );
    }

    
}
