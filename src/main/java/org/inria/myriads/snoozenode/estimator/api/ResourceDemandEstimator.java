package org.inria.myriads.snoozenode.estimator.api;

import org.inria.myriads.snoozenode.configurator.estimator.EstimatorSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.HostMonitoringSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.MonitoringSettings;
import org.inria.myriads.snoozenode.configurator.submission.PackingDensity;

/**
 * @author msimonin
 *
 */
public abstract class ResourceDemandEstimator
{
    
    protected EstimatorSettings estimatorSettings_;
    
    protected MonitoringSettings monitoringSettings_;
    
    protected HostMonitoringSettings hostMonitoringSettings_;
    
    protected PackingDensity packingDensity_;    
    
    /**
     * @return the estimatorSettings
     */
    public EstimatorSettings getEstimatorSettings()
    {
        return estimatorSettings_;
    }

    /**
     * @param estimatorSettings the estimatorSettings to set
     */
    public void setEstimatorSettings(EstimatorSettings estimatorSettings)
    {
        estimatorSettings_ = estimatorSettings;
    }

    /**
     * @return the packingDensity
     */
    public PackingDensity getPackingDensity()
    {
        return packingDensity_;
    }

    /**
     * @param packingDensity the packingDensity to set
     */
    public void setPackingDensity(PackingDensity packingDensity)
    {
        packingDensity_ = packingDensity;
    }

    /**
     * @return the hostMonitoringSettings
     */
    public HostMonitoringSettings getHostMonitoringSettings()
    {
        return hostMonitoringSettings_;
    }

    /**
     * @param hostMonitoringSettings the hostMonitoringSettings to set
     */
    public void setHostMonitoringSettings(HostMonitoringSettings hostMonitoringSettings)
    {
        hostMonitoringSettings_ = hostMonitoringSettings;
    }

    /**
     * @return the monitoringSettings
     */
    public MonitoringSettings getMonitoringSettings()
    {
        return monitoringSettings_;
    }

    /**
     * @param monitoringSettings the monitoringSettings to set
     */
    public void setMonitoringSettings(MonitoringSettings monitoringSettings)
    {
        monitoringSettings_ = monitoringSettings;
    }
    
    
}
