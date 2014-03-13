/**
 * Copyright (C) 2010-2013 Eugen Feller, INRIA <eugen.feller@inria.fr>
 *
 * This file is part of Snooze, a scalable, autonomic, and
 * energy-aware virtual machine (VM) management framework.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package org.inria.myriads.snoozenode.localcontroller.monitoring.threshold;

import java.util.ArrayList;
import java.util.List;

import org.inria.myriads.snoozecommon.communication.localcontroller.MonitoringThresholds;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.VirtualMachineMonitoringData;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.util.MathUtils;
import org.inria.myriads.snoozenode.localcontroller.monitoring.enums.LocalControllerState;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.AggregatedVirtualMachineData;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.LocalControllerDataTransporter;
import org.inria.myriads.snoozenode.util.ThresholdUtils;
import org.inria.myriads.snoozenode.util.UtilizationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes the received monitoring data.
 * 
 * @author Eugen Feller
 */
public final class ThresholdCrossingDetector 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(ThresholdCrossingDetector.class);
    
    /** Node parameters. */
    private MonitoringThresholds monitoringThresholds_;

    /** Total capacity. */
    private List<Double> totalCapacity_;
    
    /**
     * Constructor.
     * 
     * @param monitoringThresholds  The monitoring thresholds
     * @param totalCapacity         The total local controller capacity
     */
    public ThresholdCrossingDetector(MonitoringThresholds monitoringThresholds, 
                                     List<Double> totalCapacity)
    {
        Guard.check(monitoringThresholds, totalCapacity);
        log_.debug("Initializing the threshold crossing detector");
        
        monitoringThresholds_ = monitoringThresholds;
        totalCapacity_ = totalCapacity;
    }
    
    /**
     * Detects possible threshold crossings.
     * 
     * @param monitoringData    The monitoring data
     * @return                  true if crossing detected, false otherwise
     */
    public boolean detectThresholdCrossing(LocalControllerDataTransporter monitoringData) 
    {
        Guard.check(monitoringData);
        log_.debug("Starting threshold crossing detection");

        List<AggregatedVirtualMachineData> dataMap = monitoringData.getVirtualMachineAggregatedData();
        if (dataMap == null)
        {
            log_.error("The data map is NULL");
            return false;
        }
        
        List<Double> hostUtilization = computeHostUtilization(dataMap);
        if (hostUtilization == null)
        {
            log_.error("Host utilization is NULL");
            return false;
        }
        
        log_.debug(String.format("Average host utilization is: %s", hostUtilization));                       
        boolean isDetected = startThresholdCrossingDetection(hostUtilization, monitoringData);
        return isDetected;
    }
      
    /**
     * Starts the threshold crossing detection.
     *  
     * @param hostUtilization     The host utilization
     * @param monitoringData      The monitoring data
     * @return                    true if underloaded, false otherwise
     */
    private boolean startThresholdCrossingDetection(List<Double> hostUtilization,
                                                    LocalControllerDataTransporter monitoringData)
    {
        Guard.check(hostUtilization, monitoringData);
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
            monitoringData.setState(LocalControllerState.OVERLOADED);
            return true;
        }
                
        boolean isUnderloaded = detectUnderloadSituation(cpuUtilization, 
                                                         memoryUtilization, 
                                                         networkRxUtilization, 
                                                         networkTxUtilization);
        if (isUnderloaded)
        {
            log_.debug("UNDERLOAD situation detected!");
            monitoringData.setState(LocalControllerState.UNDERLOADED);
            return true;
        }
        
        return false;        
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
    
    /**
     * Computes the host utilization.
     * 
     * @param dataMap       The data map
     * @return              The average host utilization
     */
    private ArrayList<Double> computeHostUtilization(List<AggregatedVirtualMachineData> dataMap)
    {
        Guard.check(dataMap);
        log_.debug("Computing average host utilization");
        
        ArrayList<Double> hostUtilization = MathUtils.createEmptyVector();
        for (AggregatedVirtualMachineData metaData : dataMap) 
        {
            String virtualMachineId = metaData.getVirtualMachineId();
            log_.debug(String.format("Starting to compute average virtual machine %s utilization", 
                                     virtualMachineId));
            
            List<VirtualMachineMonitoringData> dataList = metaData.getMonitoringData();
            // hard coded here
            ArrayList<Double> virtualMachineUtilization = computeAverageVirtualMachineUtilization(dataList);
            log_.debug(String.format("Average virtual machine %s utilization is %s", 
                                     virtualMachineId,
                                     virtualMachineUtilization));
            hostUtilization = MathUtils.addVectors(hostUtilization, virtualMachineUtilization);
        }
        
        return hostUtilization;
    }
    
    /**
     * Computes average virtual machine utilization.
     * 
     * @param monitoringData    The aggregated monitoring data
     * @return                  The utilization vector
     */
    private ArrayList<Double> computeAverageVirtualMachineUtilization(List<VirtualMachineMonitoringData> monitoringData)
    {
        Guard.check(monitoringData);
        log_.debug("Computing average virtual machine utilization");
        
        ArrayList<Double> virtualMachineUtilization = MathUtils.createEmptyVector();
        for (int i = 0; i < monitoringData.size(); i++)
        {
            VirtualMachineMonitoringData virtualMachineData = monitoringData.get(i);
            ArrayList<Double> usedCapacity = virtualMachineData.getUsedCapacity();
            log_.debug(String.format("Adding virtual machine utilization data: %s", usedCapacity));
            virtualMachineUtilization = MathUtils.addVectors(virtualMachineUtilization, usedCapacity);
        }
        
        virtualMachineUtilization = MathUtils.divideVector(virtualMachineUtilization, monitoringData.size());        
        return virtualMachineUtilization;
    }
}
