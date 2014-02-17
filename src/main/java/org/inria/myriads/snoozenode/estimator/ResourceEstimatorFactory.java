package org.inria.myriads.snoozenode.estimator;

import org.inria.myriads.snoozecommon.communication.localcontroller.MonitoringThresholds;
import org.inria.myriads.snoozenode.configurator.estimator.EstimatorSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.HostMonitoringSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.MonitoringSettings;
import org.inria.myriads.snoozenode.configurator.submission.PackingDensity;
import org.inria.myriads.snoozenode.estimator.api.impl.StaticDynamicResourceDemandEstimator;
import org.inria.myriads.snoozenode.exception.ResourceDemandEstimatorException;

public final class ResourceEstimatorFactory
{

    public static StaticDynamicResourceDemandEstimator newResourceDemandEstimator(
            EstimatorSettings estimatorSettings, 
            MonitoringSettings monitoringSettings, 
            HostMonitoringSettings hostMonitoringsettings,
            PackingDensity packingDensity) throws ResourceDemandEstimatorException
    {
        StaticDynamicResourceDemandEstimator resourceDemandEstimator = new StaticDynamicResourceDemandEstimator();
        resourceDemandEstimator.setEstimatorSettings(estimatorSettings);
        resourceDemandEstimator.setMonitoringSettings(monitoringSettings);
        resourceDemandEstimator.setHostMonitoringSettings(hostMonitoringsettings);
        resourceDemandEstimator.setPackingDensity(packingDensity);
        resourceDemandEstimator.initialize();
        return resourceDemandEstimator;
    }


}
