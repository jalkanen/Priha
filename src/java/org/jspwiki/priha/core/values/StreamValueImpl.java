package org.jspwiki.priha.core.values;

import java.io.InputStream;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

public class StreamValueImpl extends ValueImpl implements Value
{
    private InputStream m_value;
    
    public StreamValueImpl(InputStream value)
    {
        m_value = value;
    }

    public int getType()
    {
        return PropertyType.BINARY;
    }

    @Override
    public InputStream getStream() throws IllegalStateException, RepositoryException
    {
        checkStream();
        return m_value;
    }
}
