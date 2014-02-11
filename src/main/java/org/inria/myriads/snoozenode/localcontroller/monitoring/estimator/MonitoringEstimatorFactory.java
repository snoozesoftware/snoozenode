package org.inria.myriads.snoozenode.localcontroller.monitoring.estimator;

import org.inria.myriads.snoozenode.localcontroller.monitoring.estimator.api.impl.AverageEstimator;

public class MonitoringEstimatorFactory
{

    public static MonitoringEstimator newEstimator()
    {
        return new AverageEstimator();
    }

}
