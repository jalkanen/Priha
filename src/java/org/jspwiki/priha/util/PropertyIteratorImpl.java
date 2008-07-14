package org.jspwiki.priha.util;

import java.util.Collection;
import java.util.List;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;

import org.jspwiki.priha.core.PropertyImpl;

public class PropertyIteratorImpl 
    extends GenericIterator 
    implements PropertyIterator
{
    public PropertyIteratorImpl(Collection<PropertyImpl> references)
    {
        super(references);
    }

    public Property nextProperty()
    {
        return (Property) next();
    }

}
