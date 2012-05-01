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
package org.inria.myriads.snoozenode.groupmanager.virtualmachinemanager.worker;

import java.util.ArrayList;
import java.util.List;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerStatus;
import org.inria.myriads.snoozecommon.communication.rest.CommunicatorFactory;
import org.inria.myriads.snoozecommon.communication.rest.api.LocalControllerAPI;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineErrorCode;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineSubmission;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.exception.VirtualMachineSubmissionException;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPolicy;
import org.inria.myriads.snoozenode.groupmanager.statemachine.api.StateMachine;
import org.inria.myriads.snoozenode.groupmanager.virtualmachinemanager.listener.VirtualMachineManagerListener;
import org.inria.myriads.snoozenode.util.ManagementUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Virtual machine submission worker.
 * 
 * @author Eugen Feller
 */
public final class VirtualMachineSubmissionWorker
    implements Runnable
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(VirtualMachineSubmissionWorker.class);
    
    /** Virtual machine submission request. */
    private VirtualMachineSubmission submissionRequest_;
    
    /** Submission policy. */
    private PlacementPolicy placementPolicy_;

    /** Virtual machine submission listener. */
    private VirtualMachineManagerListener managerListener_;

    /** State machine. */
    private StateMachine stateMachine_;
    
    /** Group manager repository. */
    private GroupManagerRepository repository_;
    
    /** Number of monitoring entries. */
    private int numberOfMonitoringEntries_;

    /** Task identifier. */
    private String taskIdentifier_;
    
    /**
     * Constructor.
     * 
     * @param taskIdentifier             The task identifier
     * @param numberOfMonitoringEntries  The number of monitoring entries
     * @param submissionRequest          The virtual machine submission request
     * @param repository                 The repository
     * @param placementPolicy            The placement policy
     * @param stateMachine               The state machine
     * @param managerListener            The manager listener
     */
    public VirtualMachineSubmissionWorker(String taskIdentifier,
                                          int numberOfMonitoringEntries,
                                          VirtualMachineSubmission submissionRequest, 
                                          GroupManagerRepository repository,
                                          PlacementPolicy placementPolicy,
                                          StateMachine stateMachine,
                                          VirtualMachineManagerListener managerListener)
    {
        taskIdentifier_ = taskIdentifier;
        numberOfMonitoringEntries_ = numberOfMonitoringEntries;
        submissionRequest_ = submissionRequest;
        repository_ = repository;
        placementPolicy_ = placementPolicy;
        stateMachine_ = stateMachine;
        managerListener_ = managerListener;
    }
    
    /**
     * Starts the virtual machine.
     * 
     * @param virtualMachine    The virtual machine meta data
     * @param localController   The local controller description
     * @return                  true if started, false otherwise
     */ 
    private boolean startVirtualMachine(VirtualMachineMetaData virtualMachine, 
                                        LocalControllerDescription localController)
    {
        LocalControllerAPI communicator = 
            CommunicatorFactory.newLocalControllerCommunicator(localController.getControlDataAddress());
        boolean isStarted = communicator.startVirtualMachine(virtualMachine);
        return isStarted;
    }
    
    /**
     * Schedules a virtual machine.
     * 
     * @param metaData               The virtual machine meta data
     * @param localControllers       The list of local controllers
     */
    private void scheduleVirtualMachine(VirtualMachineMetaData metaData,
                                        List<LocalControllerDescription> localControllers)
    {
        String virtualMachineId = metaData.getVirtualMachineLocation().getVirtualMachineId();
        log_.debug(String.format("Scheduling virtual machine: %s", virtualMachineId));
        
        boolean isAdded = false;
        try
        {
            String localControllerId = repository_.searchVirtualMachine(virtualMachineId);
            if (localControllerId != null)
            {
                metaData.setErrorCode(VirtualMachineErrorCode.ALREADY_RUNNING);
                throw new VirtualMachineSubmissionException("Virtual machine is already running! Unable to start!");
            }
            
            LocalControllerDescription localController = placementPolicy_.place(metaData, localControllers);
            if (localController == null)
            {
                metaData.setErrorCode(VirtualMachineErrorCode.NOT_ENOUGH_LOCAL_CONTROLLER_CAPACITY);
                throw new VirtualMachineSubmissionException("Unable to schedule virtual machine! Not enough capacity!");
            }
                    
            if (localController.getStatus().equals(LocalControllerStatus.PASSIVE))
            {
                log_.debug("Found PASSIVE local controller! Will try to do wakeup!");
                boolean isWokenUp = stateMachine_.onWakeupLocalController(localController);
                if (!isWokenUp)
                {
                    metaData.setErrorCode(VirtualMachineErrorCode.LOCAL_CONTROLLER_WAKEUP_FAILED);
                    throw new VirtualMachineSubmissionException("Failed to wakeup local controller");
                }
            }
                 
            boolean isStarted = startVirtualMachine(metaData, localController);
            if (!isStarted)
            {
                metaData.setErrorCode(VirtualMachineErrorCode.UNABLE_TO_START_ON_LOCAL_CONTROLLER);
                throw new VirtualMachineSubmissionException("Virtual machine could not be started!");
            }
                     
            log_.debug(String.format("Virtual machine %s started!", virtualMachineId));
            ManagementUtils.setVirtualMachineRunning(metaData, localController);
            isAdded = repository_.addVirtualMachine(metaData);
            if (!isAdded)
            {
                metaData.setErrorCode(VirtualMachineErrorCode.FAILED_TO_UPDATE_REPOSITORY);
                throw new VirtualMachineSubmissionException("Failed to update repository!");
            }
        }
        catch (VirtualMachineSubmissionException exception)
        {
            log_.warn(String.format("Something wrong happened during virtual machine %s submission: %s", 
                                    virtualMachineId, exception.getMessage()));
        }
        catch (Exception exception)
        {
            log_.error("Exception", exception);
        }
        finally
        {
            if (!isAdded)
            {
                metaData.setStatus(VirtualMachineStatus.ERROR);   
            }
        }
    }
        
    /** Run method. */
    @Override
    public void run() 
    {
        log_.debug(String.format("Starting submission %s procedure", taskIdentifier_));
          
        ArrayList<VirtualMachineMetaData> virtualMachines = submissionRequest_.getVirtualMachineMetaData(); 
        for (VirtualMachineMetaData virtualMachine : virtualMachines)
        {
            List<LocalControllerDescription> localControllers = 
                repository_.getLocalControllerDescriptions(numberOfMonitoringEntries_, false);
            scheduleVirtualMachine(virtualMachine, localControllers);
        }
        
        VirtualMachineSubmission submissionResponse = new VirtualMachineSubmission();
        submissionResponse.setVirtualMachineMetaData(virtualMachines);
        managerListener_.onSubmissionFinished(taskIdentifier_, submissionResponse);
    }
}
