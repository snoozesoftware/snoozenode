package org.inria.myriads.snoozenode.localcontroller.anomaly.detector;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.List;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.MonitoringThresholds;
import org.inria.myriads.snoozenode.configurator.anomaly.AnomalyDetectorSettings;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.estimator.api.impl.StaticDynamicResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPolicy;
import org.inria.myriads.snoozenode.localcontroller.anomaly.detector.api.AnomalyDetector;
import org.inria.myriads.snoozenode.localcontroller.anomaly.detector.api.impl.SimpleAnomalyDetector;
import org.inria.myriads.snoozenode.util.PluginUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author msimonin
 *
 */
public class AnomalyDetectorFactory
{

    /** Define the logger. */
    final static Logger log_ = LoggerFactory.getLogger(AnomalyDetectorFactory.class);
    
    /**
     *  Hide Constructor. 
     */
    private AnomalyDetectorFactory()
    {
                
    }

    public static AnomalyDetector newAnomalyDetectorEstimator(
            ResourceDemandEstimator estimator,
            LocalControllerDescription localController,
            AnomalyDetectorSettings anomalyDetectorSettings
            )
    {        
        String classURI = anomalyDetectorSettings.getName();
        AnomalyDetector anomalyDetector = null;
        try
        {
            Class<?> anomalyClass = PluginUtils.getClassFromPluginsDirectory(classURI);
            Object anomalyDetectorObject;
            anomalyDetectorObject = anomalyClass.getConstructor().newInstance();
            anomalyDetector = (AnomalyDetector) anomalyDetectorObject;
            log_.debug("Sucessfully created anomaly detector " + classURI);
            
        }
        catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException | MalformedURLException e)
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
