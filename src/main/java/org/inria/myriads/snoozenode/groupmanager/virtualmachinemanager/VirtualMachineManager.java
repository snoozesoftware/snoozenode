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
package org.inria.myriads.snoozenode.groupmanager.virtualmachinemanager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.rest.CommunicatorFactory;
import org.inria.myriads.snoozecommon.communication.rest.api.LocalControllerAPI;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineSubmissionRequest;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineSubmissionResponse;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.scheduler.GroupManagerSchedulerSettings;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.GroupManagerPolicyFactory;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPolicy;
import org.inria.myriads.snoozenode.groupmanager.statemachine.VirtualMachineCommand;
import org.inria.myriads.snoozenode.groupmanager.statemachine.api.StateMachine;
import org.inria.myriads.snoozenode.groupmanager.virtualmachinemanager.listener.VirtualMachineManagerListener;
import org.inria.myriads.snoozenode.groupmanager.virtualmachinemanager.worker.VirtualMachineSubmissionWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Virtual machine manager.
 * 
 * @author Eugen Feller
 */
public final class VirtualMachineManager
    implements VirtualMachineManagerListener
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(VirtualMachineManager.class);
    
    /** Group manager  repository. */
    private GroupManagerRepository repository_;
    
    /** Virtual machine placement policy. */
    private PlacementPolicy placementPolicy_;
                      
    /** State machine. */
    private StateMachine stateMachine_;
    
    /** Finsished submissions. */
    private Map<String, VirtualMachineSubmissionResponse> submissionResponses_;
    
    /** Number of monitoring entries. */
    private int numberOfMonitoringEntries_;

    /**
     * Constructor.
     * 
     * @param schedulerSettings         The scheduler settings
     * @param estimator                 The group manager repository
     * @param groupManagerRepository    The number of monitoring entries
     * @param stateMachine              The state machine
     */
    public VirtualMachineManager(GroupManagerSchedulerSettings schedulerSettings,
                                 ResourceDemandEstimator estimator,
                                 GroupManagerRepository groupManagerRepository, 
                                 StateMachine stateMachine) 
    {
        Guard.check(schedulerSettings, estimator);
        log_.debug("Initializing virtual machine management");
            
        numberOfMonitoringEntries_ = estimator.getNumberOfMonitoringEntries();        
        repository_ = groupManagerRepository;
        stateMachine_ = stateMachine;
        submissionResponses_ = new HashMap<String, VirtualMachineSubmissionResponse>();
        placementPolicy_ = GroupManagerPolicyFactory.newVirtualMachinePlacement(schedulerSettings.getPlacementPolicy(), 
                                                                                estimator);
    }
    
    /**
     * Starts a virtual machine.
     * 
     * @param submissionRequest     The virtual machine description
     * @return                      The task identifier
     */
    public String start(VirtualMachineSubmissionRequest submissionRequest) 
    {
        Guard.check(submissionRequest);
        
        String taskIdentifier = UUID.randomUUID().toString();
        VirtualMachineSubmissionWorker worker = new VirtualMachineSubmissionWorker(taskIdentifier,
                                                                                   numberOfMonitoringEntries_,
                                                                                   submissionRequest, 
                                                                                   repository_, 
                                                                                   placementPolicy_, 
                                                                                   stateMachine_,
                                                                                   this);
        new Thread(worker).start();
        return taskIdentifier;
    }
            
    /**
     * Suspend a virtual machine.
     * 
     * @param location     The virtual machine location
     * @return             true if everything ok, false otherwise
     */
    private boolean suspend(VirtualMachineLocation location)
    {      
        log_.debug(String.format("Suspending virtual machine %s", location.getVirtualMachineId()));
               
        if (!repository_.checkVirtualMachineStatus(location, VirtualMachineStatus.RUNNING))
        {
            return false;
        }
        
        NetworkAddress localController = repository_.getLocalControllerControlDataAddress(location);
        if (localController == null)
        {
            log_.debug(String.format("Unable to get local controller description from virtual machine: %s",
                                     location.getVirtualMachineId()));
            return false;
        }
        
        LocalControllerAPI communicator = CommunicatorFactory.newLocalControllerCommunicator(localController);
        boolean returnValue = communicator.suspendVirtualMachine(location.getVirtualMachineId());
        if (!returnValue)
        {
            log_.error(String.format("Unable to suspend virtual machine: %s", location.getVirtualMachineId()));
            return false;
        }
        
        returnValue = repository_.changeVirtualMachineStatus(location, VirtualMachineStatus.PAUSED);  
        if (!returnValue)
        {
            log_.error("Unable to change the virtual machine status");
            return false;
        }
        
        return true;
    }
    
    /**
     * Resume a virtual machine.
     * 
     * @param location     The virtual machine location
     * @return             true if everything ok, false otherwise
     */
    private boolean resume(VirtualMachineLocation location)
    {
        log_.debug(String.format("Resuming virtual machine %s", location.getVirtualMachineId()));
                
        if (!repository_.checkVirtualMachineStatus(location, VirtualMachineStatus.PAUSED))
        {
            return false;
        }
        
        NetworkAddress localController = repository_.getLocalControllerControlDataAddress(location);
        if (localController == null)
        {
            log_.debug(String.format("Unable to get local controller description from virtual machine: %s",
                                     location.getVirtualMachineId()));
            return false;
        }
        
        LocalControllerAPI communicator = CommunicatorFactory.newLocalControllerCommunicator(localController);
        boolean isResumed = communicator.resumeVirtualMachine(location.getVirtualMachineId());
        if (!isResumed)
        {
            log_.error(String.format("Unable to resume virtual machine: %s", location.getVirtualMachineId()));
            return false;
        }
        
        repository_.changeVirtualMachineStatus(location, VirtualMachineStatus.RUNNING);        
        return true;
    }    
  
    /**
     * Shutdown a virtual machine.
     * 
     * @param location     The virtual machine location 
     * @return             true if everything ok, false otherwise
     */
    private boolean shutdown(VirtualMachineLocation location)
    {
        String virtualMachineId = location.getVirtualMachineId();
        log_.debug(String.format("Shutdown down virtual machine %s", virtualMachineId));
        
        if (!repository_.checkVirtualMachineStatus(location, VirtualMachineStatus.RUNNING) &&
            !repository_.checkVirtualMachineStatus(location, VirtualMachineStatus.SHUTDOWN_PENDING))
        {
            return false;
        }

        NetworkAddress localController = repository_.getLocalControllerControlDataAddress(location);
        if (localController == null)
        {
            log_.debug(String.format("Unable to get local controller description from virtual machine: %s",
                                     virtualMachineId));
            return false;
        }
        
        LocalControllerAPI communicator = CommunicatorFactory.newLocalControllerCommunicator(localController);
        boolean isShutdown = communicator.shutdownVirtualMachine(virtualMachineId);
        if (!isShutdown)
        {
            log_.error(String.format("Unable to shutdown virtual machine: %s", virtualMachineId));
            return false;
        }
                                     
        boolean isChanged = repository_.changeVirtualMachineStatus(location, VirtualMachineStatus.SHUTDOWN_PENDING);   
        if (!isChanged)
        {
            log_.error("Failed to change virtual machine status");
            return false;
        }
        
        return true;
    }  
    
    /**
     * Reboot a virtual machine.
     * 
     * @param location     The virtual machine location 
     * @return             true if everything ok, false otherwise
     */
    private boolean reboot(VirtualMachineLocation location)
    {
        String virtualMachineId = location.getVirtualMachineId();
        log_.debug(String.format("Rebooting virtual machine %s", virtualMachineId));
        
        if (!repository_.checkVirtualMachineStatus(location, VirtualMachineStatus.RUNNING))
        {
            return false;
        }

        NetworkAddress localController = repository_.getLocalControllerControlDataAddress(location);
        if (localController == null)
        {
            log_.debug(String.format("Unable to get local controller description from virtual machine: %s",
                                     virtualMachineId));
            return false;
        }
        
        LocalControllerAPI communicator = CommunicatorFactory.newLocalControllerCommunicator(localController);
        boolean isRebooted = communicator.rebootVirtualMachine(virtualMachineId);
        if (!isRebooted)
        {
            log_.error(String.format("Unable to reboot virtual machine: %s", virtualMachineId));
            return false;
        }
                                     
        boolean isChanged = repository_.changeVirtualMachineStatus(location, VirtualMachineStatus.RUNNING);   
        if (!isChanged)
        {
            log_.error("Failed to change virtual machine status");
            return false;
        }
        
        return true;
    }  
    
    /**
     * Destroy a virtual machine.
     * 
     * @param location     The virtual machine location
     * @return             true if everything ok, false otherwise
     */
    private boolean destroy(VirtualMachineLocation location)
    {
        log_.debug(String.format("Destroying virtual machine %s", location.getVirtualMachineId()));
                
        NetworkAddress localController = repository_.getLocalControllerControlDataAddress(location);
        if (localController == null)
        {
            log_.debug(String.format("Unable to get local controller description from virtual machine: %s",
                                     location.getVirtualMachineId()));
            return false;
        }
        
        LocalControllerAPI communicator = CommunicatorFactory.newLocalControllerCommunicator(localController);
        boolean returnValue = communicator.destroyVirtualMachine(location.getVirtualMachineId());
        if (!returnValue)
        {
            log_.error(String.format("Unable to destroy virtual machine: %s", location.getVirtualMachineId()));
            return false;
        }

        returnValue = repository_.dropVirtualMachineData(location);
        if (!returnValue)
        {
            log_.error(String.format("Unable to remove virtual machine data for: %s", 
                                     location.getVirtualMachineId()));
            return false;
        }
        
        log_.debug("Destroy was successfull!");
        return true;
    }

    /**
     * Adds a virtual cluster response.
     *
     * @param taskIdentifier        The task identifier
     * @param submissionResponse    The virtual machine submisson response
     */
    @Override
    public void onSubmissionFinished(String taskIdentifier, VirtualMachineSubmissionResponse submissionResponse) 
    {
        Guard.check(taskIdentifier, submissionResponse);
        log_.debug(String.format("Adding submission %s response", taskIdentifier));
        submissionResponses_.put(taskIdentifier, submissionResponse);
        stateMachine_.onVirtualMachineSubmissionFinished();
    }
    
    /**
     * Returns virtual cluster response if available.
     * 
     * @param taskIdentifier   The submission task identifier
     * @return                 The virtual machine submission response
     */
    public VirtualMachineSubmissionResponse getVirtualMachineSubmissionResponse(String taskIdentifier)
    {
        Guard.check(taskIdentifier);        
        
        VirtualMachineSubmissionResponse submissionResponse = submissionResponses_.get(taskIdentifier);
        if (submissionResponse != null)
        {
            submissionResponses_.remove(taskIdentifier);
        }
        
        log_.debug(String.format("Returning virtual machine submission response: %s", submissionResponse));
        return submissionResponse;
    } 
    
    /**
     * Processes the control command.
     * 
     * @param command   The control command
     * @param location  The virtual machine location
     * @return          true if everything ok, false otherwise
     */
    public boolean processControlCommand(VirtualMachineCommand command, VirtualMachineLocation location)
    {
        Guard.check(command, location);
        log_.debug(String.format("Starting virtual machine control command: %s processing", command));
        
        boolean isProcessed = false;
        switch (command)
        {       
            case SUSPEND:
                isProcessed = suspend(location);
                break;
           
            case RESUME:
                isProcessed = resume(location);
                break;
            
            case SHUTDOWN:
                isProcessed = shutdown(location);
                break;
                
            case REBOOT:
                isProcessed = reboot(location);
                break;
                
            case DESTROY:
                isProcessed = destroy(location);
                break;
            
                
            default:
                log_.error(String.format("Wrong command specified: %s", command));
        }
       
        return isProcessed;
    }

}
