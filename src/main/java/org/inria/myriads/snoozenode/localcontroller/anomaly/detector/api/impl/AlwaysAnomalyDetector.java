package org.inria.myriads.snoozenode.localcontroller.anomaly.detector.api.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.localcontroller.MonitoringThresholds;
import org.inria.myriads.snoozecommon.communication.localcontroller.Resource;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.util.MathUtils;
import org.inria.myriads.snoozenode.localcontroller.anomaly.detector.api.AnomalyDetector;
import org.inria.myriads.snoozenode.localcontroller.monitoring.enums.LocalControllerState;
import org.inria.myriads.snoozenode.util.ThresholdUtils;
import org.inria.myriads.snoozenode.util.UtilizationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * This detector detects if one of the vm metric exceed its max threshold 
 * and send an alert. 
 * 
 * 
 * @author msimonin
 *
 */
public class AlwaysAnomalyDetector extends AnomalyDetector
{

    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(AlwaysAnomalyDetector.class);
    
    /**
     * Constructor.
     * 
     * @param monitoringThresholds  The monitoring thresholds
     * @param totalCapacity         The total local controller capacity
     */
    public AlwaysAnomalyDetector()
    {
        log_.debug("Initializing the allways detector");
    }
    

    @Override
    public void initialize()
    {

    }
    
    @Override
    public LocalControllerState detectAnomaly(Map<String, Resource> hostResources, List<VirtualMachineMetaData> virtualMachines)
    {
        return LocalControllerState.OVERLOADED;
    }

}
