package org.inria.myriads.snoozenode.localcontroller.anomaly.detector;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozenode.configurator.anomaly.AnomalyDetectorSettings;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.localcontroller.anomaly.detector.api.AnomalyDetector;
import org.inria.myriads.snoozenode.localcontroller.anomaly.detector.api.impl.SimpleAnomalyDetector;
import org.inria.myriads.snoozenode.util.PluginUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author msimonin
 *
 */
public final class AnomalyDetectorFactory
{

    /** Define the logger. */
    static final Logger log_ = LoggerFactory.getLogger(AnomalyDetectorFactory.class);
    
    /**
     *  Hide Constructor. 
     */
    private AnomalyDetectorFactory()
    {
                
    }

    /**
     * 
     * Creates a new anomaly detector.
     * 
     * @param estimator                 The estimator.
     * @param localController           The local controller description.
     * @param anomalyDetectorSettings   The anomaly detector settings.
     * @return  The newly created anomaly detector.
     */
    public static AnomalyDetector newAnomalyDetectorEstimator(
            ResourceDemandEstimator estimator,
            LocalControllerDescription localController,
            AnomalyDetectorSettings anomalyDetectorSettings
            )
    {     
        String classURI = anomalyDetectorSettings.getName();
        
        AnomalyDetector anomalyDetector = null;
        if (classURI.equals("simple"))
        {
            log_.debug("Loading the simple anomaly detector");
            anomalyDetector = new SimpleAnomalyDetector();
        }
        else
        {
            Object anomalyDetectorObject;
            try
            {
                log_.debug("Loading custom anomaly detector");
                anomalyDetectorObject = PluginUtils.createFromFQN(classURI);
                anomalyDetector = (AnomalyDetector) anomalyDetectorObject;
            }
            catch (Exception e)
            {
                log_.error("Unable to load the custom anomaly detector");
                anomalyDetector = new SimpleAnomalyDetector();
            }
        }
        anomalyDetector.setSettings(anomalyDetectorSettings);
        anomalyDetector.setLocalController(localController);
        anomalyDetector.setMonitoringEstimator(estimator);
        anomalyDetector.initialize();
        return anomalyDetector;
        
    }
    
}
