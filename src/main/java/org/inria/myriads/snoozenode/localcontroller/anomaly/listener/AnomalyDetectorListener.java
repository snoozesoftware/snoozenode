package org.inria.myriads.snoozenode.localcontroller.anomaly.listener;

import org.inria.myriads.snoozenode.localcontroller.monitoring.enums.LocalControllerState;

public interface AnomalyDetectorListener
{
    void onAnomalyDetected(LocalControllerState state);   
}
