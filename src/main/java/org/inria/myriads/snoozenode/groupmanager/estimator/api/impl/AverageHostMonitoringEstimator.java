package org.inria.myriads.snoozenode.groupmanager.estimator.api.impl;

import org.inria.myriads.snoozecommon.communication.localcontroller.Resource;
import org.inria.myriads.snoozecommon.datastructure.LRUCache;
import org.inria.myriads.snoozenode.groupmanager.estimator.api.HostMonitoringEstimator;

public class AverageHostMonitoringEstimator implements HostMonitoringEstimator 
{

    @Override
    public double estimate(Resource resource)
    {
        LRUCache<Long, Double> usedCapacity = resource.getHistory();
        if (usedCapacity == null || usedCapacity.size() == 0)
        {
            return 0;
        }
        int result = 0;
        for (double value : usedCapacity.values())
        {
            result += value;
        }
        return result / (usedCapacity.size() * 1.0);
        
    }

}
