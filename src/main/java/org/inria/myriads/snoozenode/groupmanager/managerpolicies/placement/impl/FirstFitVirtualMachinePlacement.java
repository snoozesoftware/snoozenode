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
import org.inria.myriads.snoozenode.groupmanager.estimator.util.EstimatorUtils;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPolicy;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.util.SortUtils;
import org.inria.myriads.snoozenode.util.OutputUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the first-fit virtual machine placement policy.
 * 
 * @author Eugen Feller
 */
public final class FirstFitVirtualMachinePlacement 
    implements PlacementPolicy 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(FirstFitVirtualMachinePlacement.class);
    
    /** Resource demand estimator. */
    private ResourceDemandEstimator estimator_;
    
    /**
     * Constructor.
     * 
     * @param estimator     The estimator
     */
    public FirstFitVirtualMachinePlacement(ResourceDemandEstimator estimator) 
    {
        Guard.check(estimator);
        log_.debug("Initializing first-fit virtual machine placement policy");
        estimator_ = estimator;
    }
    
    /**
     * Places a single virtual machine.
     * 
     * @param virtualMachineMetaData    The virtual machine meta data
     * @param localControllers          The local controller descriptions
     * @return                          The local controller description
     */
    @Override
    public LocalControllerDescription place(VirtualMachineMetaData virtualMachineMetaData,
                                            List<LocalControllerDescription> localControllers)
    {
        Guard.check(virtualMachineMetaData, localControllers);    
        String virtualMachineId = virtualMachineMetaData.getVirtualMachineLocation().getVirtualMachineId();
        log_.debug(String.format("Placing virtual machine: %s", virtualMachineId));
                
        SortUtils.sortLocalControllersDecreasing(localControllers, estimator_);
        OutputUtils.printLocalControllers(localControllers);
        LocalControllerDescription localController = EstimatorUtils.findSuitableLocalController(virtualMachineMetaData, 
                                                                                                localControllers,
                                                                                                estimator_);
        if (localController == null)                                                       
        {
            log_.debug(String.format("No suitable local controller to host the virtual machine: %s", virtualMachineId));
            return null;
        }
              
        log_.debug(String.format("Local controller %s has enough capacity!", localController.getId()));
        return localController;
    }
}
