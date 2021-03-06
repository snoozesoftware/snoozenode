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
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPlan;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPolicy;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.util.SortUtils;
import org.inria.myriads.snoozenode.util.ManagementUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the round-robin virtual machine placement policy.
 * 
 * @author Eugen Feller
 */
public final class RoundRobin implements PlacementPolicy
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(RoundRobin.class);

    /** Running index variable. */
    private int runningIndex_;

    /** Resource demand estimator. */
    private ResourceDemandEstimator estimator_;

    /**
     * Constructor.
     * 
     * @param estimator     The estimator
     */
    public RoundRobin(ResourceDemandEstimator estimator)
    {
        Guard.check(estimator);
        log_.debug("Initializing round robin virtual machine placement policy");
        estimator_ = estimator;
    }

    /**
     * Places a single virtual machine.
     * 
     * @param virtualMachines   The virtual machines
     * @param localControllers  The local controller descriptions
     * @return                  Placement Plan
     */
    @Override
    public PlacementPlan place(List<VirtualMachineMetaData> virtualMachines,
            List<LocalControllerDescription> localControllers)
    {
        Guard.check(virtualMachines, localControllers);
        log_.debug(String.format("Placing %d virtual machines", virtualMachines.size()));

        Map<String, LocalControllerDescription> targetLocalControllers = 
                new HashMap<String, LocalControllerDescription>();
        List<VirtualMachineMetaData> unassignedVirtualMachines = new ArrayList<VirtualMachineMetaData>();      
        SortUtils.sortLocalControllersDecreasing(localControllers, estimator_);

        for (VirtualMachineMetaData virtualMachine : virtualMachines)
        {
            String virtualMachineId = virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
            
            boolean isAssigned = false;
            for (@SuppressWarnings("unused") LocalControllerDescription localController : localControllers)
            {
                int nextLocalControllerIndex = runningIndex_ % localControllers.size();
                LocalControllerDescription nextLocalController = localControllers.get(nextLocalControllerIndex);
                runningIndex_++;

                if (estimator_.hasEnoughLocalControllerCapacity(virtualMachine, nextLocalController))
                {
                    log_.debug(String.format("Local controller %s has enough capacity for virtual machine: %s!",
                                             nextLocalController.getId(), 
                                             virtualMachineId));
                    
                    nextLocalController.getVirtualMachineMetaData().put(virtualMachineId, virtualMachine);
                    nextLocalController.getAssignedVirtualMachines().add(virtualMachine);
                    String localControllerId = nextLocalController.getId();
                    if (!targetLocalControllers.containsKey(localControllerId))
                    {
                        log_.debug(String.format("Adding local controller %s to the used list", localControllerId));
                        targetLocalControllers.put(localControllerId, nextLocalController);
                    }
                    
                    isAssigned = true;
                    break;
                }
            }
            
            if (!isAssigned)
            {
                log_.debug(String.format("No suitable local controller to host the virtual machine: %s", 
                                         virtualMachineId));
                ManagementUtils.updateVirtualMachineMetaData(
                        virtualMachine, 
                        VirtualMachineStatus.ERROR, 
                        VirtualMachineErrorCode.NOT_ENOUGH_LOCAL_CONTROLLER_CAPACITY);
                unassignedVirtualMachines.add(virtualMachine);
            }
        }

        List<LocalControllerDescription> usedLocalControllers = 
                new ArrayList<LocalControllerDescription>(targetLocalControllers.values());
        PlacementPlan placementPlan = new PlacementPlan(usedLocalControllers, unassignedVirtualMachines);
        return placementPlan;
    }
}
