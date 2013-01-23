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
package org.inria.myriads.snoozenode.groupmanager.managerpolicies.relocation.utility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.estimator.util.EstimatorUtils;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.ReconfigurationPlan;
import org.inria.myriads.snoozenode.localcontroller.monitoring.enums.LocalControllerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Relocation utility class.
 * 
 * @author Eugen Feller
 */
public final class RelocationUtility 
{ 
    /** Number of released nodes. */
    public static  final int NUMBER_OF_RELEASED_NODES = 1;
    
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(RelocationUtility.class);
    
    /**
     * Hide the consturctor.
     */
    private RelocationUtility() 
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Computes the migration plan for anomalied local controllers. 
     * 
     * @param migrationCandidates           The migration candidates
     * @param destinationLocalControllers   The destination local controllers
     * @param estimator                     The resource demand estimator
     * @param state                         The local controller state
     * @return                              The migration plan
     */
    public static ReconfigurationPlan 
        computeReconfigurationPlan(List<VirtualMachineMetaData> migrationCandidates,
                                   List<LocalControllerDescription> destinationLocalControllers,
                                   ResourceDemandEstimator estimator,
                                   LocalControllerState state) 
    {
        Guard.check(migrationCandidates, destinationLocalControllers, estimator);
        log_.debug(String.format("Computing migration plan for %d virtual machines", migrationCandidates.size()));
        
        if (migrationCandidates.size() == 0)
        {
            log_.debug("List of migration candidates is empty!");
            return null;
        }
        
        Map<VirtualMachineMetaData, LocalControllerDescription> mapping = 
            new HashMap<VirtualMachineMetaData, LocalControllerDescription>();
        
        for (VirtualMachineMetaData candidateVirtualMachine : migrationCandidates)
        {
            String virtualMachineId = candidateVirtualMachine.getVirtualMachineLocation().getVirtualMachineId();
            log_.debug(String.format("Finding destination for virtual machine: %s", virtualMachineId));
            
            LocalControllerDescription localController = null;
            switch (state)
            {
                case OVERLOADED :
                   localController = EstimatorUtils.findSuitableLocalController(candidateVirtualMachine, 
                                                                                destinationLocalControllers,
                                                                                estimator);
                    if (localController == null)
                    {
                        log_.debug("No local controller with enough capacity could be detected!");
                        continue;
                    }
                    break;
                    
                case UNDERLOADED :
                    localController = EstimatorUtils.findSuitableLocalController(candidateVirtualMachine, 
                                                                                 destinationLocalControllers,
                                                                                 estimator);
                    if (localController == null)
                    {
                        log_.debug("No local controller with enough capacity could be detected!");
                        return null;
                    }
                    break;
                    
                default:
                    log_.error(String.format("Unknown local controller state: %s!", state));
                    return null;
            } 
            
            log_.debug(String.format("Local controller %s has enough capacity to host virtual machine %s! Adding!",
                                     localController.getId(), virtualMachineId));
            localController.getVirtualMachineMetaData().put(virtualMachineId, candidateVirtualMachine);
            mapping.put(candidateVirtualMachine, localController);
        }       
        
        if (mapping.size() == 0)
        {
            log_.debug("Mapping is empty!");
            return null;
        }
        
        int numberOfUsedNodes = destinationLocalControllers.size() - 1;
        ReconfigurationPlan migrationPlan = 
                new ReconfigurationPlan(mapping, numberOfUsedNodes, NUMBER_OF_RELEASED_NODES);      
        return migrationPlan;
    }
}
