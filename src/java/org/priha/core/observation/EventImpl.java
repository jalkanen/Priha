/**
 * 
 */
package org.priha.core.observation;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.priha.core.SessionImpl;
import org.priha.path.Path;

class EventImpl implements Event
{
    SessionImpl m_session;
    int m_eventType;
    Path m_path;
    
    public EventImpl(SessionImpl s, int eventType, Path path)
    {
        m_session = s;
        m_eventType = eventType;
        m_path = path;
    }
    
    public String getPath() throws RepositoryException
    {
        return m_path.toString(m_session);
    }

    public int getType()
    {
        return m_eventType;
    }

    public String getUserID()
    {
        return m_session.getUserID();
    }

    public String toString()
    {
        return getEventString(m_eventType) + " " + m_path.toString();
    }

    private String getEventString(int type)
    {
        switch( type )
        {
            case NODE_ADDED:
                return "NODE_ADDED";
                
            case NODE_REMOVED:
                return "NODE_REMOVED";
                
            case PROPERTY_ADDED:
                return "PROPERTY_ADDED";
                
            case PROPERTY_CHANGED:
                return "PROPERTY_CHANGED";
                
            case PROPERTY_REMOVED:
                return "PROPERTY_REMOVED";
                
            default:
                return "UNKNOWN";
        }
    }
}