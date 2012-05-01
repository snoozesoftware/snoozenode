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
package org.inria.myriads.snoozenode.groupmanager.leaderpolicies.assignment.impl;

import java.util.List;

import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.assignment.AssignmentPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Round robin assignment policy.
 * 
 * @author Eugen Feller
 *
 */
public final class RoundRobinLocalController 
    implements AssignmentPolicy 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(RoundRobinLocalController.class);
    
    /** Running index variable. */
    private int runningIndex_;
    
    /** Constructor. */
    public RoundRobinLocalController() 
    {
        log_.debug("Initializing round robin local controller assignment policy");
    }
    
    /** 
     * Main routine to assign a group manager.
     *  
     * @param localController  The local controller
     * @param groupManager     The group manager descriptions
     * @return                 The group manager description
     */ 
    public GroupManagerDescription assign(LocalControllerDescription localController,
                                          List<GroupManagerDescription> groupManager)
    {
        Guard.check(localController, groupManager);
        log_.debug("Starting round robin local controller assignment");

        if (groupManager.size() == 0) 
        {
            log_.debug("No group manager data exist! Unable to assign the local controller!");
            return null;
        }

        int nextLocalController = runningIndex_ % groupManager.size();         
        GroupManagerDescription groupManagerDescription = groupManager.get(nextLocalController);
        runningIndex_++;
        return groupManagerDescription;
    }
}
