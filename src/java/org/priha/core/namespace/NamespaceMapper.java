package org.priha.core.namespace;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;

import org.priha.util.QName;

public interface NamespaceMapper
{
    public QName toQName( String prefixedName ) throws NamespaceException, RepositoryException;
    
    public String fromQName( QName name ) throws NamespaceException;
}
