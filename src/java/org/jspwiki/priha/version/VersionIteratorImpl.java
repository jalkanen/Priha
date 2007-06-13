package org.jspwiki.priha.version;

import java.util.List;

import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;

import org.jspwiki.priha.util.GenericIterator;

public class VersionIteratorImpl extends GenericIterator implements VersionIterator
{
    public VersionIteratorImpl(List list)
    {
        super(list);
    }

    public Version nextVersion()
    {
        return (Version) next();
    }

}
