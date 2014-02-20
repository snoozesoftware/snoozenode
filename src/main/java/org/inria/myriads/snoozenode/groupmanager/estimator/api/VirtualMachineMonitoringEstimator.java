package org.inria.myriads.snoozenode.groupmanager.estimator.api;

import java.util.Map;

import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.VirtualMachineMonitoringData;

public interface VirtualMachineMonitoringEstimator
{
    /**
     * 
     * Estimate a resource (CPU, Memory, Network)
     * 
     * @param history
     * @return
     */
    double estimate(Map<Long, VirtualMachineMonitoringData> history);
}
