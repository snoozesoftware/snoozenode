package org.inria.myriads.snoozenode.configurator.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.localcontroller.Resource;
import org.inria.myriads.snoozenode.configurator.estimator.HostEstimatorSettings;

/**
 * @author msimonin
 *
 */
public class HostMonitorSettings
{
    /** Type of monitor.*/
    private HostMonitorType type_;
        
    /** Resources monitored by this monitor.*/
    private List<Resource> resources_;
    
    /**  estimators.*/
    private Map<String, HostEstimatorSettings> estimators_;
    
    /** Interval.*/
    private int interval_;
    
    
    /** Options.*/
    private Map<String, String> options_;

    /**
     * 
     */
    public HostMonitorSettings()
    {
        resources_ = new ArrayList<Resource>();
        estimators_ = new HashMap<String, HostEstimatorSettings>();
    }

    /**
     * @return the type
     */
    public HostMonitorType getType()
    {
        return type_;
    }

    /**
     * @param type the type to set
     */
    public void setType(HostMonitorType type)
    {
        type_ = type;
    }

   
    /**
     * @return the resources
     */
    public List<Resource> getResources()
    {
        return resources_;
    }

    /**
     * @param resources the resources to set
     */
    public void setResources(List<Resource> resources)
    {
        resources_ = resources;
    }

    public void add(Resource resource)
    {
        resources_.add(resource);
    }
    
    public void add(String resourceName, HostEstimatorSettings hostEstimatorSettings)
    {
        estimators_.put(resourceName, hostEstimatorSettings);
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
     * @return the estimators
     */
    public Map<String, HostEstimatorSettings> getEstimators()
    {
        return estimators_;
    }

    /**
     * @param estimators the estimators to set
     */
    public void setEstimators(Map<String, HostEstimatorSettings> estimators)
    {
        estimators_ = estimators;
    }

    /**
     * @return the options
     */
    public Map<String, String> getOptions()
    {
        return options_;
    }

    /**
     * @param options the options to set
     */
    public void setOptions(Map<String, String> options)
    {
        options_ = options;
    }

    
    
    
}
