package org.inria.myriads.snoozenode.localcontroller.monitoring.estimator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.localcontroller.Resource;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.VirtualMachineMonitoringData;
import org.inria.myriads.snoozecommon.datastructure.LRUCache;
import org.inria.myriads.snoozecommon.globals.Globals;
import com.google.common.primitives.Doubles;

public abstract class MonitoringEstimator
{
    public abstract double estimate(List<Double> values);
    
    public List<Double> estimateVirtualMachineUtilization(VirtualMachineMetaData virtualMachineMetaData)
    {
        LRUCache<Long, VirtualMachineMonitoringData> usedCapacity = virtualMachineMetaData.getUsedCapacity();
        List<Double> cpuUtilization = getUtilization(usedCapacity, Globals.CPU_UTILIZATION_INDEX);
        List<Double> memUtilization = getUtilization(usedCapacity, Globals.MEMORY_UTILIZATION_INDEX);        
        List<Double> rxUtilization = getUtilization(usedCapacity, Globals.NETWORK_RX_UTILIZATION_INDEX);
        List<Double> txUtilization = getUtilization(usedCapacity, Globals.NETWORK_TX_UTILIZATION_INDEX);
        double cpuEstimation = estimate(cpuUtilization);
        double memEstimation = estimate(memUtilization);
        double rxEstimation = estimate(rxUtilization);
        double txEstimation = estimate(txUtilization);
        
        List<Double> utilizationVector = 
                Doubles.asList(cpuEstimation, memEstimation, rxEstimation, txEstimation);
        
        return utilizationVector;
        
    }
    
    public Map<String, Double> estimateHostUtilization(Map<String, Resource> hostUtilizationHistory)
    {
        Map<String, Double> hostUtilization = new HashMap<String, Double>(); 
        for (Resource resource : hostUtilizationHistory.values())
        {
            List<Double> history = new ArrayList<Double>(resource.getHistory().values());
            double estimation = estimate(history);
            hostUtilization.put(resource.getName(), estimation);
        }
        return hostUtilization;
    }

    private List<Double> getUtilization(LRUCache<Long, VirtualMachineMonitoringData> usedCapacity, int index)
    {
        List<Double> utilization = new ArrayList<Double>();
       for (VirtualMachineMonitoringData monitoringData : usedCapacity.values())
       {
           utilization.add(monitoringData.getUsedCapacity().get(index));
       }
       return utilization;
    }
    
    //TODO.
    
}
