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
package org.inria.myriads.snoozenode.groupmanager.managerpolicies.util;

import java.util.Collections;
import java.util.List;

import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.comparators.GroupManagerL1Decreasing;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.comparators.LocalControllerL1Decreasing;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.comparators.LocalControllerL1Increasing;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.comparators.VirtualMachineEuclidDecreasing;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.comparators.VirtualMachineL1Decreasing;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.comparators.VirtualMachineL1Increasing;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.comparators.VirtualMachineMaxDecreasing;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.sort.SortNorm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sort utilities.
 * 
 * @author Eugen Feller
 */
public final class SortUtils 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(SortUtils.class);
    
    /** Hide. */
    private SortUtils()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Sorts virtual machines in increasing order.
     * 
     * @param virtualMachines      The virtual machines
     * @param estimator            The resource estimator
     */
    public static void sortVirtualMachinesIncreasing(List<VirtualMachineMetaData> virtualMachines,
                                                     ResourceDemandEstimator estimator) 
    {
        Guard.check(virtualMachines, estimator);
        log_.debug(String.format("Sorting virtual machines in increasing order according to %s norm!", 
                                 estimator.getSortNorm()));
        
        switch (estimator.getSortNorm())
        {
            case L1 : 
                Collections.sort(virtualMachines, new VirtualMachineL1Increasing(estimator));
                break;
                        
            default:
                log_.debug("Unknown virtual machine demand measure selected!");
                break;
        }
    }
    
    /**
     * Sorts group managers in decreasing order.
     * 
     * @param groupManagers   The group managers
     * @param sortNorm        The sort norm
     */
    public static void sortGroupManagerDesceasing(List<GroupManagerDescription> groupManagers, SortNorm sortNorm) 
    {
        Guard.check(groupManagers, sortNorm);
        log_.debug(String.format("Sorting group managers in decreasing order according to %s norm!", sortNorm));
        
        switch (sortNorm)
        {
            case L1 : 
                Collections.sort(groupManagers, new GroupManagerL1Decreasing());
                break;
                        
            default:
                log_.debug("Unknown group manager sort norm selected!");
                break;
        }
    }
    
    /**
     * Sort the given VM list in decreasing order according to the specified demand measure.
     *  
     * @param virtualMachines    The virtual machine descriptions
     * @param estimator          The estimator
     */
    public static void sortVirtualMachinesDecreasing(List<VirtualMachineMetaData> virtualMachines,  
                                                     ResourceDemandEstimator estimator)
    {
        Guard.check(virtualMachines, estimator);
        log_.debug(String.format("Sorting virtual machines in decreasing order according to %s norm!", 
                                 estimator.getSortNorm()));
        
        switch (estimator.getSortNorm())
        {
            case L1 : 
                Collections.sort(virtualMachines, new VirtualMachineL1Decreasing(estimator));
                break;
            
            case Euclid :
                Collections.sort(virtualMachines, new VirtualMachineEuclidDecreasing(estimator));
                break;
            
            case Max :
                Collections.sort(virtualMachines, new VirtualMachineMaxDecreasing(estimator));
                break;
            
            default:
                log_.debug("Unknown virtual machine demand measure selected!");
                break;
        }
    }

    /**
     * Sort the local controlelrs in decreasing order according to the specified demand measure.
     *  
     * @param localControllers      The local controller descriptions
     * @param estimator             The estimator
     */
    public static void sortLocalControllersIncreasing(List<LocalControllerDescription> localControllers,
                                                      ResourceDemandEstimator estimator)
    {
        Guard.check(localControllers, estimator);
        log_.debug(String.format("Sorting local controllers in increasing order according to %s norm!", 
                                  estimator.getSortNorm()));
        
        switch (estimator.getSortNorm())
        {
            case L1 : 
                Collections.sort(localControllers, new LocalControllerL1Increasing(estimator));
                break;
            
            default:
                log_.debug("Unknown local controller demand measure selected!");
                break;
        }
    }
    
    /**
     * Sort the local controlelrs in decreasing order according to the specified demand measure.
     *  
     * @param localControllers      The local controller descriptions
     * @param estimator             The estimator
     */
    public static void sortLocalControllersDecreasing(List<LocalControllerDescription> localControllers,
                                                      ResourceDemandEstimator estimator)
    {
        Guard.check(localControllers, estimator);
        log_.debug(String.format("Sorting local controllers in decreasing order according to %s norm!", 
                                  estimator.getSortNorm()));
        
        switch (estimator.getSortNorm())
        {
            case L1 : 
                Collections.sort(localControllers, new LocalControllerL1Decreasing(estimator));
                break;
            
            default:
                log_.debug("Unknown local controller demand measure selected!");
                break;
        }
    }    
}
