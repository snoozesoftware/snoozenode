package org.inria.myriads.snoozenode.configurator.database.cassandra;

/**
 * 
 * Cassandra settings.
 * 
 * @author msimonin
 *
 */
public class CassandraSettings
{

    /** Coma separated list of hosts.*/
    private String hosts_;

    /**
     * Constructor. 
     */
    public CassandraSettings()
    {
    
    }

    /**
     * 
     * Constructor.
     * 
     * @param hosts     The list of hosts (coma separated).
     */
    public CassandraSettings(String hosts)
    {
        hosts_ = hosts;
    }

    /**
     * @return the hosts
     */
    public String getHosts()
    {
        return hosts_;
    }

    /**
     * @param hosts the hosts to set
     */
    public void setHosts(String hosts)
    {
        hosts_ = hosts;
    }
    
    
    
}
