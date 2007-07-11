package org.jspwiki.priha.util;

import javax.jcr.RepositoryException;

public class InvalidPathException extends RepositoryException
{
    private static final long serialVersionUID = 1L;

    public InvalidPathException(String msg)
    {
        super(msg);
    }
}
