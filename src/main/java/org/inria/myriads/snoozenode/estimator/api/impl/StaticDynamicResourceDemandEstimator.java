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
package org.inria.myriads.snoozenode.estimator.api.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.groupmanager.summary.GroupManagerSummaryInformation;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerStatus;
import org.inria.myriads.snoozecommon.communication.localcontroller.MonitoringThresholds;
import org.inria.myriads.snoozecommon.communication.localcontroller.Resource;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.NetworkDemand;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.VirtualMachineMonitoringData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineErrorCode;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineStatus;
import org.inria.myriads.snoozecommon.util.MathUtils;
import org.inria.myriads.snoozecommon.util.MonitoringUtils;
import org.inria.myriads.snoozenode.configurator.estimator.HostEstimatorSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.HostMonitorSettings;
import org.inria.myriads.snoozenode.configurator.submission.PackingDensity;
import org.inria.myriads.snoozenode.estimator.ResourceEstimatorFactory;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.exception.ResourceDemandEstimatorException;
import org.inria.myriads.snoozenode.groupmanager.estimator.api.HostMonitoringEstimator;
import org.inria.myriads.snoozenode.groupmanager.estimator.api.VirtualMachineMonitoringEstimator;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.sort.SortNorm;
import org.inria.myriads.snoozenode.util.ThresholdUtils;
import org.inria.myriads.snoozenode.util.UtilizationUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource demand estimator.
 * 
 * @author Eugen Feller
 */
