package org.inria.myriads.snoozenode.localcontroller.anomaly.detector.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.localcontroller.Resource;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozenode.localcontroller.monitoring.enums.LocalControllerState;

public interface AnomalyDetector
{
    
    LocalControllerState detectAnomaly(Map<String, Resource> hostResources, List<VirtualMachineMetaData> virtualMachines);
    
}
