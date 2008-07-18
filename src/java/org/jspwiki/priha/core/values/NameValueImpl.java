package org.jspwiki.priha.core.values;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

public class NameValueImpl extends NodeValueImpl implements Value, Serializable
{
    private static final long serialVersionUID = -5040292769406453341L;

    public NameValueImpl(String value) throws ValueFormatException
    {
        super( value, PropertyType.NAME );

        try
        {
            URI uri = new URI( value );
        }
        catch (URISyntaxException e)
        {
            throw new ValueFormatException("Not in URI format");
        } 
    }
    
    @Override
    public String getString()
    {
        return m_value;
    }
    
}
