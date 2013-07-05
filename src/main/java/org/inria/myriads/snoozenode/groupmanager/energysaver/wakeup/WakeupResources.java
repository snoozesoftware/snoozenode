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
package org.inria.myriads.snoozenode.groupmanager.energysaver.wakeup;

import java.util.List;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerStatus;
import org.inria.myriads.snoozecommon.communication.localcontroller.wakeup.WakeupSettings;
import org.inria.myriads.snoozecommon.util.TimeUtils;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.executor.ShellCommandExecuter;
import org.inria.myriads.snoozenode.groupmanager.powermanagement.PowerManagementFactory;
import org.inria.myriads.snoozenode.groupmanager.powermanagement.api.WakeUp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the triggers to wakeup resources.
 * 
 * @author Eugen Feller
 */
public final class WakeupResources 
{
    /** Logging instance. */
    private static final Logger log_ = LoggerFactory.getLogger(WakeupResources.class);
    
    /** Group manager repository. */
    private GroupManagerRepository repository_;
    
    /** Shell command executor. */
    private ShellCommandExecuter commandExecutor_;
    
    /** Wakeup timeout. */
    private int wakeupTimeout_;

    /** 
     * Constructor. 
     * 
     * @param wakeupTimeout             The wakeup timeout
     * @param commandExecutionTimeout   The command execution timeout
     * @param repository                The repository
     */
    public WakeupResources(int wakeupTimeout, int commandExecutionTimeout, GroupManagerRepository repository)
    {
        wakeupTimeout_ = wakeupTimeout;
        repository_ = repository;
        commandExecutor_ = new ShellCommandExecuter(commandExecutionTimeout);
    }
    
    /**
     * Wakeup local controllers.
     * 
     * @param localControllers         The local controllers
     * @return                         true if everything ok, false otherwise
     * @throws InterruptedException 
     */
    public boolean wakeupLocalControllers(List<LocalControllerDescription> localControllers)
        throws InterruptedException 
    {
        log_.debug("Starting to wakeup passive local controllers");
        
        if (localControllers.size() == 0)
        {
            log_.debug("List of passive local controllers is empty!");
            return false;
        }
        
        for (LocalControllerDescription localController : localControllers)
        {
            boolean isWokenUp = wakeupLocalController(localController);
            if (!isWokenUp)
            {
                log_.error("Failed to wakeup local controller!");
                return false;
            }
        }

        return true;
    }
    
    /** 
     * Causes the resolver module to sleep.
     * 
     * @return  true if everything ok, false otherwise
     */
    public boolean sleep() 
    {
        try 
        {
            log_.debug(String.format("Waiting %s seconds for local controller to boot!!", wakeupTimeout_));
            Thread.sleep(TimeUtils.convertSecondsToMilliseconds(wakeupTimeout_));
            log_.debug("Finished waiting!");
        } 
        catch (InterruptedException exception) 
        {
            log_.error("Interrupted exception", exception);
            return false;
        }   
        
        return true;
    }
    
    /**
     * Wakes up a passive local controller.
     * 
     * @param localController   The local controller description
     * @return                  true if everything ok, false otherwise
     */
    public boolean wakeupLocalController(LocalControllerDescription localController)
    {
        log_.debug(String.format("Waking up local controller: %s!", localController.getId()));
        
        WakeupSettings settings = localController.getWakeupSettings();
        WakeUp wakeupLogic = PowerManagementFactory.newWakeupDriver(settings.getDriver(), commandExecutor_);
        if (wakeupLogic == null)
        {
            log_.error("Error during wakeup logic retrieval!");
            return false;
        }
        
        boolean isWokenUp = wakeupLogic.wakeUp(settings.getOptions());
        if (!isWokenUp)
        {
            log_.error("Unable to wakeup the local controller");
            return false;
        }
        
        boolean isChanged = repository_.changeLocalControllerStatus(localController.getId(), 
                                                                    LocalControllerStatus.WOKENUP);
        if (!isChanged)
        {
            log_.error("Failed to change local controller status!");
            return false;
        }
        
        log_.debug("Local controller powered up!");
        return true;
    }
}
