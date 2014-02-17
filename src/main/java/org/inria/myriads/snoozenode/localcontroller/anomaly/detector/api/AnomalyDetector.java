package org.inria.myriads.snoozenode.localcontroller.anomaly.detector.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.MonitoringThresholds;
import org.inria.myriads.snoozecommon.communication.localcontroller.Resource;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozenode.configurator.anomaly.AnomalyDetectorSettings;
import org.inria.myriads.snoozenode.localcontroller.anomaly.detector.api.impl.SimpleAnomalyDetector;
import org.inria.myriads.snoozenode.localcontroller.monitoring.enums.LocalControllerState;
import org.inria.myriads.snoozenode.localcontroller.monitoring.estimator.MonitoringEstimator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AnomalyDetector
{
    
    /** LocalController description.*/
    protected LocalControllerDescription localController_;
    
    /** anomaly settings.*/
    protected AnomalyDetectorSettings settings_;

    /** Monitoring estimator.*/
    protected MonitoringEstimator monitoringEstimator_;
    
    public void setSettings(AnomalyDetectorSettings anomalyDetectorSettings)
    {
        settings_ = anomalyDetectorSettings;
    }
    
    public AnomalyDetectorSettings getSettings()
    {
        return settings_;
    }
    
    /** Initialize the detector. Called after constructor and setters.*/
    public abstract void initialize();
    
    /** Routines to detect an anomaly.*/
    public abstract LocalControllerState detectAnomaly(Map<String, Resource> hostResources, List<VirtualMachineMetaData> virtualMachines);
   
    /**
     * @return the localController
     */
    public LocalControllerDescription getLocalController()
    {
        return localController_;
    }

    /**
     * @param localController the localController to set
     */
    public void setLocalController(LocalControllerDescription localController)
    {
        localController_ = localController;
    }

    /**
     * @return the monitoringEstimator
     */
    public MonitoringEstimator getMonitoringEstimator()
    {
        return monitoringEstimator_;
    }

    /**
     * @param monitoringEstimator the monitoringEstimator to set
     */
    public void setMonitoringEstimator(MonitoringEstimator monitoringEstimator)
    {
        monitoringEstimator_ = monitoringEstimator;
    }
    
  
    
}
