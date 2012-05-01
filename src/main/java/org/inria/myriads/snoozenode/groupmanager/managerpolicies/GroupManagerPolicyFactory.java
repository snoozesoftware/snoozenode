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
package org.inria.myriads.snoozenode.groupmanager.managerpolicies;

import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.enums.Placement;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.enums.Reconfiguration;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.enums.Relocation;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPolicy;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.impl.FirstFitVirtualMachinePlacement;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.impl.RoundRobinVirtualMachinePlacement;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.ReconfigurationPolicy;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.impl.SerconVirtualMachineConsolidation;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.relocation.VirtualMachineRelocation;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.relocation.impl.GreedyOverloadRelocation;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.relocation.impl.GreedyUnderloadRelocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Group manager policy factory.
 * 
 * @author Eugen Feller
 */
public final class GroupManagerPolicyFactory 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupManagerPolicyFactory.class);
    
    /**
     * Hide the consturctor.
     */
    private GroupManagerPolicyFactory() 
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Creates a new virtual machine placement policy.
     * 
     * @param virtualMachinePlacementPolicy    The desired virtual machine placement policy
     * @param estimator                        The resource demand estimator
     * @return                                 The selected virtual machine placement policy
     */
    public static PlacementPolicy 
        newVirtualMachinePlacement(Placement virtualMachinePlacementPolicy,
                                   ResourceDemandEstimator estimator) 
    {
        Guard.check(virtualMachinePlacementPolicy);
        log_.debug(String.format("Selecting the virtual machine placement policy: %s", 
                                 virtualMachinePlacementPolicy));
        
        PlacementPolicy placementPolicy = null;
        switch (virtualMachinePlacementPolicy) 
        {
            case FirstFit :
                placementPolicy = new FirstFitVirtualMachinePlacement(estimator);
                break;
            
            case RoundRobin :
                placementPolicy = new RoundRobinVirtualMachinePlacement(estimator);
                break;
                
            default :
                log_.error("Unknown virtual machine placement policy selected!");
        }
        
        return placementPolicy;
    }
    
    /**
     * Creates a new virtual machine reconfiguration policy.
     * 
     * @param reconfigurationPolicy  The desired virtual machine reconfiguration policy
     * @param estimator              The resource demand estimator
     * @return                       The selected virtual machine reconfiguration policy
     */
    public static ReconfigurationPolicy newVirtualMachineReconfiguration(Reconfiguration reconfigurationPolicy,
                                                                         ResourceDemandEstimator estimator) 
    {
        Guard.check(reconfigurationPolicy);
        log_.debug(String.format("Selecting the virtual machine reconfiguration policy: %s", 
                                 reconfigurationPolicy));
        
        ReconfigurationPolicy reconfiguration = null;
        switch (reconfigurationPolicy) 
        {
            case Sercon :
                reconfiguration = new SerconVirtualMachineConsolidation(estimator);
                break;
              
            default :
                log_.error("Unknown virtual machine reconfiguration policy selected!");
        }
        
        return reconfiguration;
    }
    
    /**
     * Creates a new virtual machine relocation policy.
     * 
     * @param relocationPolicy  The desired virtual machine relocation policy
     * @param estimator         The resource demand estimator
     * @return                  The selected virtual machine relocation policy
     */
    public static VirtualMachineRelocation newVirtualMachineRelocation(Relocation relocationPolicy,
                                                                       ResourceDemandEstimator estimator) 
    {
        Guard.check(relocationPolicy);
        log_.debug(String.format("Selecting the virtual machine relocation policy: %s", 
                                 relocationPolicy));
        
        VirtualMachineRelocation relocation = null;
        switch (relocationPolicy) 
        {
            case GreedyUnderloadRelocation :
                relocation = new GreedyUnderloadRelocation(estimator);
                break;
            
            case GreedyOverloadRelocation :
                relocation = new GreedyOverloadRelocation(estimator);
                break;
              
            default :
                log_.error("Unknown virtual machine relocation policy selected!");
        }
        
        return relocation;
    }
}
