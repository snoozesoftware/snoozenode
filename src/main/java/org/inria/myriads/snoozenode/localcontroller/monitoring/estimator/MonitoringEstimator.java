package org.inria.myriads.snoozenode.localcontroller.monitoring.estimator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.Resource;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.VirtualMachineMonitoringData;
import org.inria.myriads.snoozecommon.datastructure.LRUCache;
import org.inria.myriads.snoozecommon.globals.Globals;
import org.inria.myriads.snoozenode.configurator.estimator.EstimatorSettings;
import org.inria.myriads.snoozenode.configurator.estimator.HostEstimatorSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.HostMonitorSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.HostMonitoringSettings;
import org.inria.myriads.snoozenode.estimator.api.Estimator;


import com.google.common.primitives.Doubles;

/**
 * @author msimonin
 *
 */
public class MonitoringEstimator
{
    /** cpu estimator.*/
    Estimator estimatorCpu_; 
    
    /** memory estimator.*/
    Estimator estimatorMemory_;
    
    /** rx estimator.*/
    Estimator estimatorRx_;
    
    /** tx estimator.*/
    Estimator estimatorTx_;

    /** hostEstimator.*/
    Map<String, Estimator> hostEstimators_;
    
    
    /**
     * 
     */
    public MonitoringEstimator(
            EstimatorSettings estimatorSettings,
            HostMonitoringSettings hostMonitoringSettings
            )
    {
        estimatorCpu_ = MonitoringEstimatorFactory.newEstimator(estimatorSettings.getPolicy().getCPU().toString());
        estimatorMemory_ = MonitoringEstimatorFactory.newEstimator(estimatorSettings.getPolicy().getMemory().toString());
        estimatorRx_ = MonitoringEstimatorFactory.newEstimator(estimatorSettings.getPolicy().getNetwork().toString());
        estimatorTx_ = MonitoringEstimatorFactory.newEstimator(estimatorSettings.getPolicy().getNetwork().toString());
        hostEstimators_ = new HashMap<String, Estimator>();
        for (HostMonitorSettings hostMonitorSetting : hostMonitoringSettings.getHostMonitorSettings().values())
        {
            for (Resource resource : hostMonitorSetting.getResources())
            {
                HostEstimatorSettings hostEstimatorSettings = hostMonitorSetting.getEstimators().get(resource.getName());
                hostEstimators_.put(resource.getName(), MonitoringEstimatorFactory.newEstimator(hostEstimatorSettings));
            }
        }
    }

    public List<Double> estimateVirtualMachineUtilization(VirtualMachineMetaData virtualMachineMetaData)
    {
        LRUCache<Long, VirtualMachineMonitoringData> usedCapacity = virtualMachineMetaData.getUsedCapacity();
        
        List<Double> cpuUtilization = getUtilization(usedCapacity, Globals.CPU_UTILIZATION_INDEX);
        List<Double> memUtilization = getUtilization(usedCapacity, Globals.MEMORY_UTILIZATION_INDEX);        
        List<Double> rxUtilization = getUtilization(usedCapacity, Globals.NETWORK_RX_UTILIZATION_INDEX);
        List<Double> txUtilization = getUtilization(usedCapacity, Globals.NETWORK_TX_UTILIZATION_INDEX);
        double cpuEstimation = estimatorCpu_.estimate(cpuUtilization);
        double memEstimation = estimatorMemory_.estimate(memUtilization);
        double rxEstimation = estimatorRx_.estimate(rxUtilization);
        double txEstimation = estimatorTx_.estimate(txUtilization);
        
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
            double estimation = hostEstimators_.get(resource.getName()).estimate(history);
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
        
}
