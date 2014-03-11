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
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.assignment.api.AssignmentPolicy;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.assignment.api.impl.RandomLocalController;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.assignment.api.impl.RoundRobinLocalController;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.DispatchingPolicy;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.impl.FirstFit;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.impl.RoundRobin;
import org.inria.myriads.snoozenode.util.PluginUtils;
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
    public static AssignmentPolicy newLocalControllerAssignment(String localControllerAssignmentPolicy) 
    {
        Guard.check(localControllerAssignmentPolicy);
        log_.debug(String.format("Selected local controller asasignment policy: %s", localControllerAssignmentPolicy));
        
        AssignmentPolicy assignmentPolicy = null;
        
        if (localControllerAssignmentPolicy.equals("random"))
        {
            assignmentPolicy = new RandomLocalController();
        }
        else if (localControllerAssignmentPolicy.equals("roundrobin"))
        {
            assignmentPolicy = new RoundRobinLocalController();
        }
        else
        {
            // create a custom virtualcluster placement.
            try
            {
                log_.debug(String.format("Creating a custom local controller assignement policy %s",
                        localControllerAssignmentPolicy));
                Object assignementPolicyObject = PluginUtils.createFromFQN(localControllerAssignmentPolicy);
                assignmentPolicy = (AssignmentPolicy) assignementPolicyObject;
            }
            catch(Exception exception)
            {
                log_.error("Unable to create custom virtual cluster placement policy, falling back to default");
                assignmentPolicy = new RoundRobinLocalController();
            }     
        }
        
        assignmentPolicy.initialize();
        
        
        return assignmentPolicy;
    }
    
    /**
     * Assign a virtual cluster to group managers.
     * 
     * @param dispatchingPolicy     The virtual cluster dispatching policy
     * @param estimator       The resource demand estimator
     * @return                      The virtual cluster mapping
     */
    public static DispatchingPolicy newVirtualClusterPlacement(String dispatchingPolicy,
                                                               ResourceDemandEstimator estimator)
    {
        Guard.check(dispatchingPolicy, estimator);
        log_.debug(String.format("Selected virtual cluster dispatching policy: %s", dispatchingPolicy));
        
        DispatchingPolicy assignmentPolicy = null;
        if (dispatchingPolicy.equals("firstfit"))
        {
            assignmentPolicy = new FirstFit();
        }
        else if (dispatchingPolicy.equals("roundrobin"))
        {
            assignmentPolicy = new RoundRobin();
        }
        else
        {
            // create a custom virtualcluster placement.
            try
            {
                log_.debug(String.format("Creating a custom virtual cluster placement policy %s",
                        dispatchingPolicy));
                Object assignementPolicyObject = PluginUtils.createFromFQN(dispatchingPolicy);
                assignmentPolicy = (DispatchingPolicy) assignementPolicyObject;
            }
            catch(Exception exception)
            {
                log_.error("Unable to create custom virtual cluster placement policy, falling back to default");
                assignmentPolicy = new FirstFit();
            }     
        }
        
        assignmentPolicy.setEstimator(estimator);
        assignmentPolicy.initialize();
        return assignmentPolicy;       
    }    
}
