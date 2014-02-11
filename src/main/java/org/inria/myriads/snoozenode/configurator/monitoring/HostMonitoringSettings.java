package org.inria.myriads.snoozenode.configurator.monitoring;

import java.util.HashMap;

/**
 * @author msimonin
 *
 */
public class HostMonitoringSettings
{
    /** List of monitor settings.*/
    private HashMap<HostMonitorType, HostMonitorSettings> hostMonitorSettings_;
    
    /**
     * Constructor. 
     */
    public HostMonitoringSettings()
    {
        super();
        hostMonitorSettings_ = new HashMap<HostMonitorType, HostMonitorSettings>();
    }

    /**
     * @return the hostMonitorSettings
     */
    public HashMap<HostMonitorType, HostMonitorSettings> getHostMonitorSettings()
    {
        return hostMonitorSettings_;
    }

    /**
     * @param hostMonitorSettings the hostMonitorSettings to set
     */
    public void setHostMonitorSettings(HashMap<HostMonitorType, HostMonitorSettings> hostMonitorSettings)
    {
        hostMonitorSettings_ = hostMonitorSettings;
    }

    public void add(HostMonitorType type, HostMonitorSettings hostMonitorSettings)
    {
        hostMonitorSettings_.put(type, hostMonitorSettings);
    }

 
    
    
    
    
}
