package org.jspwiki.priha.core.values;

import java.io.Serializable;

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.jspwiki.priha.util.InvalidPathException;
import org.jspwiki.priha.util.PathUtil;

public class PathValueImpl extends NodeValueImpl implements Value, Serializable
{
    private static final long serialVersionUID = -980121404025627369L;

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
