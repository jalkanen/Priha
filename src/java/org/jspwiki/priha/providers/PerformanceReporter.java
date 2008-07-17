package org.jspwiki.priha.providers;

public interface PerformanceReporter
{
    public enum Count {
        Open,
        Close,
        Start,
        Stop,
        ListProperties,
        GetPropertyValue,
        NodeExists,
        AddNode,
        PutPropertyValue,
        Copy,
        Move,
        ListNodes,
        ListWorkspaces,
        Remove,
        FindByUUID,
        FindReferences
    };
    
    public void resetCounts();
    
    public long getCount( Count item );
}
