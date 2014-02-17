package org.inria.myriads.snoozenode.localcontroller.anomaly.detector;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.MonitoringThresholds;
import org.inria.myriads.snoozenode.configurator.anomaly.AnomalyDetectorSettings;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPolicy;
import org.inria.myriads.snoozenode.localcontroller.anomaly.detector.api.AnomalyDetector;
import org.inria.myriads.snoozenode.localcontroller.anomaly.detector.api.impl.SimpleAnomalyDetector;
import org.inria.myriads.snoozenode.localcontroller.monitoring.estimator.MonitoringEstimator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnomalyDetectorFactory
{


    public static AnomalyDetector newAnomalyDetectorEstimator(
            MonitoringEstimator estimator,
            LocalControllerDescription localController,
            AnomalyDetectorSettings anomalyDetectorSettings
            )
    {
        
        /** Define the logger. */
        final Logger log_ = LoggerFactory.getLogger(AnomalyDetectorFactory.class);
        
        String classURI = anomalyDetectorSettings.getName();
        ClassLoader classLoader = AnomalyDetectorFactory.class.getClassLoader();
        
        AnomalyDetector anomalyDetector = null;
        try
        {
            Class<?> anomalyClass = classLoader.loadClass(classURI);
            Object anomalyDetectorObject;
            anomalyDetectorObject = anomalyClass.getConstructor().newInstance();
            anomalyDetector = (AnomalyDetector) anomalyDetectorObject;
            log_.debug("Sucessfully created anomaly detector " + classURI);
            
        }
        catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException e)
        {
            e.printStackTrace();
            anomalyDetector = new SimpleAnomalyDetector();
        }
        
        anomalyDetector.setSettings(anomalyDetectorSettings);
        anomalyDetector.setLocalController(localController);
        anomalyDetector.setMonitoringEstimator(estimator);
        anomalyDetector.initialize();
        return anomalyDetector;
        
    }
    
}
