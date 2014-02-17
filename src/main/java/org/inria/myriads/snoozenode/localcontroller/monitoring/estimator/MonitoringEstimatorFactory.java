package org.inria.myriads.snoozenode.localcontroller.monitoring.estimator;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozenode.configurator.estimator.HostEstimatorSettings;
import org.inria.myriads.snoozenode.estimator.api.Estimator;
import org.inria.myriads.snoozenode.estimator.api.impl.AverageEstimator;

public class MonitoringEstimatorFactory
{

    public static Estimator newEstimator(String estimator)
    {
        return new AverageEstimator();
    }

    public static Estimator newEstimator(HostEstimatorSettings hostEstimatorSettings)
    {
        return new AverageEstimator();
    }


}
