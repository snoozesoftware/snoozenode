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
package org.inria.myriads.snoozenode.groupmanager.managerpolicies.relocation.api.impl;

import java.util.ArrayList;
import java.util.List;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.ReconfigurationPlan;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.relocation.api.VirtualMachineRelocation;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.relocation.utility.RelocationUtility;
import org.inria.myriads.snoozenode.localcontroller.monitoring.enums.LocalControllerState;
import org.inria.myriads.snoozenode.util.OutputUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Moderate loaded server relocation policy.
 * 
 * @author Eugen Feller
 * @author Matthieu Simonin
 */
public final class DummyOverloadRelocation 
    extends VirtualMachineRelocation 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(DummyOverloadRelocation.class);
    
    /**
     * Constructor.
     * 
     */
    public DummyOverloadRelocation()
    {
        log_.debug("Initializing the least loaded server relocation policy");       
    }

    

    @Override
    public void initialize()
    {
        
    }
    
    /**
     * Compute migration candidates.
     * 
     * @param virtualMachines       The virtual machines
     * @return                      The migration candidates
     */
    private List<VirtualMachineMetaData> getMigrationCandidates(List<VirtualMachineMetaData> virtualMachines)
    {
        log_.debug("Computing list of migration candidates");
        
        List<VirtualMachineMetaData> migrationCandidates = new ArrayList<VirtualMachineMetaData>();
        if (virtualMachines.size() > 0)
        {
            migrationCandidates.add(virtualMachines.get(0));
        }
        return migrationCandidates;
    }
    
    /**
     * Computes a migration plan to relocates virtual machines.
     * 
     * @param sourceLocalController         The source local controller description
     * @param destinationLocalControllers   The destination local controller candidates
     * @return                              The migration plan
     */
    public ReconfigurationPlan relocateVirtualMachines(LocalControllerDescription sourceLocalController, 
                                                 List<LocalControllerDescription> destinationLocalControllers)
    {
        log_.debug("Starting to compute the dummy moderate loaded migration plan");
        
        List<VirtualMachineMetaData> virtualMachines = 
            new ArrayList<VirtualMachineMetaData>(sourceLocalController.getVirtualMachineMetaData().values());
        estimator_.sortVirtualMachines(virtualMachines, false);
        
        OutputUtils.printVirtualMachines(virtualMachines);
                            
        List<VirtualMachineMetaData> migrationCandidates = getMigrationCandidates(virtualMachines);
        
        estimator_.sortLocalControllers(destinationLocalControllers, false);
        ReconfigurationPlan reconfigurationPlan = 
                RelocationUtility.computeReconfigurationPlan(migrationCandidates,  
                                                             destinationLocalControllers, 
                                                             estimator_,
                                                             LocalControllerState.OVERLOADED);
        return reconfigurationPlan;
    }

    

}
