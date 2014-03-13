package org.inria.myriads.snoozenode.groupmanager.estimator.api;

import java.util.Map;

import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.VirtualMachineMonitoringData;

/**
 * 
 * 
 * Virtual machine monitoring estimator.
 * 
 * @author msimonin
 *
 */
public interface VirtualMachineMonitoringEstimator
{
    /**
     * 
     * Estimate a resource (CPU, Memory, Network).
     * 
     * @param history               The history.
     * @return  the estimation.
     */
    double estimate(Map<Long, VirtualMachineMonitoringData> history);
    
}
