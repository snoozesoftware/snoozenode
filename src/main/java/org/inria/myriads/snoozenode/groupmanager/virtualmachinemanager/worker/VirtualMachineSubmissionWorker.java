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
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineSubmissionRequest;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineSubmissionResponse;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.estimator.api.impl.StaticDynamicResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPlan;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPolicy;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.impl.Static;
import org.inria.myriads.snoozenode.groupmanager.statemachine.api.StateMachine;
import org.inria.myriads.snoozenode.groupmanager.virtualmachinemanager.listener.VirtualMachineManagerListener;
import org.inria.myriads.snoozenode.message.ManagementMessage;
import org.inria.myriads.snoozenode.message.ManagementMessageType;
import org.inria.myriads.snoozenode.util.ExternalNotifierUtils;
import org.inria.myriads.snoozenode.util.ManagementUtils;
import org.inria.snoozenode.external.notifier.ExternalNotificationType;
import org.inria.snoozenode.external.notifier.ExternalNotifier;
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
    private VirtualMachineSubmissionRequest submissionRequest_;
    
    /** Submission policy. */
    private PlacementPolicy placementPolicy_;
    
    /** Static policy. */
    private PlacementPolicy staticPlacementPolicy_;
    
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
    
    /** External Notifier. */
    private ExternalNotifier externalNotifier_;
    
    /**
     * Constructor.
     * 
     * @param taskIdentifier             The task identifier
     * @param numberOfMonitoringEntries  The number of monitoring entries
     * @param submissionRequest          The virtual machine submission request
     * @param repository                 The repository
     * @param placementPolicy            The placement policy
     * @param stateMachine               The state machine
     * @param estimator                  The resource estimator
     * @param managerListener            The manager listener
     * @param externalNotifier           The external Notifier
     */
    public VirtualMachineSubmissionWorker(String taskIdentifier,
                                          int numberOfMonitoringEntries,
                                          VirtualMachineSubmissionRequest submissionRequest, 
                                          GroupManagerRepository repository,
                                          PlacementPolicy placementPolicy,
                                          StateMachine stateMachine,
                                          ResourceDemandEstimator estimator,
                                          VirtualMachineManagerListener managerListener,
                                          ExternalNotifier externalNotifier
                                          )
    {
        taskIdentifier_ = taskIdentifier;
        numberOfMonitoringEntries_ = numberOfMonitoringEntries;
        submissionRequest_ = submissionRequest;
        repository_ = repository;
        placementPolicy_ = placementPolicy;
        stateMachine_ = stateMachine;
        managerListener_ = managerListener;
    
        staticPlacementPolicy_ = new Static();
        staticPlacementPolicy_.setEstimator(estimator);
        staticPlacementPolicy_.initialize();
        externalNotifier_ = externalNotifier;
    }
    
    /**
     * Updates the virtual machine repository information.
     * 
     * @param virtualMachine    The virtual machine
     */
    private void updateVirtualMachineRepositoryInformation(VirtualMachineMetaData virtualMachine)
    {       
        if (virtualMachine.getStatus().equals(VirtualMachineStatus.RUNNING))
        {
            boolean isAdded = repository_.addVirtualMachine(virtualMachine);
            if (!isAdded)
            {              
                ManagementUtils.updateVirtualMachineMetaData(virtualMachine,
                                                             VirtualMachineStatus.ERROR,
                                                             VirtualMachineErrorCode.FAILED_TO_UPDATE_REPOSITORY);
            }
        }
        
        ExternalNotifierUtils.send(
                externalNotifier_,
                ExternalNotificationType.MANAGEMENT,
                new ManagementMessage(ManagementMessageType.PROCESSED, virtualMachine),
                repository_.getGroupManagerId() + "." +
                virtualMachine.getVirtualMachineLocation().getLocalControllerId() + "." + 
                virtualMachine.getVirtualMachineLocation().getVirtualMachineId() + "." +
                "START"
                );
    }
    
    /**
     * Starts virtual machines assingned to a local controller.
     * 
     * @param localController   The local controller description
     * @return                  The virtual machine submission response
     */
    private VirtualMachineSubmissionResponse startVirtualMachines(LocalControllerDescription localController)
    {
        List<VirtualMachineMetaData> virtualMachines = localController.getAssignedVirtualMachines();
        log_.debug(String.format("Sending a request to start %d virtual machines to the local controller: %s", 
                                 virtualMachines.size(), localController.getId()));
        
        VirtualMachineSubmissionRequest submissionRequest = new VirtualMachineSubmissionRequest();
        submissionRequest.setVirtualMachineMetaData(localController.getAssignedVirtualMachines());
        LocalControllerAPI communicator = 
                CommunicatorFactory.newLocalControllerCommunicator(localController.getControlDataAddress());
        VirtualMachineSubmissionResponse submissionResponse = communicator.startVirtualMachines(submissionRequest); 
        log_.debug(String.format("Submission response received from local controller: %s", localController.getId()));
        return submissionResponse;
    }
    
    /**
     * Wakes up a local controller if it is passive.
     * 
     * @param localController   The local controller
     * @return                  true if everything ok, false otherwise
     */
    private boolean wakeupLocalControllerIfPassive(LocalControllerDescription localController)
    {
        boolean isWokenUp = true;
        
        if (localController.getStatus().equals(LocalControllerStatus.PASSIVE))
        {
            log_.debug("Found PASSIVE local controller! Will try to do wakeup!");
            isWokenUp = stateMachine_.onWakeupLocalController(localController);
        }
        
        return isWokenUp;
    }
    
    /**
     * Enforces the placement plan.
     * 
     * @param placementPlan    The placement plan
     * @return                 The virtual machine submissions response
     */ 
    private VirtualMachineSubmissionResponse enforcePlacementPlan(PlacementPlan placementPlan)
    {        
        ArrayList<VirtualMachineMetaData> allVirtualMachines = new ArrayList<VirtualMachineMetaData>();
        allVirtualMachines.addAll(placementPlan.gettUnassignedVirtualMachines());
        
        for (LocalControllerDescription localController : placementPlan.getLocalControllers())
        {             
            List<VirtualMachineMetaData> assignedVirtualMachines = localController.getAssignedVirtualMachines();
            log_.debug(String.format("Starting to enforce the placement plan for local controller: %s", 
                                     localController.getId()));
            
            boolean isWokenUp = wakeupLocalControllerIfPassive(localController);
            if (!isWokenUp)
            {
                ManagementUtils.updateAllVirtualMachineMetaData(assignedVirtualMachines,
                                                                VirtualMachineStatus.ERROR,
                                                                VirtualMachineErrorCode.LOCAL_CONTROLLER_WAKEUP_FAILED);
                allVirtualMachines.addAll(assignedVirtualMachines);
                continue;
            }
            
            VirtualMachineSubmissionResponse submissionResponse = startVirtualMachines(localController);
            if (submissionResponse == null)
            {
                ManagementUtils.updateAllVirtualMachineMetaData(assignedVirtualMachines,
                                                                VirtualMachineStatus.ERROR,
                                                                VirtualMachineErrorCode.INVALID_SUBMISSION_RESPONSE);
                allVirtualMachines.addAll(assignedVirtualMachines);
                continue;
            }
            
            for (VirtualMachineMetaData virtualMachine : submissionResponse.getVirtualMachineMetaData())
            {
                updateVirtualMachineRepositoryInformation(virtualMachine);
                allVirtualMachines.add(virtualMachine);
            }
        }
        
        VirtualMachineSubmissionResponse submissionResponse = new VirtualMachineSubmissionResponse();
        submissionResponse.setVirtualMachineMetaData(allVirtualMachines);
        return submissionResponse;
    }

    /** Run method. */
    @Override
    public void run()
    {
        log_.debug(String.format("Starting the virtual machine submission %s procedure", taskIdentifier_));
        
        ArrayList<VirtualMachineMetaData> virtualMachines = submissionRequest_.getVirtualMachineMetaData();
        
        ArrayList<VirtualMachineMetaData> boundVirtualMachines = new ArrayList<VirtualMachineMetaData>();
        ArrayList<VirtualMachineMetaData> freeVirtualMachines = new ArrayList<VirtualMachineMetaData>();
        // split virtual machines.
        splitVirtualMachines(virtualMachines, boundVirtualMachines, freeVirtualMachines);
        
        // build the construction plan.
        List<LocalControllerDescription> localControllers = 
                repository_.getLocalControllerDescriptions(numberOfMonitoringEntries_, false, true);
        
        PlacementPlan placementPlan = buildPlacementPlan(boundVirtualMachines, freeVirtualMachines, localControllers);
                
        VirtualMachineSubmissionResponse submissionResponse = enforcePlacementPlan(placementPlan);
        managerListener_.onSubmissionFinished(taskIdentifier_, submissionResponse);
    }

    
    protected PlacementPlan buildPlacementPlan(
            ArrayList<VirtualMachineMetaData> boundVirtualMachines, 
            ArrayList<VirtualMachineMetaData> freeVirtualMachines,
            List<LocalControllerDescription> localControllers
            )
    {
        //places static vms
        PlacementPlan boundPlacementPlan = staticPlacementPolicy_.place(boundVirtualMachines, localControllers);

        //place free vms
        PlacementPlan freePlacementPlan = placementPolicy_.place(freeVirtualMachines, localControllers);
        
        //merge boundPlacement.getLocalControllers and freePlacementPlan.getLocalControllers (remove monitoring ?)
        ArrayList<LocalControllerDescription> targetLocalControllers = new ArrayList<LocalControllerDescription>();
        targetLocalControllers.addAll(boundPlacementPlan.getLocalControllers());
        targetLocalControllers.addAll(freePlacementPlan.getLocalControllers());
        
        List<VirtualMachineMetaData> unassignedVirtualMachine = new ArrayList<VirtualMachineMetaData>();
        unassignedVirtualMachine.addAll(boundPlacementPlan.gettUnassignedVirtualMachines());
        unassignedVirtualMachine.addAll(freePlacementPlan.gettUnassignedVirtualMachines());

       return new PlacementPlan(targetLocalControllers, unassignedVirtualMachine);

    }

    /**
     * 
     * Splits the virtual machines.
     * 
     * @param virtualMachines           the virtual machines list
     * @param boundVirtualMachines      the bound virtual machines
     * @param freeVirtualMachines       the free virtual machines
     */
    protected void splitVirtualMachines(ArrayList<VirtualMachineMetaData> virtualMachines,
            ArrayList<VirtualMachineMetaData> boundVirtualMachines,
            ArrayList<VirtualMachineMetaData> freeVirtualMachines)
    {
        for (VirtualMachineMetaData virtualMachine : virtualMachines)
        {
            if (virtualMachine.getVirtualMachineLocation().getLocalControllerId() != null)
            {
                String localControllerId = virtualMachine.getVirtualMachineLocation().getLocalControllerId();
                log_.debug(String.format(
                        "The user force the start of this vm on local localcontroller %s",
                        localControllerId));
                LocalControllerDescription localController =
                        repository_.getLocalControllerDescription(localControllerId, 0, false);
                if (localController != null)
                {
                    log_.debug("Found a bound virtual machine");
                    boundVirtualMachines.add(virtualMachine);
                }
                else
                {
                    log_.debug("Found a free virtual machine");
                    freeVirtualMachines.add(virtualMachine);
                }
            }
            else
            {
                freeVirtualMachines.add(virtualMachine); 
            }
        }
        
    }
}
