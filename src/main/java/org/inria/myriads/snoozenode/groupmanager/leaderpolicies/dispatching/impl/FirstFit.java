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
package org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.DispatchingPlan;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.DispatchingPolicy;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.util.LeaderPolicyUtils;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.util.SortUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the round robin single group manager virtual cluster placement policy.
 * 
 * @author Eugen Feller
 */
public final class FirstFit 
    implements DispatchingPolicy 
{
    /** Logger. */
    private static final Logger log_ = LoggerFactory.getLogger(FirstFit.class);

    /** Estimator.*/
    private ResourceDemandEstimator estimator_;
    
    /** 
     * Constructor. 
     * 
     * @param estimator     The estimator
     */
    public FirstFit(ResourceDemandEstimator estimator) 
    {
        log_.debug("Initializing the first-fit virtual cluster dispatching policy");  
        estimator_ = estimator;
    }
    
    /**
     * Assigns a virtual cluster.
     * 
     * @param virtualMachines              The virtual machines
     * @param groupManagerDescriptions     The group manager descriptions
     * @return                             The dispatch plan
     */
    public DispatchingPlan dispatch(List<VirtualMachineMetaData> virtualMachines,
                                    List<GroupManagerDescription> groupManagerDescriptions)
    {
        Guard.check(virtualMachines, groupManagerDescriptions);      
        log_.debug("Computing dispatching according to the first-fit policy");
         
        SortUtils.sortGroupManagerDesceasing(groupManagerDescriptions, estimator_.getSortNorm());
        LeaderPolicyUtils.printGroupManagerDescriptions(groupManagerDescriptions);

        ArrayList<GroupManagerDescription> candidateGroupManagers = new ArrayList<GroupManagerDescription>(); 
        for (GroupManagerDescription groupManager : groupManagerDescriptions)
        {                          
            if (virtualMachines.size() == 0)
            {
                log_.debug("All virtual machines assigned! Finished!");
                break;
            }
            
            Iterator<VirtualMachineMetaData> iterator = virtualMachines.iterator();
            while (iterator.hasNext())
            {
                VirtualMachineMetaData virtualMachine = iterator.next();
                if (estimator_.hasEnoughGroupManagerCapacity(virtualMachine, groupManager))
                {
                    String virtualMachineId = virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
                    log_.debug(String.format("Virtual machine: %s assigned to be potentially scheduled on " +
                                              "group manager: %s", 
                                              virtualMachineId,
                                              groupManager.getId()));
                    
                    NetworkAddress address = groupManager.getListenSettings().getControlDataAddress();
                    virtualMachine.setGroupManagerControlDataAddress(address);
                    groupManager.getVirtualMachines().add(virtualMachine);
                    iterator.remove();
                }
            }
            
            if (groupManager.getVirtualMachines().size() > 0)
            {
                candidateGroupManagers.add(groupManager);
            }
        }
        
        DispatchingPlan dispatchPlan = new DispatchingPlan(candidateGroupManagers);
        return dispatchPlan;
    }        
}
