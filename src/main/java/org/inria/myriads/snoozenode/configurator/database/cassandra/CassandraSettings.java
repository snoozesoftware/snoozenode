package org.inria.myriads.snoozenode.configurator.database.cassandra;

public class CassandraSettings
{

    private String hosts_;

    /**
     * Constructor. 
     */
    public CassandraSettings()
    {
    
    }

    /**
     * @param hosts
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
