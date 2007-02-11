package org.jspwiki.priha.util;

import javax.jcr.RepositoryException;

public class InvalidPathException extends RepositoryException
{
    public InvalidPathException(String msg)
    {
        super(msg);
    }
}
