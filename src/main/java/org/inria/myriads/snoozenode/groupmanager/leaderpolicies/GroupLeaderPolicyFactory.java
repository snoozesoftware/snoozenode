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
package org.inria.myriads.snoozenode.groupmanager.leaderpolicies;

import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.assignment.AssignmentPolicy;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.assignment.impl.RandomLocalController;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.assignment.impl.RoundRobinLocalController;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.DispatchingPolicy;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.impl.FirstFit;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.impl.RoundRobin;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.enums.Assignment;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.enums.Dispatching;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Group leader policy factory.
 * 
 * @author Eugen Feller
 */
public final class GroupLeaderPolicyFactory 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupLeaderPolicyFactory.class);

    /**
     * Hide the consturctor.
     */
    private GroupLeaderPolicyFactory() 
    {
        throw new UnsupportedOperationException();
    }
    
    /** 
     * Assigns a local controller to group manager.
     *  
     * @param localControllerAssignmentPolicy    The local controller assignment strategy
     * @return                                   The group manager description
     */
    public static AssignmentPolicy newLocalControllerAssignment(Assignment localControllerAssignmentPolicy) 
    {
        Guard.check(localControllerAssignmentPolicy);
        log_.debug(String.format("Selected local controller asasignment policy: %s", localControllerAssignmentPolicy));
        
        AssignmentPolicy assignmentPolicy = null;     
        switch (localControllerAssignmentPolicy) 
        {
            case Random :       
                assignmentPolicy = new RandomLocalController();               
                break;
                
            case RoundRobin :
                assignmentPolicy = new RoundRobinLocalController(); 
                break;
                                
            default:
                log_.error("Unknown local controller assignment strategy selected");
        }
        
        return assignmentPolicy;
    }
    
    /**
     * Assign a virtual cluster to group managers.
     * 
     * @param dispatchingPolicy     The virtual cluster dispatching policy
     * @param demandEstimator       The resource demand estimator
     * @return                      The virtual cluster mapping
     */
    public static DispatchingPolicy newVirtualClusterPlacement(Dispatching dispatchingPolicy,
                                                               ResourceDemandEstimator demandEstimator)
    {
        Guard.check(dispatchingPolicy, demandEstimator);
        log_.debug(String.format("Selected virtual cluster dispatching policy: %s", dispatchingPolicy));
        
        DispatchingPolicy assignmentPolicy = null;
        switch (dispatchingPolicy) 
        {
            case FirstFit :              
                assignmentPolicy = new FirstFit(demandEstimator);
                break;
 
            case RoundRobin :            
                assignmentPolicy = new RoundRobin(demandEstimator);            
                break;
                
            default:
                log_.error("Unknown virtual cluster assignment policy selected");
        }
        
        return assignmentPolicy;       
    }    
}
