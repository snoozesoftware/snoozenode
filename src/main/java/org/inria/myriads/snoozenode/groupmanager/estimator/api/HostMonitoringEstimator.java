package org.inria.myriads.snoozenode.groupmanager.estimator.api;

import java.util.Map;

import org.inria.myriads.snoozecommon.communication.localcontroller.Resource;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.VirtualMachineMonitoringData;

public interface HostMonitoringEstimator
{
    /**
     * 
     * Estimate a resource.
     * 
     * @param resource
     * @return
     */
    double estimate(Resource resource);
}
