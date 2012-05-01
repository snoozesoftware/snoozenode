/**
 * Copyright (C) 2010-2012 Eugen Feller, INRIA <eugen.feller@inria.fr>
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

import java.util.List;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPolicy;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.util.SortUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the round-robin virtual machine placement policy.
 * 
 * @author Eugen Feller
 */
public final class RoundRobinVirtualMachinePlacement 
    implements PlacementPolicy 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(RoundRobinVirtualMachinePlacement.class);
    
    /** Running index variable. */
    private int runningIndex_;
    
    /** Resource demand estimator. */
    private ResourceDemandEstimator estimator_;
    
    /**
     * Constructor.
     * 
     * @param estimator     The estimator
     */
    public RoundRobinVirtualMachinePlacement(ResourceDemandEstimator estimator) 
    {
        Guard.check(estimator);
        log_.debug("Initializing round robin virtual machine placement policy");
        estimator_ = estimator;
    }

    /**
     * Places a single virtual machine.
     * 
     * @param virtualMachine     The virtual machine meta data
     * @param localControllers   The local controller descriptions
     * @return                   The local controller description
     */
    @Override
    public LocalControllerDescription place(VirtualMachineMetaData virtualMachine,
                                            List<LocalControllerDescription> localControllers)
    {
        Guard.check(virtualMachine, localControllers);    
        String virtualMachineId = virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
        log_.debug(String.format("Placing virtual machine: %s", virtualMachineId));
                
        LocalControllerDescription localController = null;
        SortUtils.sortLocalControllersDecreasing(localControllers, estimator_);
        while (localControllers.iterator().hasNext())
        {
            int nextLocalController = runningIndex_ % localControllers.size();         
            localController = localControllers.get(nextLocalController);
            runningIndex_++;
            String localControllerId = localController.getId();
            if (localControllerId.equals(virtualMachine.getVirtualMachineLocation().getLocalControllerId()))
            {
                log_.debug("Do not consider myself as local controller candidate!");
                continue;
            }
            
            if (estimator_.hasEnoughLocalControllerCapacity(virtualMachine, localController))
            {
                return localController;
            }          
        }
        
        log_.debug(String.format("Local controller %s has enough capacity!", localController.getId()));
        return localController;
    }
}
