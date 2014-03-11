package org.inria.myriads.snoozenode.configurator.monitoring;

import java.util.HashMap;

/**
 * @author msimonin
 *
 */
public class HostMonitoringSettings
{
    /** List of monitor settings.*/
    private HashMap<String, HostMonitorSettings> hostMonitorSettings_;
    
    /** Default Interval.*/ 
    private int interval_;
    
    /** Default estimator name.*/
    private String estimator_;
    
    /** Default number of monitoring entries.*/
    private int numberOfMonitoringEntries_;
    
    /**
     * Constructor. 
     */
    public HostMonitoringSettings()
    {
        super();
        hostMonitorSettings_ = new HashMap<String, HostMonitorSettings>();
    }

    /**
     * @return the hostMonitorSettings
     */
    public HashMap<String, HostMonitorSettings> getHostMonitorSettings()
    {
        return hostMonitorSettings_;
    }

    /**
     * @param hostMonitorSettings the hostMonitorSettings to set
     */
    public void setHostMonitorSettings(HashMap<String, HostMonitorSettings> hostMonitorSettings)
    {
        hostMonitorSettings_ = hostMonitorSettings;
    }

    public void add(String name, HostMonitorSettings hostMonitorSettings)
    {
        System.out.println(name.toString());
        System.out.println(hostMonitorSettings.getResources().size());
        hostMonitorSettings_.put(name, hostMonitorSettings);
    }

    /**
     * @return the interval
     */
    public int getInterval()
    {
        return interval_;
    }

    /**
     * @param interval the interval to set
     */
    public void setInterval(int interval)
    {
        interval_ = interval;
    }

    /**
     * @return the estimator
     */
    public String getEstimator()
    {
        return estimator_;
    }

    /**
     * @param estimator the estimator to set
     */
    public void setEstimator(String estimator)
    {
        estimator_ = estimator;
    }

    /**
     * @return the numberOfMonitoringEntries
     */
    public int getNumberOfMonitoringEntries()
    {
        return numberOfMonitoringEntries_;
    }

    /**
     * @param numberOfMonitoringEntries the numberOfMonitoringEntries to set
     */
    public void setNumberOfMonitoringEntries(int numberOfMonitoringEntries)
    {
        numberOfMonitoringEntries_ = numberOfMonitoringEntries;
    }

 
    
    
    
    
}
