package org.jspwiki.priha.core.values;

import javax.jcr.PropertyType;

public class PathValueImpl extends NodeValueImpl
{

    public PathValueImpl(String value)
    {
        super( value, PropertyType.PATH );
    }

}
