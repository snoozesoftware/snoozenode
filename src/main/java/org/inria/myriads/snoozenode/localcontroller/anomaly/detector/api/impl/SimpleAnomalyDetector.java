package org.inria.myriads.snoozenode.localcontroller.anomaly.detector.api.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.MonitoringThresholds;
import org.inria.myriads.snoozecommon.communication.localcontroller.Resource;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.util.MathUtils;
import org.inria.myriads.snoozenode.localcontroller.anomaly.detector.api.AnomalyDetector;
import org.inria.myriads.snoozenode.localcontroller.monitoring.enums.LocalControllerState;
import org.inria.myriads.snoozenode.localcontroller.monitoring.estimator.MonitoringEstimator;
import org.inria.myriads.snoozenode.localcontroller.monitoring.threshold.ThresholdCrossingDetector;
import org.inria.myriads.snoozenode.util.ThresholdUtils;
import org.inria.myriads.snoozenode.util.UtilizationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Doubles;

/**
 * @author msimonin
 *
 */
public class SimpleAnomalyDetector implements AnomalyDetector
{

    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(SimpleAnomalyDetector.class);
    
    /** Node parameters. */
    private MonitoringThresholds monitoringThresholds_;

    /** Total capacity. */
    private List<Double> totalCapacity_;
    
    /** Monitoring estimator.*/
    private MonitoringEstimator estimator_;

    private LocalControllerDescription localController_;
    
    /**
     * Constructor.
     * 
     * @param monitoringThresholds  The monitoring thresholds
     * @param totalCapacity         The total local controller capacity
     */
    public SimpleAnomalyDetector(
                        MonitoringEstimator estimator,
                        LocalControllerDescription localController
            )
    {
        Guard.check(estimator, localController);
        log_.debug("Initializing the simple threshold crossing detector");
        estimator_ = estimator;
        localController_ = localController;
        monitoringThresholds_ = localController_.getThresholds();
        totalCapacity_ = localController_.getTotalCapacity();
    }
    
    @Override
    public LocalControllerState detectAnomaly(Map<String, Resource> hostResources, List<VirtualMachineMetaData> virtualMachines)
    {
        List<Double> virtualMachinesUtilization = computeVirtualMachinesUtilization(virtualMachines);
        Map<String, Double> hostEstimation = estimator_.estimateHostUtilization(hostResources);
        log_.debug("Total host utilization is " + hostEstimation);        
        log_.debug("Total virtual machines utilization is " + virtualMachinesUtilization);
        return startThresholdCrossingDetection(virtualMachinesUtilization);
    }
          
    /**
     * Starts the threshold crossing detection.
     *  
     * @param hostUtilization     The host utilization
     * @param monitoringData      The monitoring data
     * @return                    true if underloaded, false otherwise
     */
    private LocalControllerState startThresholdCrossingDetection(List<Double> hostUtilization)
    {
        Guard.check(hostUtilization);
        log_.debug("Starting threshold crossing detection");
        
        double cpuUtilization = UtilizationUtils.getCpuUtilization(hostUtilization) / 
                                UtilizationUtils.getCpuUtilization(totalCapacity_);        
        double memoryUtilization = UtilizationUtils.getMemoryUtilization(hostUtilization) / 
                                   UtilizationUtils.getMemoryUtilization(totalCapacity_);        
        double networkRxUtilization = UtilizationUtils.getNetworkRxUtilization(hostUtilization) / 
                                      UtilizationUtils.getNetworkRxUtilization(totalCapacity_);
        double networkTxUtilization = UtilizationUtils.getNetworkTxUtilization(hostUtilization) / 
                                      UtilizationUtils.getNetworkTxUtilization(totalCapacity_);
        log_.debug(String.format("Normalized CPU: %f, " +
                                 "Memory: %f, " +
                                 "Network Rx: %f, and " +
                                 "Network Tx: %f utilization", 
                                 cpuUtilization, 
                                 memoryUtilization, 
                                 networkRxUtilization,
                                 networkTxUtilization));
        
        boolean isOverloaded = detectOverloadSituation(cpuUtilization, 
                                                       memoryUtilization, 
                                                       networkRxUtilization, 
                                                       networkTxUtilization);
        if (isOverloaded)
        {
            log_.debug("OVERLOAD situation detected!");
//            monitoringData.setState(LocalControllerState.OVERLOADED);
            return LocalControllerState.OVERLOADED;
        }
                
        boolean isUnderloaded = detectUnderloadSituation(cpuUtilization, 
                                                         memoryUtilization, 
                                                         networkRxUtilization, 
                                                         networkTxUtilization);
        if (isUnderloaded)
        {
            log_.debug("UNDERLOAD situation detected!");
//            monitoringData.setState(LocalControllerState.UNDERLOADED);
            return LocalControllerState.UNDERLOADED;
        }
        
        return LocalControllerState.STABLE;        
    }
    
