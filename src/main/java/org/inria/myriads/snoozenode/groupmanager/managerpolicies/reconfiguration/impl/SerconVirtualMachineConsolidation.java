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
package org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.estimator.util.EstimatorUtils;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.ReconfigurationPlan;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.ReconfigurationPolicy;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.util.SortUtils;
import org.inria.myriads.snoozenode.util.OutputUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a modified version of the Sercon consolidation algorithm.
 * 
 * @author Eugen Feller
 */
public final class SerconVirtualMachineConsolidation 
    implements ReconfigurationPolicy
{
    /** Logging instance. */
    private static final Logger log_ = LoggerFactory.getLogger(SerconVirtualMachineConsolidation.class);

    /** Resource demand estimator. */
    private ResourceDemandEstimator estimator_;
    
    /**
     * Constructor.
     * 
     * @param estimator    The resource demand estimator
     */
    public SerconVirtualMachineConsolidation(ResourceDemandEstimator estimator)
    {
        log_.debug("Initializing the Sercon VM consolidation algorithm");
        estimator_ = estimator;
    }
    
    /**
     * Computes the consolidated placement.
     * 
     * @param localControllers   The local controller descriptions
     * @return                   The reconfiguration plan
     */
    @Override
    public ReconfigurationPlan reconfigure(List<LocalControllerDescription> localControllers) 
    {
        Guard.check(localControllers);
        log_.debug("Starting to compute the optimized virtual machine placement");
        OutputUtils.printLocalControllers(localControllers);
        
        if (localControllers.size() < 1)
        {
            log_.debug("Not enough local controllers to do consolidation!");
            return null;
        }
        int numberOfActiveLocalControllers = localControllers.size();
        int numberOfReleasedNodes = 0;
                     
        Map<VirtualMachineMetaData, LocalControllerDescription> mapping = 
            new HashMap<VirtualMachineMetaData, LocalControllerDescription>();
        
        
        int runningIndex = localControllers.size();
        int leastLoadedController;
        while (runningIndex != 0 && localControllers.size() > 0)
        {
            leastLoadedController = localControllers.size() - 1;
            log_.debug(String.format("There are still %d localControllers", leastLoadedController));
            try
            {          
                SortUtils.sortLocalControllersDecreasing(localControllers, estimator_);                
                LocalControllerDescription localController = localControllers.get(leastLoadedController);
                log_.debug(String.format("Getting local controller %s description", localController.getId()));
                
                List<VirtualMachineMetaData> virtualMachines = getVirtualMachineMetaData(localController);
                if (virtualMachines.size() == 0)
                {
                    log_.debug("No virtual machines available on this local controller!");
                    localControllers.remove(leastLoadedController);
                    runningIndex--;
                    continue;
                }
                
                OutputUtils.printVirtualMachines(virtualMachines);
                SortUtils.sortVirtualMachinesDecreasing(virtualMachines, estimator_);    
                int numberOfPlacedVirtualMachines = placeVirtualMachines(virtualMachines, localControllers, mapping);
                log_.debug(String.format("Total virtual machines count %d, assigned: %d", 
                                         virtualMachines.size(), numberOfPlacedVirtualMachines));
                
                for (VirtualMachineMetaData virtualMachine : mapping.keySet())
                {
                    log_.debug(String.format("virtual machine %s on localController %s ", 
                            virtualMachine.getVirtualMachineLocation().getVirtualMachineId(),
                            mapping.get(virtualMachine).getControlDataAddress().getAddress()
                            )); 
                }
                
                boolean isEqual = numberOfPlacedVirtualMachines == virtualMachines.size();
                if (isEqual)
                {
                    numberOfReleasedNodes++; 
                    localControllers.remove(leastLoadedController);
                    
                } else
                {
                    removeVirtualMachines(virtualMachines, mapping, localController);
                    
                }
                runningIndex--; 
                log_.debug(String.format("Number of migrations: %d", mapping.size()));
            }
            catch (Exception exception)
            {
                log_.error("Exception during consolidation", exception);
                break;
            }
        }

        log_.debug(String.format("Total number of active local controllers: %d, released local controllers: %d", 
                   numberOfActiveLocalControllers, numberOfReleasedNodes));  
        int numberOfUsedNodes = numberOfActiveLocalControllers - numberOfReleasedNodes;
        ReconfigurationPlan reconfigurationPlan = new ReconfigurationPlan(mapping, 
                                                                          numberOfUsedNodes, 
                                                                          numberOfReleasedNodes);
        return reconfigurationPlan;
    }
    
    /**
     * Places virtual machines.
     * 
     * @param virtualMachines       The virtual machines
     * @param localControllers      The local controllers
     * @param mapping               The mapping
     * @return                      The number of successfully placed virtual machines
     */
    private int placeVirtualMachines(List<VirtualMachineMetaData> virtualMachines,
                                     List<LocalControllerDescription> localControllers,
                                     Map<VirtualMachineMetaData, LocalControllerDescription> mapping)
    {
        log_.debug("Starting to place virtual machines");
        
        int numberOfVirtualMachines = 0;
        for (VirtualMachineMetaData virtualMachine : virtualMachines)
        {
            LocalControllerDescription localController = EstimatorUtils.findSuitableLocalController(virtualMachine, 
                                                                                                    localControllers,
                                                                                                    estimator_);
            if (localController == null)
            {
                log_.debug("No suitable local controller found!");
                continue;
            }
            
            if (localController.getVirtualMachineMetaData().size() == 0)
            {
                log_.debug("This local controller has no virtual machines assigned! Skipping!");
                continue;
            }
            
            numberOfVirtualMachines++;
            String virtualMachineId = virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
            localController.getVirtualMachineMetaData().put(virtualMachineId, virtualMachine);
            mapping.put(virtualMachine, localController);
        }
        
        return numberOfVirtualMachines;
    }
    
    /**
     * Removes virtual machines from current schedule.
     * 
     * @param virtualMachines           The virtual machines
     * @param mapping                   The current mapping
     * @param currentLocalController    The current localController
     */
    private void removeVirtualMachines(List<VirtualMachineMetaData> virtualMachines, 
                                       Map<VirtualMachineMetaData, LocalControllerDescription> mapping,
                                       LocalControllerDescription currentLocalController
                                       ) 
    {
        log_.debug("Starting to remove virtual machines");
        for (VirtualMachineMetaData metaData : virtualMachines)
        {
            String virtualMachineId = metaData.getVirtualMachineLocation().getVirtualMachineId();
            String localControllerSource = metaData.getVirtualMachineLocation().getLocalControllerId();
            if (localControllerSource.equals(currentLocalController.getId()))
            {
                log_.debug(String.format("virtual machine  : %s was initially on " +
                        "the current local controller %s: removing from the migration plan",
                        virtualMachineId,
                        currentLocalController.getControlDataAddress().getAddress()));
                LocalControllerDescription localController = mapping.remove(metaData);
                if (localController != null)
                {
                    localController.getVirtualMachineMetaData().remove(virtualMachineId);
                }
            }
            else
            {
                log_.debug(String.format("virtual machine : %s wasn't on the current local controller %s" +
                        ": not removing", 
                        virtualMachineId,
                        currentLocalController.getControlDataAddress().getAddress()
                        ));
            }
            
            
        }
        
    }

    /**
     * Returns virtual machine meta data.
     * 
     * @param localController      The local controller description
     * @return                     The meta data list
     */
    private List<VirtualMachineMetaData> getVirtualMachineMetaData(LocalControllerDescription localController)
    {
        Guard.check(localController);
        log_.debug(String.format("Returning virtual machine meta data for local controller %s", 
                                 localController.getId()));
        
        HashMap<String, VirtualMachineMetaData> metaData = localController.getVirtualMachineMetaData();
        List<VirtualMachineMetaData> metaDataList = new ArrayList<VirtualMachineMetaData>(metaData.values());
        return metaDataList;
    }
}
