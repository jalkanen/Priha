package org.priha.core.values;

import org.priha.core.namespace.NamespaceMapper;

/**
 *  A QValue is a Value which contains something with an FQN, like a QName or a Path.
 *  Because some Values require a mapping to the current namespace, we need to have
 *  a way to represent a Session-independent Value.
 */
public abstract class QValue
{
    public abstract ValueImpl getValue(NamespaceMapper nsm);
    
    public interface QValueInner
    {
        public QValue getQValue();
    }
}
