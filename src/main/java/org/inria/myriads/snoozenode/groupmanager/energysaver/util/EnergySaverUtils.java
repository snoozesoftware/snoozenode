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
package org.inria.myriads.snoozenode.groupmanager.energysaver.util;

import java.util.List;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerStatus;
import org.inria.myriads.snoozecommon.communication.rest.CommunicatorFactory;
import org.inria.myriads.snoozecommon.communication.rest.api.GroupManagerAPI;
import org.inria.myriads.snoozecommon.communication.rest.api.LocalControllerAPI;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.energymanagement.enums.PowerSavingAction;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Energy saver utilities.
 * 
 * @author Eugen Feller
 */
public final class EnergySaverUtils 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(EnergySaverUtils.class);

    /** Hide constructor. */
    private EnergySaverUtils()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Suspends the the energy savers.
     * 
     * @param groupManagers      The group manager descriptions
     * @return                   true if everything ok, false otherwise
     */
    public static boolean suspendEnergySavers(List<GroupManagerDescription> groupManagers)
    {
        Guard.check(groupManagers);
        log_.debug("Freezing the energy savers on all grop managers");
        
        for (GroupManagerDescription groupManager : groupManagers)
        {
            log_.debug(String.format("Sending energy saver suspend request to group manager: %s", 
                                     groupManager.getId()));
            
            NetworkAddress address = groupManager.getListenSettings().getControlDataAddress();
            GroupManagerAPI groupManagerCommunicator = CommunicatorFactory.newGroupManagerCommunicator(address);
            boolean isSuspended = groupManagerCommunicator.suspendEnergySaver();
            if (!isSuspended)
            {
                log_.debug("Error suspending the energy saver");
                return false;
            }         
        }        
        
        return true;
    }
    
    /**
     * Resumes the energy savers.
     * 
     * @param groupMaangers      The group manager descriptions
     * @return                   true if everything ok, false otherwise
     */
    public static boolean resumeEnergySavers(List<GroupManagerDescription> groupMaangers)
    {
        Guard.check(groupMaangers);
        log_.debug("Unfreezing energy savers");
        
        for (GroupManagerDescription groupManager : groupMaangers)
        {
            log_.debug(String.format("Sending energy saver resume request to group manager: %s", 
                                     groupManager.getId()));
            
            NetworkAddress address = groupManager.getListenSettings().getControlDataAddress();
            GroupManagerAPI groupManagerCommunicator = CommunicatorFactory.newGroupManagerCommunicator(address);
            boolean isResumed = groupManagerCommunicator.resumeEnergySaver();
            if (!isResumed)
            {
                log_.debug("Failed to resume the energy saver");
                return false;
            }
        }    
        
        log_.debug("Energy saver successfully resumed");
        return true;
    }
    
    /**
     * Power cycles local controllers.
     * 
     * @param localControllers      The list of local controllers
     * @param powerSavingAction     The power saving action
     * @param repository            The group manager repository
     */
    public static void powerCycleLocalControllers(List<LocalControllerDescription> localControllers,
                                                  PowerSavingAction powerSavingAction,
                                                  GroupManagerRepository repository)
    {
        for (LocalControllerDescription localController : localControllers)
        {
            powerCycleLocalController(localController, powerSavingAction, repository);
        }
    }
    
    /**
     * Power cycles local controller.
     * 
     * @param localController     The local controller description
     * @param powerSavingAction   The power saving action
     * @param repository          The group manager repository
     * @return                    true if everything ok, false otherwise
     */
    public static boolean powerCycleLocalController(LocalControllerDescription localController,
                                                    PowerSavingAction powerSavingAction,
                                                    GroupManagerRepository repository)
    {
        log_.debug(String.format("Starting local controller power cycling: %s", localController));
        
        boolean isChanged = repository.changeLocalControllerStatus(localController.getId(),
                                                                   LocalControllerStatus.PASSIVE);
        if (!isChanged)
        {
            log_.error("Failed to change the local controller status to PASSIVE!");
            return false;
        }
        
        boolean isSuccessfull = powerCycleLocalController(localController, powerSavingAction);
        if (isSuccessfull)
        {
            log_.debug("Local controller powered down!");   
            return true;
        }
        
        log_.error(String.format("Failed to power cycle the local controller: %s :%d",
                                  localController.getControlDataAddress().getAddress(),
                                  localController.getControlDataAddress().getPort()));
        isChanged = repository.changeLocalControllerStatus(localController.getId(), 
                                                           LocalControllerStatus.ACTIVE);
        if (!isChanged)
        {
            log_.error("Failed to change the local controller status back to ACTIVE!");
            return false;
        }
        
        return true;
    }
    
    /**
     * Triggers the specified power saving action on the local controller.
     * 
     * @param localController       The local controller description
     * @param powerSavingAction     The power saving action
     * @return                      true if everything ok, false otherwise
     */
    private static boolean powerCycleLocalController(LocalControllerDescription localController,
                                                     PowerSavingAction powerSavingAction)
    {     
        Guard.check(localController, powerSavingAction);    
        
        NetworkAddress controlDataAddress = localController.getControlDataAddress();
        log_.debug(String.format("%s local controller: %s: %d", 
                                 powerSavingAction,
                                 controlDataAddress.getAddress(),
                                 controlDataAddress.getPort()));
        
        boolean isSuccessfull = false;
        LocalControllerAPI communicator = CommunicatorFactory.newLocalControllerCommunicator(controlDataAddress);
        switch (powerSavingAction)
        {
            case shutdown :
                isSuccessfull = communicator.shutdownNode();
                break;
                
            case suspendToRam :
                isSuccessfull = communicator.suspendNodeToRam();
                break;
                
            case suspendToDisk :
                isSuccessfull = communicator.suspendNodeToDisk();
                break;
                
            case suspendToBoth :
                isSuccessfull = communicator.suspendNodeToBoth();
                break;
                
            default: 
                log_.error(String.format("Unknown power saving action specified: %s", powerSavingAction));
                break;           
        }
        
        return isSuccessfull;
    }
}
