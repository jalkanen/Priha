package org.priha.core.namespace;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.xml.namespace.QName;

public interface NamespaceMapper
{
    public QName toQName( String prefixedName ) throws NamespaceException;
    
    public String fromQName( QName name ) throws NamespaceException;
}
