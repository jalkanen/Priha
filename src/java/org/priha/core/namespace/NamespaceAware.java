package org.priha.core.namespace;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.xml.namespace.QName;

public interface NamespaceAware
{
    public QName toQName( String prefixedName ) throws NamespaceException, RepositoryException;
    
    public String fromQName( QName name ) throws NamespaceException, RepositoryException;
}
