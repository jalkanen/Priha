package org.jspwiki.priha.util;

import java.util.List;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;

public class PropertyIteratorImpl 
    extends GenericIterator 
    implements PropertyIterator
{

    public PropertyIteratorImpl(List list)
    {
        super(list);
    }

    public Property nextProperty()
    {
        return (Property) next();
    }

}
