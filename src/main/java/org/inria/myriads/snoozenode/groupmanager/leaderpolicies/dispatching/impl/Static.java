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
package org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.impl;

import java.util.ArrayList;
import java.util.List;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineErrorCode;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineStatus;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.DispatchingPlan;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.DispatchingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the round robin single group manager virtual cluster 
 * placement policy.
 * 
 * @author Eugen Feller
 */
public class Static 
    implements DispatchingPolicy 
{
    /** Logger. */
    private static final Logger log_ = LoggerFactory.getLogger(Static.class);
    
    /** Resource demand estimator. */
    private ResourceDemandEstimator estimator_;
    
    /** 
     * Constructor. 
     * 
     * @param estimator     The estimator
     */
    public Static(ResourceDemandEstimator estimator) 
    {
        log_.debug("Initializing the static virtual cluster dispatching policy");  
        estimator_ = estimator;
    }
    
    /**
     * Assigns a virtual cluster.
     * 
     * @param virtualMachines              The virtual machines
     * @param groupManagerDescriptions     The group manager descriptions
     * @return                             The dispatching plan
     */
    public DispatchingPlan dispatch(List<VirtualMachineMetaData> virtualMachines,
                                    List<GroupManagerDescription> groupManagerDescriptions)
    {
        Guard.check(virtualMachines, groupManagerDescriptions);      
        log_.debug("Dispatching virtual machines according to the Bound policy");
        log_.debug("Constructing the dispatch plan for bound virtual machines");
        ArrayList<GroupManagerDescription> groupManagerCandidates = new ArrayList<GroupManagerDescription>();

        
        for (GroupManagerDescription groupManager : groupManagerDescriptions)
        {
            String groupManagerId = groupManager.getId();
            for (VirtualMachineMetaData virtualMachine : virtualMachines)
            {
                if (virtualMachine.getVirtualMachineLocation().getGroupManagerId().equals(groupManagerId))
                {
                    String virtualMachineId = virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
                    if (estimator_.hasEnoughGroupManagerCapacity(virtualMachine, groupManager))
                    {
                        log_.debug(String.format("Virtual machine: %s assigned to be potentially scheduled on " +
                                "group manager: %s", 
                                virtualMachineId,
                                groupManager.getId()));
                        
                        NetworkAddress address = groupManager.getListenSettings().getControlDataAddress();
                        virtualMachine.setGroupManagerControlDataAddress(address);
                        groupManager.getVirtualMachines().add(virtualMachine);
                        

                    }
                    virtualMachine.setStatus(VirtualMachineStatus.ERROR);
                    virtualMachine.setErrorCode(VirtualMachineErrorCode.NOT_ENOUGH_GROUP_MANAGER_CAPACITY);
                }
            }
            
            if (groupManager.getVirtualMachines().size() > 0)
            {
                groupManagerCandidates.add(groupManager);
            }
        }
        log_.debug(String.format("Returning a new dispatch plan for bound " +
                "virtual machines with %d groupmanagers used", groupManagerCandidates.size()));
        DispatchingPlan dispatchPlan = new DispatchingPlan(groupManagerCandidates);
        return dispatchPlan;

    }        
}
