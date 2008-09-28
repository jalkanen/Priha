package org.priha.core;

import javax.xml.namespace.QName;

public final class JCRConstants
{

    /* Namespaces */
    public static final String NS_JCP_SV       = "http://www.jcp.org/jcr/sv/1.0";
    public static final String NS_JCP_MIX      = "http://www.jcp.org/jcr/mix/1.0";
    public static final String NS_JCP_NT       = "http://www.jcp.org/jcr/nt/1.0";
    public static final String NS_JCP          = "http://www.jcp.org/jcr/1.0";

    /* Strings */
    public static final String JCR_CREATED      = "{"+NS_JCP+"}created";
    public static final String JCR_UUID         = "{"+NS_JCP+"}uuid";
    public static final String JCR_MIXIN_TYPES  = "{"+NS_JCP+"}mixinTypes";
    public static final String JCR_PRIMARY_TYPE = "{"+NS_JCP+"}primaryType";
    
    /* QNames */
    public static final QName  Q_NT_VERSION        = QName.valueOf("{"+NS_JCP_NT+"}version");
    public static final QName  Q_NT_VERSIONHISTORY = QName.valueOf("{"+NS_JCP_NT+"}versionHistory");
    public static final QName  Q_NT_UNSTRUCTURED   = QName.valueOf("{"+NS_JCP_NT+"}unstructured");
    public static final QName  Q_NT_BASE           = QName.valueOf("{"+NS_JCP_NT+"}base");
    public static final QName  Q_JCR_PRIMARYTYPE   = QName.valueOf(JCR_PRIMARY_TYPE);
    public static final QName  Q_JCR_UUID          = QName.valueOf(JCR_UUID);
    public static final QName  Q_JCR_CREATED       = QName.valueOf(JCR_CREATED);
    public static final QName  Q_JCR_MIXINTYPES    = QName.valueOf(JCR_MIXIN_TYPES);
}
