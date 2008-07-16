package org.jspwiki.priha.core.values;

import javax.jcr.PropertyType;
import javax.jcr.ValueFormatException;

import org.jspwiki.priha.util.InvalidPathException;
import org.jspwiki.priha.util.PathUtil;

public class PathValueImpl extends NodeValueImpl
{

    public PathValueImpl(String value) throws ValueFormatException
    {
        super( value, PropertyType.PATH );
        
        try
        {
            PathUtil.validatePath(value);
        }
        catch (InvalidPathException e)
        {
            throw new ValueFormatException("Invalid path "+e.getMessage());
        }
    }

}
