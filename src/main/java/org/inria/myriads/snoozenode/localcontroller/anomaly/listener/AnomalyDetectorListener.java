package org.inria.myriads.snoozenode.localcontroller.anomaly.listener;


/**
 * 
 * Anomaly Detector listener.
 * 
 * @author msimonin
 *
 */
public interface AnomalyDetectorListener
{
    /**
     * 
     * Called upon anomaly.
     * 
     * @param anomaly       The anomaly object.
     */
    void onAnomalyDetected(Object anomaly);   
}