    /**
     * Checks thresholds for underload situation.
     *  
     * @param cpuUtilization            The CPU utilization
     * @param memoryUtilization         The memory utilization
     * @param networkRxUtilization      The network Rx utilization
     * @param networkTxUtilization      The network Tx utilization
     * @return                          true if underloaded, false otherwise
     */
    private boolean detectUnderloadSituation(double cpuUtilization, 
                                             double memoryUtilization,
                                             double networkRxUtilization,
                                             double networkTxUtilization)
    {
        Guard.check(cpuUtilization, memoryUtilization, networkRxUtilization, networkTxUtilization);        
        boolean cpuUnderload = cpuUtilization < 
                               ThresholdUtils.getMinThreshold(monitoringThresholds_.getCpu());
        boolean memoryUnderload = memoryUtilization <
                                  ThresholdUtils.getMinThreshold(monitoringThresholds_.getMemory());
        boolean networkRxUnderload = networkRxUtilization < 
                                     ThresholdUtils.getMinThreshold(monitoringThresholds_.getNetwork());
        boolean networkTxUnderload = networkTxUtilization < 
                                     ThresholdUtils.getMinThreshold(monitoringThresholds_.getNetwork());
        
        if (cpuUnderload && memoryUnderload && networkRxUnderload && networkTxUnderload)
        {
            return true;
        }
        
        return false;
    }
    
    /**
     * Checks thresholds for overload situation.
     *  
     * @param cpuUtilization            The CPU utilization
     * @param memoryUtilization         The memory utilization
     * @param networkRxUtilization      The network Rx utilization
     * @param networkTxUtilization      The network Tx utilization
     * @return                          true if overloaded, false otherwise
     */
    private boolean detectOverloadSituation(double cpuUtilization, 
                                            double memoryUtilization,
                                            double networkRxUtilization,
                                            double networkTxUtilization)
    {
        Guard.check(cpuUtilization, memoryUtilization, networkRxUtilization, networkTxUtilization);        
        boolean cpuOverload = cpuUtilization >
                              ThresholdUtils.getMaxThreshold(monitoringThresholds_.getCpu());
        boolean memoryOverload = memoryUtilization >
                                 ThresholdUtils.getMaxThreshold(monitoringThresholds_.getMemory());
        boolean networkRxOverload = networkRxUtilization > 
                                    ThresholdUtils.getMaxThreshold(monitoringThresholds_.getNetwork());
        boolean networkTxOverload = networkTxUtilization > 
                                    ThresholdUtils.getMaxThreshold(monitoringThresholds_.getNetwork());

        if (cpuOverload || memoryOverload || networkRxOverload || networkTxOverload)
        {
            return true;
        }
        
        return false;
    }
    
    private List<Double> computeVirtualMachinesUtilization(List<VirtualMachineMetaData> virtualMachines)
    {
        ArrayList<Double> virtualMachinesUtilization = MathUtils.createEmptyVector();
        for (VirtualMachineMetaData virtualMachine : virtualMachines)
        {
            
            List<Double> virtualMachineEstimation = estimator_.estimateVirtualMachineUtilization(virtualMachine);
            virtualMachinesUtilization = MathUtils.addVectors(virtualMachineEstimation, virtualMachinesUtilization);
        }
        return virtualMachinesUtilization;
    }
    



}
