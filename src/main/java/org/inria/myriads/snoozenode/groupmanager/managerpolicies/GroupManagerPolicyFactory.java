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
import org.inria.myriads.snoozenode.configurator.scheduler.GroupManagerSchedulerSettings;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.enums.Reconfiguration;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.enums.Relocation;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPolicy;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.impl.FirstFit;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.impl.RoundRobin;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.ReconfigurationPolicy;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.impl.SerconVirtualMachineConsolidation;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.relocation.VirtualMachineRelocation;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.relocation.impl.GreedyOverloadRelocation;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.relocation.impl.GreedyUnderloadRelocation;
import org.inria.myriads.snoozenode.util.PluginUtils;
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
     * 
     * Creates a new virtual machine placement policy.
     * 
     * @param schedulerSettings         GroupManager scheduler settings.
     * @param estimator                 Resource demand estimator.
     * @return  The placement policy.
     */
    @SuppressWarnings("unchecked")
    public static PlacementPolicy newVirtualMachinePlacement(GroupManagerSchedulerSettings schedulerSettings,
                                                             ResourceDemandEstimator estimator) 
    {
        Guard.check(schedulerSettings, estimator);
       
        
        PlacementPolicy placement = null;
        String placementPolicy = schedulerSettings.getPlacementPolicy();
        log_.debug(String.format("Selected virtual machine placement policy: %s", placementPolicy));
        if (placementPolicy.toLowerCase().equals("firstfit")) 
        {
            log_.debug("Loading the first fit placement policy");
            placement = new FirstFit(estimator);
        }
        else if (placementPolicy.toLowerCase().equals("roundrobin"))
        {
            log_.debug("Loading the round robin placement policy");
            placement = new RoundRobin(estimator);
        }
        else
        {
            try
            {
                log_.debug("Loading a custom placement policy");
                String pluginsDirectory = schedulerSettings.getPluginsDirectory();
                Class placementClass = PluginUtils.getClassFromDirectory(pluginsDirectory , placementPolicy);
                log_.debug(String.format("instantiate the placement policy %s from the jar", placementPolicy));
                Object placementObject =
                        placementClass.getConstructor(ResourceDemandEstimator.class).newInstance(estimator);
                placement = (PlacementPolicy) placementObject;
            }
            catch (Exception e)
            {
                log_.error("Unable to load the placement policy from the plugin directory");
                e.printStackTrace();
                log_.debug("Back to default placement policy");
                placement = new FirstFit(estimator);
            }                
        }
        
        return placement;
    }
    
    /**
     * Creates a new virtual machine reconfiguration policy.
     * 
     * @param reconfigurationPolicy  The desired reconfiguration policy
     * @param estimator              The resource demand estimator
     * @return                       The selected reconfiguration policy
     */
    public static ReconfigurationPolicy newVirtualMachineReconfiguration(Reconfiguration reconfigurationPolicy,
                                                                         ResourceDemandEstimator estimator) 
    {
        Guard.check(reconfigurationPolicy);
        log_.debug(String.format("Selected virtual machine reconfiguration policy: %s", 
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
     * @param relocationPolicy  The desired relocation policy
     * @param estimator         The resource demand estimator
     * @return                  The selected relocation policy
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
