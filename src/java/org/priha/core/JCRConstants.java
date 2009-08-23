package org.priha.core;

import org.priha.util.QName;

public final class JCRConstants
{

    /* Namespaces */
    public static final String NS_JCP_SV       = "http://www.jcp.org/jcr/sv/1.0";
    public static final String NS_JCP_MIX      = "http://www.jcp.org/jcr/mix/1.0";
    public static final String NS_JCP_NT       = "http://www.jcp.org/jcr/nt/1.0";
    public static final String NS_JCP          = "http://www.jcp.org/jcr/1.0";

    /* Strings */
    public static final String JCR_CREATED         = mk( NS_JCP, "created" );
    public static final String JCR_UUID            = mk( NS_JCP, "uuid" );
    public static final String JCR_MIXIN_TYPES     = mk( NS_JCP, "mixinTypes" );
    public static final String JCR_PRIMARY_TYPE    = mk( NS_JCP, "primaryType");
    public static final String JCR_VERSIONHISTORY  = mk( NS_JCP, "versionHistory" );
    public static final String JCR_ISCHECKEDOUT    = mk( NS_JCP, "isCheckedOut" );
    public static final String JCR_BASEVERSION     = mk( NS_JCP, "baseVersion" );
    public static final String JCR_ROOT            = mk( NS_JCP, "root" );
    public static final String JCR_SYSTEM          = mk( NS_JCP, "system" );
    public static final String MIX_VERSIONABLE     = mk( NS_JCP_MIX, "versionable" );
    public static final String MIX_REFERENCEABLE   = mk( NS_JCP_MIX, "referenceable" );

    /* QNames */
    public static final QName  Q_NT_VERSION        = QName.valueOf("{"+NS_JCP_NT+"}version");
    public static final QName  Q_NT_VERSIONHISTORY = QName.valueOf("{"+NS_JCP_NT+"}versionHistory");
    public static final QName  Q_NT_UNSTRUCTURED   = QName.valueOf("{"+NS_JCP_NT+"}unstructured");
    public static final QName  Q_NT_BASE           = QName.valueOf("{"+NS_JCP_NT+"}base");
    public static final QName  Q_JCR_PRIMARYTYPE   = QName.valueOf(JCR_PRIMARY_TYPE);
    public static final QName  Q_JCR_UUID          = QName.valueOf(JCR_UUID);
    public static final QName  Q_JCR_CREATED       = QName.valueOf(JCR_CREATED);
    public static final QName  Q_JCR_MIXINTYPES    = QName.valueOf(JCR_MIXIN_TYPES);
    public static final QName  Q_JCR_VERSIONHISTORY = QName.valueOf(JCR_VERSIONHISTORY);
    public static final QName  Q_JCR_ISCHECKEDOUT  = QName.valueOf( JCR_ISCHECKEDOUT );
    
    public static final QName  Q_MIX_VERSIONABLE   = QName.valueOf( MIX_VERSIONABLE );
    public static final QName  Q_MIX_REFERENCEABLE = QName.valueOf( MIX_REFERENCEABLE );
    public static final QName  Q_JCR_BASEVERSION   = QName.valueOf( JCR_BASEVERSION );
    public static final QName  Q_JCR_ROOT          = QName.valueOf( JCR_ROOT );
    public static final QName  Q_JCR_SYSTEM        = QName.valueOf( JCR_SYSTEM );
    
    private static final String mk(String namespace, String name)
    {
        return "{"+namespace+"}"+name;
    }
}
