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
package org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineErrorCode;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineStatus;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.estimator.api.impl.StaticDynamicResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.estimator.util.EstimatorUtils;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPlan;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPolicy;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.util.SortUtils;
import org.inria.myriads.snoozenode.util.ManagementUtils;
import org.inria.myriads.snoozenode.util.OutputUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the first-fit virtual machine placement policy.
 * 
 * @author Eugen Feller
 */
public final class FirstFit 
    extends PlacementPolicy 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(FirstFit.class);
    
    /**
     * Constructor.
     * 
     * @param estimator     The estimator
     */
    public FirstFit() 
    {
    }
    
    @Override
    public void initialize()
    {
        log_.debug("Initializing first-fit virtual machine placement policy");
    }
    
    /**
     * Places a single virtual machine.
     * 
     * @param virtualMachines      The virtual machines
     * @param localControllers     The local controller descriptions
     * @return                     The placement plan
     */
    @Override
    public PlacementPlan place(List<VirtualMachineMetaData> virtualMachines,
                               List<LocalControllerDescription> localControllers)
    {
        Guard.check(virtualMachines, localControllers);    
        log_.debug(String.format("Placing %d virtual machine", virtualMachines.size()));
               
        SortUtils.sortLocalControllersDecreasing(localControllers, estimator_);
        OutputUtils.printLocalControllers(localControllers);
                
        Map<String, LocalControllerDescription> targetLocalControllers =  
                new HashMap<String, LocalControllerDescription>(); 
        List<VirtualMachineMetaData> unassignedVirtualMachines = new ArrayList<VirtualMachineMetaData>();
        for (VirtualMachineMetaData virtualMachine : virtualMachines)
        {
            String virtualMachineId = virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
            LocalControllerDescription localController = EstimatorUtils.findSuitableLocalController(virtualMachine, 
                                                                                                    localControllers,
                                                                                                    estimator_);
            if (localController == null)                                                       
            {
                log_.debug(String.format("No suitable local controller to host the virtual machine: %s", 
                                         virtualMachineId));
                ManagementUtils.updateVirtualMachineMetaData(
                        virtualMachine, 
                        VirtualMachineStatus.ERROR, 
                        VirtualMachineErrorCode.NOT_ENOUGH_LOCAL_CONTROLLER_CAPACITY);
                
                unassignedVirtualMachines.add(virtualMachine);
                continue;
            }
            
            log_.debug(String.format("Local controller %s has enough capacity to host virtual machine %s!", 
                                     localController.getId(), virtualMachineId));
            localController.getVirtualMachineMetaData().put(virtualMachineId, virtualMachine);
            localController.getAssignedVirtualMachines().add(virtualMachine);
         
            if (!targetLocalControllers.containsKey(localController.getId()))
            {
                targetLocalControllers.put(localController.getId(), localController);
            }
        }
        
        List<LocalControllerDescription> usedLocalControllers = 
                new ArrayList<LocalControllerDescription>(targetLocalControllers.values());
        PlacementPlan placementPlan = new PlacementPlan(usedLocalControllers, unassignedVirtualMachines);
        return placementPlan;
    }


}