public class StaticDynamicResourceDemandEstimator extends ResourceDemandEstimator
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(StaticDynamicResourceDemandEstimator.class);
        
    /** CPU demand estimator. */
    private VirtualMachineMonitoringEstimator cpuDemandEstimator_;
    
    /** Memory demand estimator. */
    private VirtualMachineMonitoringEstimator memoryDemandEstimator_;
     
    /** Rx demand estimator. */
    private VirtualMachineMonitoringEstimator rxDemandEstimator_;
    
    /** Rx demand estimator. */
    private VirtualMachineMonitoringEstimator txDemandEstimator_;
    
    /** hostEstimator.*/
    private Map<String, HostMonitoringEstimator> hostEstimators_;
    
    /** Resource demand estimator settings. */
    private int numberOfMonitoringEntries_;
    
    /** Monitoring thresholds. */
    private MonitoringThresholds monitoringThresholds_;
    
    /** Sort norm. (from options)*/
    private SortNorm sortNorm_;
    
    /** Consider static capacity. (from options) */
    private boolean isStatic_;

    /** packing density.*/
    private PackingDensity packingDensity_;
    
    

    /**
     * Constructor.
     */
    public StaticDynamicResourceDemandEstimator()
    {
        log_.debug("Building the static/dynamic resource demand estimator");
        packingDensity_ = new PackingDensity();
        hostEstimators_ = new HashMap<String, HostMonitoringEstimator>();
    }
    
    @Override
    public void initialize() throws ResourceDemandEstimatorException
    {
        String sortNormString = estimatorSettings_.getOptions().get("sortNorm");
        // TODO avoid this boilerplate code: 
        if (sortNormString == null)
        {
            throw new ResourceDemandEstimatorException("sort norm is null");
        }
        sortNorm_ = SortNorm.valueOf(sortNormString);

        String isStaticString = estimatorSettings_.getOptions().get("static");
        if (isStaticString == null)
        {
            throw new ResourceDemandEstimatorException("static flag null");
        }
        isStatic_ = Boolean.valueOf(isStaticString);
        
        String packingCpuString = estimatorSettings_.getOptions().get("packingDensityCpu");
        if (packingCpuString == null)
        {
            throw new ResourceDemandEstimatorException("packing density for CPU is missing");
        }
        packingDensity_.setMemory(Double.valueOf(packingCpuString));
        
        String packingMemString = estimatorSettings_.getOptions().get("packingDensityMemory");
        if (packingMemString == null)
        {
            throw new ResourceDemandEstimatorException("packing density for CPU is missing");
        }
        packingDensity_.setCPU(Double.valueOf(packingMemString));
        
        String packingNetworkString = estimatorSettings_.getOptions().get("packingDensityNetwork");
        if (packingNetworkString == null)
        {
            throw new ResourceDemandEstimatorException("packing density for CPU is missing");
        }
        packingDensity_.setNetwork(Double.valueOf(packingNetworkString));
        
        monitoringThresholds_ = monitoringSettings_.getThresholds();
        numberOfMonitoringEntries_ = estimatorSettings_.getNumberOfMonitoringEntries();
        
        // TODO move this to super class ?
        // Monitoring estimator.
        cpuDemandEstimator_ = ResourceEstimatorFactory.newVirtualMachineDemandCpuEstimator(monitoringSettings_.getEstimatorPolicy().getCpuEstimatorName());
        memoryDemandEstimator_ = ResourceEstimatorFactory.newVirtualMachineDemandMemEstimator(monitoringSettings_.getEstimatorPolicy().getMemoryEstimatorName());
        rxDemandEstimator_ = ResourceEstimatorFactory.newVirtualMachineDemandRxEstimator(monitoringSettings_.getEstimatorPolicy().getNetworkEstimatorName());
        txDemandEstimator_ = ResourceEstimatorFactory.newVirtualMachineDemandTxEstimator(monitoringSettings_.getEstimatorPolicy().getNetworkEstimatorName());
        
        // HostMonitoring estimator.
        for (HostMonitorSettings hostMonitorSetting : hostMonitoringSettings_.getHostMonitorSettings().values())
        {
            for (Resource resource : hostMonitorSetting.getResources())
            {
                HostEstimatorSettings estimatorSetting = hostMonitorSetting.getEstimators().get(resource.getName());                
                hostEstimators_.put(resource.getName(), ResourceEstimatorFactory.newHostMonitoringEstimator(estimatorSetting));
            }
        }
    }
    
    
    /**
     * Returns the group manager capacity.
     * 
     * @param summaryData   The group manager summary data
     * @return              The group manager capacity
     */
    private List<Double> getGroupManagerCapacity(GroupManagerSummaryInformation summaryData)
    {
        List<Double> capacity; 
        if (!isStatic_)
        {
            capacity = summaryData.getUsedCapacity();  
            log_.debug(String.format("Considering used group manager capacity: %s", capacity));
            return capacity;
        } 
        
        capacity = summaryData.getRequestedCapacity();
        log_.debug(String.format("Considering requested group manager capacity: %s", capacity));
        return capacity;        
    }
    
    /**
     * Verifies weather a virtual machine fits into the group manager based
     * on its summary information.
     * 
     * @param virtualMachine     The virtual machine meta data
     * @param groupManager       The group manager data
     * @return                   true if everything ok, false otherwise
     */
    public boolean hasEnoughGroupManagerCapacity(VirtualMachineMetaData virtualMachine,
                                                 GroupManagerDescription groupManager)
    {
        Map<Long, GroupManagerSummaryInformation> dataMap = groupManager.getSummaryInformation();
        if (dataMap.size() == 0)
        {
            log_.debug("Group manager summary information not available!");
            return false;
        }
        
        GroupManagerSummaryInformation summaryData = MonitoringUtils.getLatestSummaryInformation(dataMap);
        List<Double> originalCapacity = getGroupManagerCapacity(summaryData);
        List<Double> activeCapacity = summaryData.getActiveCapacity();   
        List<Double> passiveCapacity = summaryData.getPassiveCapacity();
        List<Double> requestedCapacity = virtualMachine.getRequestedCapacity();
        ArrayList<Double> newCapacity = MathUtils.addVectors(originalCapacity, requestedCapacity);
        log_.debug(String.format("GM active: %s, passive: %s, VM requested: %s, new requested/used: %s capacity", 
                                 activeCapacity, passiveCapacity, requestedCapacity, newCapacity));
        
        String virtualMachineId = virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
        if (MathUtils.vectorCompareIsLess(newCapacity, activeCapacity))
        {
            log_.debug(String.format("Virtual machine: %s fits into the ACTIVE capacity of group manager: %s",
                                     virtualMachineId, 
                                     groupManager.getId()));
            if (!isStatic_)
            {
                summaryData.setUsedCapacity(newCapacity);
            } else
            {
                summaryData.setRequestedCapacity(newCapacity);
            }
            return true;
        }
        
        if (MathUtils.vectorCompareIsLess(requestedCapacity, passiveCapacity))
        {
            log_.debug(String.format("Virtual machine: %s fits into the PASSIVE capacity of group manager: %s",
                                     virtualMachineId, 
                                     groupManager.getId()));
            
            ArrayList<Double> newPassiveCapacity = MathUtils.substractVector(passiveCapacity, requestedCapacity);
            summaryData.setPassiveCapacity(newPassiveCapacity);
            return true;
        }
        virtualMachine.setStatus(VirtualMachineStatus.ERROR);
        virtualMachine.setErrorCode(VirtualMachineErrorCode.NOT_ENOUGH_GROUP_MANAGER_CAPACITY);
        return false;
    } 
    

    /**
     * Creates new requested capacity.
     * 
     * @param requestedCapacity         The requested capacity
     * @param packingDensity            The packing density
     * @return                          The new capacity vector
     */
    private List<Double> applyPackingDensity(List<Double> requestedCapacity, PackingDensity packingDensity)
    {        
        double cpu = UtilizationUtils.getCpuUtilization(requestedCapacity) * packingDensity.getCPU();
        double memory = UtilizationUtils.getMemoryUtilization(requestedCapacity) * packingDensity.getMemory();
        double networkRx = UtilizationUtils.getNetworkRxUtilization(requestedCapacity) * packingDensity.getNetwork();
        double networkTx = UtilizationUtils.getNetworkTxUtilization(requestedCapacity) * packingDensity.getNetwork();
        
        NetworkDemand networkDemand = new NetworkDemand(networkRx, networkTx);
        ArrayList<Double> newRequestedCapacity = MathUtils.createCustomVector(cpu, memory, networkDemand);
        return newRequestedCapacity;
    }   
    
    /**
     * Computes the virtual machine requested capacity.
     * 
     * @param virtualMachine         The virtual machine
     * @return                       The requested capacity
     */
    private List<Double> computeRequestedVirtualMachineCapacity(VirtualMachineMetaData virtualMachine)
    {
        List<Double> requestedCapacity = virtualMachine.getRequestedCapacity();
        requestedCapacity = applyPackingDensity(requestedCapacity, packingDensity_);
        log_.debug("" + requestedCapacity);
        log_.debug("" + packingDensity_.getCPU() + "- " + packingDensity_.getMemory() + "-" + packingDensity_.getNetwork());
        return requestedCapacity;
    }
    
    /**
     * Computes virtual machine capacity.
     * 
     * @param virtualMachine    The virtual machine meta data
     * @return                  The virtual machine capacity
     */
    private List<Double> computeVirtualMachineCapacity(VirtualMachineMetaData virtualMachine)
    {        
        Map<Long, VirtualMachineMonitoringData> capacity = virtualMachine.getUsedCapacity();  
        if (capacity.size() == 0 || isStatic_)
        {
            log_.debug("No virtual machine used capacity information available or static mode enabled! " +
                    "Taking requested!");
            List<Double> requestedCapacity = computeRequestedVirtualMachineCapacity(virtualMachine);
            return requestedCapacity;
        } 
        
        String virtualMachineId = virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
        List<Double> estimatedUsedCapacity = estimateVirtualMachineResourceDemand(virtualMachine);
        log_.debug(String.format("Virtual machine %s used capacity is: %s",
                                 virtualMachineId,
                                 estimatedUsedCapacity));    
        return estimatedUsedCapacity;
    }
    
    /** 
     * Checks whether a local controller has enough active capacity to host the VM.
     * 
     * @param virtualMachine        The virtual machine meta data        
     * @param localController       The local controller description
     * @return                      true if enough capacity vailable, false otherwise
     */
    public boolean hasEnoughLocalControllerCapacity(VirtualMachineMetaData virtualMachine, 
                                                    LocalControllerDescription localController)
    {                   
        //Monitoring estimator
        List<Double> virtualMachineCapacity = computeVirtualMachineCapacity(virtualMachine);
        List<Double> localControllerCapacity = computeLocalControllerCapacity(localController);
        List<Double> newLocalControllerCapacity = MathUtils.addVectors(virtualMachineCapacity, 
                                                                       localControllerCapacity);  
        log_.debug(String.format("Local controller %s capacity: %s, VM capacity: %s, new LC capacity: %s", 
                                  localController.getStatus(), 
                                  localControllerCapacity, 
                                  virtualMachineCapacity,
                                  newLocalControllerCapacity));
        
        List<Double> localControllerTotalCapacity = localController.getTotalCapacity();
        if (!checkCapacityConstraints(newLocalControllerCapacity, localControllerTotalCapacity))
        {   
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks the lcoal controller capacity constraints.
     * 
     * @param localControllerUsedCapacity       The used capacity
     * @param localControllerTotalCapacity      The total capacity
     * @return                                  true if everyting ok, false otherwise
     */
    private boolean checkCapacityConstraints(List<Double> localControllerUsedCapacity, 
                                             List<Double> localControllerTotalCapacity)
    {
        log_.debug(String.format("Checking capacity constraints for local controller used and total capacity: " +
                                 "%s / %s", localControllerUsedCapacity, localControllerTotalCapacity)); 
                
        double cpuMid = ThresholdUtils.getMidThreshold(monitoringThresholds_.getCpu());
        double memoryMid = ThresholdUtils.getMidThreshold(monitoringThresholds_.getMemory());
        double networkRxMid = ThresholdUtils.getMidThreshold(monitoringThresholds_.getNetwork());
        double networkTxMid = ThresholdUtils.getMidThreshold(monitoringThresholds_.getNetwork());
        
        double numberOfPhysicalCores = UtilizationUtils.getCpuUtilization(localControllerTotalCapacity);
        
        double cpuUtilization = UtilizationUtils.getCpuUtilization(localControllerUsedCapacity);
        if (cpuUtilization > cpuMid * numberOfPhysicalCores)
        {
            return false;
        } 
        
        double memoryUtilization = UtilizationUtils.getMemoryUtilization(localControllerUsedCapacity);
        double memoryTotal =  UtilizationUtils.getMemoryUtilization(localControllerTotalCapacity);
        if (memoryUtilization > 
            memoryTotal * memoryMid)
        {
            return false;
        }
        
        if (UtilizationUtils.getNetworkRxUtilization(localControllerUsedCapacity) >
            UtilizationUtils.getNetworkRxUtilization(localControllerTotalCapacity) * networkRxMid)
        {
            return false;
        }
        
        if (UtilizationUtils.getNetworkTxUtilization(localControllerUsedCapacity) >
            UtilizationUtils.getNetworkTxUtilization(localControllerTotalCapacity) * networkTxMid)
        {
            return false;
        }

        return true;        
    }
        
    /**
     * Estimates the local controller utilization.
     * 
     * @param localController    The local controller description
     * @return                   The estimated local controller utilization
     */
    public ArrayList<Double> computeLocalControllerCapacity(LocalControllerDescription localController)
    {
       log_.debug(String.format("Computing local controller %s capacity", localController.getId()));
       
       ArrayList<Double> capacity = MathUtils.createEmptyVector();
       for (VirtualMachineMetaData virtualMachine : localController.getVirtualMachineMetaData().values())
       {
           String virtualMachineId = virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
           Map<Long, VirtualMachineMonitoringData> monitoringData = virtualMachine.getUsedCapacity();
           if (monitoringData.size() == 0 || isStatic_)
           {
               capacity = MathUtils.addVectors(computeRequestedVirtualMachineCapacity(virtualMachine), capacity);
               continue;
           }
           ArrayList<Double> estimatedData = estimateVirtualMachineResourceDemand(virtualMachine);
           log_.debug(String.format("Estimated virtual machine %s resource demand is %s", 
                                     virtualMachineId,
                                     estimatedData));
           capacity = MathUtils.addVectors(estimatedData, capacity);
       }
       
       log_.debug(String.format("Local controller utilization is: %s", capacity));
       return capacity;
    }
    
    /**
     * Estimates virtual machine resource demands.
     * 
     * @param virtualMachine     The virtual machine meta data
     * @return                   The estimated virtual machine monitoring data
     */
    public ArrayList<Double> estimateVirtualMachineResourceDemand(VirtualMachineMetaData virtualMachine) 
    {              
        if (isStatic_)
        {
            return virtualMachine.getRequestedCapacity();
        }
        
        Map<Long, VirtualMachineMonitoringData> virtualMachineHistory = virtualMachine.getUsedCapacity();
        double cpuUtilization = cpuDemandEstimator_.estimate(virtualMachineHistory);
        double memoryUtilization = memoryDemandEstimator_.estimate(virtualMachineHistory);
        //NetworkDemand networkUtilization = networkDemandEstimator_.estimate(virtualMachineHistory);
        double rxUtilization = rxDemandEstimator_.estimate(virtualMachineHistory);
        double txUtilization = txDemandEstimator_.estimate(virtualMachineHistory);
        ArrayList<Double> estimates = MathUtils.createCustomVector(cpuUtilization, 
                                                                   memoryUtilization, 
                                                                   rxUtilization,
                                                                   txUtilization);
        return estimates;
    }

    @Override
    public Map<String, Double> estimateHostResourceUtilization(Map<String, Resource> hostUtilizationHistory) 
    {              
        Map<String, Double> hostUtilization = new HashMap<String, Double>(); 

        for (Resource resource : hostUtilizationHistory.values())
        {
            log_.debug(
                    String.format(
                            "Computing estimation for resource %s and history %s ",
                            resource.getName(),
                            resource.getHistory()));
            double estimation = hostEstimators_.get(resource.getName()).estimate(resource);
            hostUtilization.put(resource.getName(), estimation);
        }
        return hostUtilization;
    }
    
    /**
     * Computes the used capacity of the group manager.
     * 
     * @param localControllers  The local controllers
     * @return                  The used capacity
     */
    private ArrayList<Double> computeUsedGroupManagerCapacity(List<LocalControllerDescription> localControllers)
    {        
        ArrayList<Double> usedCapacity = MathUtils.createEmptyVector();        
        for (LocalControllerDescription localController : localControllers) 
        {  
            usedCapacity = MathUtils.addVectors(usedCapacity, computeLocalControllerCapacity(localController));
        }      
        
        return usedCapacity;
    }
    
    /**
     * Computes the total group manager capacity.
     * 
     * @param descriptions  The descriptions
     * @param status        The status
     * @return              The local controller capacity
     */
    private ArrayList<Double> computeTotalGroupManagerCapacity(List<LocalControllerDescription> descriptions,
                                                               LocalControllerStatus status)
    {        
        ArrayList<Double> totalCapacity = MathUtils.createEmptyVector();          
        for (LocalControllerDescription localControllerDescription : descriptions) 
        {
            if (localControllerDescription.getStatus().equals(status))
            {
                ArrayList<Double> localControllerCapacity = localControllerDescription.getTotalCapacity();       
                totalCapacity = MathUtils.addVectors(totalCapacity, localControllerCapacity);
            }
        }
        
        return totalCapacity;
    }
    
    /**
     * Returns the current group manager summary data.
     * 
     * @param localControllers      The list of local controllers
     * @return                      The group manager summary information
     */
    public synchronized GroupManagerSummaryInformation 
        generateGroupManagerSummaryInformation(ArrayList<LocalControllerDescription> localControllers) 
    {        
        ArrayList<Double> requestedCapacity = computeRequestedGroupManagerCapacity(localControllers);        
        ArrayList<Double> usedCapacity = computeUsedGroupManagerCapacity(localControllers);
        ArrayList<Double> totalActiveCapacity = computeTotalGroupManagerCapacity(localControllers,
                                                                                 LocalControllerStatus.ACTIVE);
        ArrayList<Double> totalPassiveCapacity = computeTotalGroupManagerCapacity(localControllers,
                                                                                  LocalControllerStatus.PASSIVE);
       
        GroupManagerSummaryInformation summary = new GroupManagerSummaryInformation();
        summary.setActiveCapacity(totalActiveCapacity);
        summary.setPassiveCapacity(totalPassiveCapacity);
        summary.setRequestedCapacity(requestedCapacity);
        summary.setUsedCapacity(usedCapacity);
    
        return summary;
    }

    /**
     * Computes the requested local controller capacity.
     * 
     * @param localController   The local controller description
     * @return                  The requested capacity
     */
    private ArrayList<Double> computeRequestedLocalControllerCapacity(LocalControllerDescription localController)
    {
        ArrayList<Double> requestedCapacity = MathUtils.createEmptyVector();
        Map<String, VirtualMachineMetaData> virtualMachines = localController.getVirtualMachineMetaData();
        for (VirtualMachineMetaData virtualMachine : virtualMachines.values())
        {
            requestedCapacity = MathUtils.addVectors(requestedCapacity, 
                                                     computeRequestedVirtualMachineCapacity(virtualMachine));
        }
        
        return requestedCapacity;
    }
    
    /**
     * Computes the requested capacity.
     * 
     * @param localControllers  The local controllers
     * @return                  The requested capacity
     */
    private ArrayList<Double> computeRequestedGroupManagerCapacity(List<LocalControllerDescription> localControllers) 
    {        
        ArrayList<Double> requestedCapacity = MathUtils.createEmptyVector();        
        for (LocalControllerDescription localController : localControllers) 
        {  
            requestedCapacity = MathUtils.addVectors(requestedCapacity,
                                                     computeRequestedLocalControllerCapacity(localController));
        }  
        
        return requestedCapacity;
    }

    /**
     * Computes the allowed utilization.
     * 
     * @param description    The local controller description
     * @return               The allowed capacity
     */
    public ArrayList<Double> computeMaxAllowedCapacity(LocalControllerDescription description) 
    {
        ArrayList<Double> totalCapacity = description.getTotalCapacity();

        double allowedCPU = UtilizationUtils.getCpuUtilization(totalCapacity) *
                            ThresholdUtils.getMidThreshold(monitoringThresholds_.getCpu());
        
        double allowedMemory = UtilizationUtils.getMemoryUtilization(totalCapacity) *
                               ThresholdUtils.getMidThreshold(monitoringThresholds_.getMemory());
        
        double allowedNetworkRx = UtilizationUtils.getNetworkRxUtilization(totalCapacity) *
                                  ThresholdUtils.getMidThreshold(monitoringThresholds_.getNetwork());

        double allowedNetworkTx = UtilizationUtils.getNetworkTxUtilization(totalCapacity) *
                                  ThresholdUtils.getMidThreshold(monitoringThresholds_.getNetwork());

        ArrayList<Double> allowedCapacity = new ArrayList<Double>();
        allowedCapacity.add(allowedCPU);
        allowedCapacity.add(allowedMemory);
        allowedCapacity.add(allowedNetworkRx);
        allowedCapacity.add(allowedNetworkTx);   
        return allowedCapacity;
    }
    
    /** 
     * Number of monitoring entries.
     * 
     * @return      The number of monitoring entries
     */
    public int getNumberOfMonitoringEntries() 
    {
        return numberOfMonitoringEntries_;
    }
    
    /**
     * Returns the sort norm.
     * 
     * @return      The sort norm
     */
    public SortNorm getSortNorm() 
    {
        return sortNorm_;
    }
}
