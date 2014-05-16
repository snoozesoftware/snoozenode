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

import java.lang.reflect.InvocationTargetException;

import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.scheduler.GroupManagerSchedulerSettings;
import org.inria.myriads.snoozenode.configurator.scheduler.ReconfigurationSettings;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPolicy;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.impl.FirstFit;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.impl.RoundRobin;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.ReconfigurationPolicy;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.impl.SerconVirtualMachineConsolidation;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.relocation.api.VirtualMachineRelocation;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.relocation.api.impl.NoOperation;
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
            placement = new FirstFit();
        }
        else if (placementPolicy.toLowerCase().equals("roundrobin"))
        {
            log_.debug("Loading the round robin placement policy");
            placement = new RoundRobin();
        }
        else
        {
            try
            {
                log_.debug("Loading a custom placement policy");
                Class<?> placementClass = PluginUtils.getClassFromPluginsDirectory(placementPolicy);
                log_.debug(String.format("instantiate the placement policy %s from the jar", placementPolicy));
                Object placementObject =
                        placementClass.getConstructor().newInstance();
                placement = (PlacementPolicy) placementObject;
            }
            catch (Exception e)
            {
                log_.error("Unable to load the placement policy from the plugin directory");
                log_.error(e.getMessage());
                log_.debug("Back to default placement policy");
                placement = new FirstFit();
            }                
        }
        
        placement.setEstimator(estimator);
        placement.setGroupManagerSettings(schedulerSettings);
        placement.initialize();
        return placement;
    }
    
    /**
     * Creates a new virtual machine reconfiguration policy.
     * 
     * @param settings  The desired reconfiguration policy
     * @param estimator              The resource demand estimator
     * @return                       The selected reconfiguration policy
     */
    public static ReconfigurationPolicy newVirtualMachineReconfiguration(ReconfigurationSettings settings,
                                                                         ResourceDemandEstimator estimator) 
    {
        Guard.check(settings);
        log_.debug(String.format("Selected virtual machine reconfiguration policy: %s", 
                                 settings.getPolicy()));
        String policy = settings.getPolicy();
        ReconfigurationPolicy reconfiguration = null;
        
        if (policy.equals("sercon"))
        {
            reconfiguration = new SerconVirtualMachineConsolidation();
        }
        else
        {
            try
            {
                log_.debug(String.format("Loading custom reconfiguration policy %s", policy));
                Object reconfigurationPolicyObject = PluginUtils.createFromFQN(policy);
                reconfiguration = (ReconfigurationPolicy) reconfigurationPolicyObject;
            }
            catch (Exception exception)
            {
                log_.error(String.format("Unable to load the custom reconfiguration policy %s", policy));
                reconfiguration = new SerconVirtualMachineConsolidation();
            }
        }
        
        reconfiguration.setEstimator(estimator);
        reconfiguration.setReconfigurationSettings(settings);
        reconfiguration.initialize();
        
        return reconfiguration;
    }
    
    /**
     * 
     * Creates a new virtual machine relocation.
     * 
     * @param policy            The relocation policy
     * @param estimator         The estimator.
     * @return  The virtual machine relocation implementation.
     */
    public static VirtualMachineRelocation newVirtualMachineRelocation(
            String policy,
            ResourceDemandEstimator estimator)
    {
        // create using reflection.
        String classURI = policy;
        ClassLoader classLoader = GroupManagerPolicyFactory.class.getClassLoader();
        
        VirtualMachineRelocation relocationPolicy = null;
        try
        {
            Class<?> relocationClass = classLoader.loadClass(classURI);
            Object relocationObject;
            relocationObject = relocationClass.getConstructor().newInstance();
            relocationPolicy = (VirtualMachineRelocation) relocationObject;
            log_.debug("Sucessfully created anomaly resolver" + classURI);
            
        }
        catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException e)
        {
            e.printStackTrace();
            relocationPolicy = new NoOperation();
        }
        relocationPolicy.setEstimator(estimator);
        relocationPolicy.initialize();
        return relocationPolicy;
    }
}
