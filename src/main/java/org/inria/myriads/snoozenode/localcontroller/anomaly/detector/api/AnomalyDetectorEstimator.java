package org.inria.myriads.snoozenode.localcontroller.anomaly.detector.api;

import java.util.HashMap;
import java.util.List;

import org.inria.myriads.snoozecommon.communication.localcontroller.Resource;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;

public interface AnomalyDetectorEstimator
{

    boolean estimate(HashMap<String, Resource> hostResources, List<VirtualMachineMetaData> virtualMachines);
    
}
