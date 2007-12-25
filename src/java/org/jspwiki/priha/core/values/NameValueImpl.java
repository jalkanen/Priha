package org.jspwiki.priha.core.values;

import javax.jcr.PropertyType;

public class NameValueImpl extends NodeValueImpl
{
    public NameValueImpl(String value)
    {
        super( value, PropertyType.NAME );
    }
}
