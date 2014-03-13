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
package org.inria.myriads.snoozenode.groupmanager.leaderpolicies.assignment.api.impl;

import java.util.List;
import java.util.Random;

import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.assignment.api.AssignmentPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Random local controller assignment policy.
 * 
 * @author Eugen Feller
 */
public final class RandomLocalController 
    extends AssignmentPolicy 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(RandomLocalController.class);
    
    /** Randomizer. */
    private Random random_;
    
    /** Constructor. */
    public RandomLocalController() 
    {
        log_.debug("Creating random local controller assignment policy");
        random_ = new Random();
    }
    
    
    @Override
    public void initialize()
    {
        log_.debug("Initializing random local controller assignment policy");
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
        Guard.check(groupManager);
        log_.debug("Starting random local controller assignment");
       
        if (groupManager.size() == 0) 
        {
            log_.debug("No group manager description exist! Unable to assign the local controller!");
            return null;
        }
       
        int randomNumber = generateRandomNumber(groupManager.size());
        return groupManager.get(randomNumber);
    }

    /** 
     * Generates a random number between 0 and size.
     *  
     * @param size     The size
     * @return         The random number
     */
    private int generateRandomNumber(int size) 
    {
        return random_.nextInt(size);
    }


}
