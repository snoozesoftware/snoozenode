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
package org.inria.myriads.snoozenode.groupmanager.estimator.util;

import java.util.List;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Estimator utils.
 * 
 * @author Eugen Feller
 */
public final class EstimatorUtils 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(EstimatorUtils.class);

    /**
     * Hide the consturctor.
     */
    private EstimatorUtils() 
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Finds a suitable local controller.
     * 
     * @param virtualMachine     The virtual machine meta data
     * @param localControllers   The local controller descriptions
     * @param estimator          The resource demand estimator
     * @return                   The local controller description
     */
    public static LocalControllerDescription 
        findSuitableLocalController(VirtualMachineMetaData virtualMachine, 
                                    List<LocalControllerDescription> localControllers,
                                    ResourceDemandEstimator estimator) 
    {
        Guard.check(virtualMachine, localControllers);
        
        String virtualMachineId = virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
        log_.debug(String.format("Starting to find a suitable local controller for virtual machine: %s", 
                                 virtualMachineId));
         
        for (LocalControllerDescription localControllerDescription : localControllers) 
        {
            String localControllerId = localControllerDescription.getId();
            if (localControllerId.equals(virtualMachine.getVirtualMachineLocation().getLocalControllerId()))
            {
                log_.debug("Do not consider myself as local controller candidate!");
                continue;
            }
                        
            if (estimator.hasEnoughLocalControllerCapacity(virtualMachine, localControllerDescription))
            {
                log_.debug(String.format("Virtual machine: %s fits into the local controller: %s", 
                                         virtualMachineId, 
                                         localControllerId));
                return localControllerDescription;
            }
            
            log_.debug(String.format("Virtual machine: %s does not fit into the local controller: %s", 
                                     virtualMachineId, 
                                     localControllerId)); 
        }
        
        return null;
    }
}
