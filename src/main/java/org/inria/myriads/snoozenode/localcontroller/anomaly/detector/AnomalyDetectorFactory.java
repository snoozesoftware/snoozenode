package org.inria.myriads.snoozenode.localcontroller.anomaly.detector;

import java.util.List;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.MonitoringThresholds;
import org.inria.myriads.snoozenode.localcontroller.anomaly.detector.api.AnomalyDetector;
import org.inria.myriads.snoozenode.localcontroller.anomaly.detector.api.AnomalyDetectorEstimator;
import org.inria.myriads.snoozenode.localcontroller.anomaly.detector.api.impl.SimpleAnomalyDetector;
import org.inria.myriads.snoozenode.localcontroller.monitoring.estimator.MonitoringEstimator;

public class AnomalyDetectorFactory
{


    public static AnomalyDetector newAnomalyDetectorEstimator(
            MonitoringEstimator estimator,
            LocalControllerDescription localController
            )
    {
        return new SimpleAnomalyDetector(estimator, localController);
    }
}
