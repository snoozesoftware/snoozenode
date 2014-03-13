package org.inria.myriads.snoozenode.groupmanager.estimator.api;

import org.inria.myriads.snoozecommon.communication.localcontroller.Resource;

/**
 * 
 * 
 * Host Monitoring estimator.
 * 
 * @author msimonin
 *
 */
public interface HostMonitoringEstimator
{
    /**
     * 
     * Estimate a resource.
     * 
     * @param resource      The resource to estimate.
     * @return  the estimation
     */
    double estimate(Resource resource);
}
