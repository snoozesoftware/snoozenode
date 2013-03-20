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
package org.inria.myriads.snoozenode.groupmanager.energysaver.saver;

import java.util.ArrayList;
import java.util.List;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.util.TimeUtils;
import org.inria.myriads.snoozenode.configurator.energymanagement.EnergyManagementSettings;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.groupmanager.statemachine.api.StateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the energy saving logic.
 * 
 * @author Eugen Feller
 */
public final class EnergySaver 
    implements Runnable
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(EnergySaver.class);
        
    /** Number of monitoring entries. */
    private static int NUMBER_OF_MONITORING_ENTRIES;
    
    /** Energy savings. */
    private EnergyManagementSettings energySettings_;
    
    /** Repository. */
    private GroupManagerRepository repository_;

    /** State machine. */
    private StateMachine stateMachine_;
    
    /** Lock object. */
    private Object lockObject_;
  
    /** Used to suspend the saver. */
    private boolean isSuspended_;
    
    /** Terminated. */
    private boolean isTerminated_;
    
    /**
     * Energy saver constructor.
     * 
     * @param energySettings   The energy settings
     * @param repository       The group manager repository
     * @param stateMachine     The state machine
     */
    public EnergySaver(EnergyManagementSettings energySettings, 
                       GroupManagerRepository repository,
                       StateMachine stateMachine)
    {
        log_.debug("Initializing the energy saver!");
        
        energySettings_ = energySettings;
        repository_ = repository;
        stateMachine_ = stateMachine;
        lockObject_ = new Object();
    }
    
    /** Run. */
    public void run() 
    {
        int idleTimeThreshold = energySettings_.getThresholds().getIdleTime();  
        List<LocalControllerDescription> localControllers;
        try
        {
            while (true)
            {                            
                log_.debug(String.format("Waiting for: %s seconds", idleTimeThreshold));
                localControllers = repository_.getLocalControllerDescriptions(NUMBER_OF_MONITORING_ENTRIES, true, true);
                {
                    lockObject_.wait(TimeUtils.convertSecondsToMilliseconds(idleTimeThreshold));
                }
                         
                if (isTerminated_)
                {             
                    break;
                }
         
                suspend();
                
                if (stateMachine_.isBusy())
                {
                    log_.debug("System is BUSY! Skipping energy savings!");
                    continue;
                }
                
                int numberOfReservedNodes = energySettings_.getNumberOfReservedNodes();
                List<LocalControllerDescription> idleResources = getIdleLocalControllers(localControllers,
                                                                                         numberOfReservedNodes);
                int numberOfIdleNodes = idleResources.size();              
                log_.debug(String.format("Number of local controllers to power cycle: %d", numberOfIdleNodes));
                
                if (numberOfIdleNodes == 0)
                {
                    log_.debug("Not enough idle resources to perform energy savings!");
                    continue;
                }
                 
                stateMachine_.onEnergySavingsEnabled(idleResources);
            }            
        }
        catch (Exception exception)
        {
            log_.error("Energy saver was interrupted", exception);
        }
        
        log_.debug("Energy saver is stopped!");
    }
    
    /**
     * Terminate routine.
     */
    public void terminate()
    {
        log_.debug("Terminating the energy saver");
        isTerminated_ = true; 
        synchronized (lockObject_)
        {
            lockObject_.notify();
        }
    }
    
    /**
     * Can be used to suspend the saver.
     */
    public void setSuspend()
    {
        isSuspended_ = true;
    }
        
    /** 
     * Wakeup the saver.
     */
    public void wakeup() 
    {
        isSuspended_ = false;       
        synchronized (lockObject_)
        {
            lockObject_.notify();
        }
    }
    
    /**
     * Returns the number of virtual machines.
     * 
     * @param localControllerId     The local controller identifier
     * @param localControllers      The list of local controllers
     * @return                      The number of virtual machines
     */
    private int getNumberOfVirtualMachines(String localControllerId, 
                                           List<LocalControllerDescription> localControllers)
    {
        int numberOfVirtualMachines = 0;     
        
        for (LocalControllerDescription localController : localControllers)
        {
            if (localController.getId().equals(localControllerId))
            {
                return localController.getVirtualMachineMetaData().size();
            }
        }
        
        return numberOfVirtualMachines;
    }
    
    /**
     * Computes a list of idle resources.
     * 
     * @param oldLocalControllers     The old list of local controllers
     * @param numberOfReservedNodes   The number of reserved nodes
     * @return                        The list of idle local controllers
     */
    private List<LocalControllerDescription> 
        getIdleLocalControllers(List<LocalControllerDescription> oldLocalControllers, int numberOfReservedNodes)
    {
        log_.debug("Computing list of idle local controllers!");
        
        List<LocalControllerDescription> idleLocalControllers = new ArrayList<LocalControllerDescription>();
        List<LocalControllerDescription> localControllers = 
            repository_.getLocalControllerDescriptions(NUMBER_OF_MONITORING_ENTRIES, true, true);
        if (localControllers.size() <= numberOfReservedNodes)
        {
            log_.debug("Number of active local controllers is less/equal the number of reserved nodes!");
            return idleLocalControllers;
        }
        
        if (oldLocalControllers.size() != localControllers.size())
        {
            log_.debug(String.format("Number of local controllers does not match! Old one: %d, new one: %d!",
                                     oldLocalControllers.size(), localControllers.size()));
            return idleLocalControllers;
        }
        
        for (LocalControllerDescription localController : localControllers) 
        {            
            String localControllerId = localController.getId();    
            int oldNumberOfVirtualMachines = getNumberOfVirtualMachines(localControllerId, oldLocalControllers);
            int newNumberOfVirtualMachines = localController.getVirtualMachineMetaData().size();
            boolean isEqual = oldNumberOfVirtualMachines == 0 && newNumberOfVirtualMachines == 0;
            if (!isEqual)
            {
                log_.debug(String.format("Local controller %s is BUSY! Old and new number of VMs are: %d / %d", 
                                         localControllerId, oldNumberOfVirtualMachines, newNumberOfVirtualMachines));
                continue;
            }
            
            log_.debug(String.format("Local controller: %s is IDLE", localControllerId));
            idleLocalControllers.add(localController);
        }
        
        /*
         * Check if enough active nodes will be online after power cycling idle LCs! If yes all idle detected LCs 
         * can be power cycled!(e.g. 10 active, 5 idle, 3 reserved => Shutdown all idle)
         */
        int numberOfActiveLocalControllers = localControllers.size() - idleLocalControllers.size();
        boolean isAllowed = numberOfActiveLocalControllers >= numberOfReservedNodes;
        if (isAllowed)
        {
            return idleLocalControllers;
        }
        
        /*
         * If newNumberOfLocalControllers < numberOfReservedNodes 
         * we can only power cycle numberOfIdleLocalControllers - 
         * numberOfReserved LCs! Hence, remove some LCs from  the
         * idle list! (e.g. 10 active, 8 idle, 3 reserved => Shutdown 
         * 5 only)
         */
        for (int i = 0; i < numberOfReservedNodes; i++)
        {
            log_.debug(String.format("Removing reserved node: %d from idle nodes: %d", 
                                     i, idleLocalControllers.size()));
            idleLocalControllers.remove(0);
        }
        
        return idleLocalControllers;
    }
    
    /** 
     * Suspends the saver.
     * 
     * @throws InterruptedException     The interrupted exception
     */
    private void suspend() 
        throws InterruptedException 
    {
        if (isSuspended_) 
        {
            log_.debug("Energy saver suspending");     
            synchronized (lockObject_)
            {
                lockObject_.wait();
            }
            log_.debug("Energy saver waking up");
        }    
    }
}
