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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.guard.Guard;
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
public class RoundRobin 
    extends DispatchingPolicy 
{
    /** Logger. */
    private static final Logger log_ = LoggerFactory.getLogger(RoundRobin.class);
    
    
    /** Running index. */
    private int runningIndex_;
        
    /** 
     * Constructor. 
     * 
     */
    public RoundRobin() 
    {
        log_.debug("Creating the round robin virtual cluster dispatching policy");  
    }
    
    @Override
    public void initialize()
    {
        log_.debug("Initializing the round robin virtual cluster dispatching policy");
        runningIndex_ = 0;   
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
        log_.debug("Dispatching virtual machines according to the round robin policy");
        
        Map<String, GroupManagerDescription> candidateGroupManagers = new HashMap<String, GroupManagerDescription>();
        int runningIndex = runningIndex_;
        int numberOfVirtualMachines = virtualMachines.size();
        int numberOfGroupManagers = groupManagerDescriptions.size();
        while (runningIndex <= numberOfGroupManagers * numberOfVirtualMachines)
        {
            int nextGroupManagerIndex = runningIndex % numberOfGroupManagers;      
            GroupManagerDescription groupManager = groupManagerDescriptions.get(nextGroupManagerIndex);
            
            if (virtualMachines.size() == 0)
            {
                log_.debug("All virtual machines assigned! Finished!");
                break;
            }
            
            Iterator<VirtualMachineMetaData> iterator = virtualMachines.iterator();
            while (iterator.hasNext())
            {
                VirtualMachineMetaData metaData = iterator.next();
                String virtualMachineId = metaData.getVirtualMachineLocation().getVirtualMachineId();
                if (estimator_.hasEnoughGroupManagerCapacity(metaData, groupManager))
                {
                    log_.debug(String.format("Virtual machine: %s assigned to be potentially scheduled on " +
                                              "group manager: %s", 
                                              virtualMachineId,
                                              groupManager.getId()));
                    
                    NetworkAddress address = groupManager.getListenSettings().getControlDataAddress();
                    metaData.getVirtualMachineLocation().setGroupManagerControlDataAddress(address);
                    metaData.getVirtualMachineLocation().setGroupManagerId(groupManager.getId());
                    groupManager.getVirtualMachines().add(metaData);
                    candidateGroupManagers.put(groupManager.getId(), groupManager);
                    iterator.remove();
                    break;
                }
                
                log_.debug(String.format("Not enough capacity for virtual machine: %s on group manager: %s",
                                          virtualMachineId,
                                          groupManager.getId()));
            }
            
            runningIndex++;
        }
        
        runningIndex_ = (runningIndex_ + 1) % numberOfGroupManagers;
        
        ArrayList<GroupManagerDescription> groupManagers = 
            new ArrayList<GroupManagerDescription>(candidateGroupManagers.values()); 
        DispatchingPlan dispatchPlan = new DispatchingPlan(groupManagers);
        return dispatchPlan;
    }        
}
