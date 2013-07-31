package org.inria.myriads.snoozenode.database.api.impl.cassandra;

public final class CassandraUtils
{
    
    /** Cluster column family. */
    public static  String CLUSTER = "Test Cluster";
    
    /** Keyspace column family. */
    public static  String KEYSPACE = "snooze";
    
    /** virtual Machines column family. */
    public static  String VIRTUALMACHINES_CF = "virtualmachines";
    
    /** virtual Machines column family. */
    public static  String VIRTUALMACHINES_MONITORING_CF = "virtualmachines_monitoring";
    
    /** Groupmanagers column family. */
    public static  String GROUPMANAGERS_CF = "groupmanagers";
    
    /** localcontrollers column family. */
    public static  String LOCALCONTROLLERS_CF = "localcontrollers";
    
    /** localcontrollers monitoring column family. */
    public static  String LOCALCONTROLLERS_MAPPING_CF = "localcontrollers_mapping";
    
    /** groupmanagers column family. */
    public static  String GROUPMANAGERS_MONITORING_CF = "groupmanagers_monitoring";
    
    /** ippools column family. */
    public static  String IPSPOOL_CF = "ipspool";
    
    /**
     * Hide Constructor.
     */
    public CassandraUtils()
    {
        throw new UnsupportedOperationException();
    }
    
    
    
}


