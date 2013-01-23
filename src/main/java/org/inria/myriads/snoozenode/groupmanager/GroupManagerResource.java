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
package org.inria.myriads.snoozenode.groupmanager;

import java.util.ArrayList;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.groupmanager.repository.GroupLeaderRepositoryInformation;
import org.inria.myriads.snoozecommon.communication.groupmanager.repository.GroupManagerRepositoryInformation;
import org.inria.myriads.snoozecommon.communication.localcontroller.AssignedGroupManager;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.rest.api.GroupManagerAPI;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.discovery.VirtualMachineDiscoveryResponse;
import org.inria.myriads.snoozecommon.communication.virtualcluster.requests.MetaDataRequest;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualClusterSubmissionRequest;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualClusterSubmissionResponse;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineSubmissionRequest;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineSubmissionResponse;
import org.inria.myriads.snoozecommon.communication.virtualmachine.ClientMigrationRequest;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.groupmanager.statemachine.VirtualMachineCommand;
import org.inria.myriads.snoozenode.groupmanager.virtualmachinediscovery.VirtualMachineDiscovery;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Group manager resource class.
 * 
 * @author Eugen Feller
 */
public final class GroupManagerResource extends ServerResource 
    implements GroupManagerAPI
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupManagerResource.class);

    /** Define group manager backend. */
    private GroupManagerBackend backend_;
    
    /**
     * Constructor.
     */
    public GroupManagerResource()
    {
        log_.debug("Starting group manager resource");
        backend_ = (GroupManagerBackend) getApplication().getContext().getAttributes().get("backend");
    }
       
    /**
     * Dispatches the virtual cluster submission request.
     * (called by the client)
     * 
     * @param  virtualClusterDescription  The virtual cluster description
     * @return                            The assigned task identifier
     */
    @Override
    public String startVirtualCluster(VirtualClusterSubmissionRequest virtualClusterDescription) 
    {
        Guard.check(virtualClusterDescription);
        log_.debug("Received virtual cluster start request");
        
        if (!isGroupLeaderActive())
        {
            return null;
        }
        
        String taskIdentifier = backend_.getGroupLeaderInit().
                                         getVirtualClusterManager().
                                         startVirtualClusterSubmission(virtualClusterDescription);  
        log_.debug(String.format("Returning task identifier: %s", taskIdentifier));
        return taskIdentifier;  
    }
        
    /** 
     * Handles the join request of a group manager.
     * (called by a group manager).
     * 
     * @param  groupManager   The group manager description
     * @return                true if everything ok, false otherwise
     */
    @Override
    public boolean joinGroupLeader(GroupManagerDescription groupManager) 
    {
        Guard.check(groupManager);
        log_.debug(String.format("Received join request from group manager %s at address %s",
                                 groupManager.getId(), 
                                 groupManager.getListenSettings().getControlDataAddress().getAddress()));
    
        if (!isGroupLeaderActive())
        {
            return false;
        }
        
        boolean isAdded = backend_.getGroupLeaderInit()
                                  .getRepository()
                                  .addGroupManagerDescription(groupManager);
        return isAdded;
    }

    /** 
     * Assign local controller to a group manager.
     * (called by the local controller).
     *  
     * @param  localControllerDescription     The local controller description
     * @return                                The group manager description
     */
    @Override
    public AssignedGroupManager assignLocalController(LocalControllerDescription localControllerDescription) 
    {
        Guard.check(localControllerDescription);
        log_.debug(String.format("Received assign local controller request from %s at address %s" +
                                  " with capacity: %s", 
                                  localControllerDescription.getId(), 
                                  localControllerDescription.getControlDataAddress().getAddress(),
                                  localControllerDescription.getTotalCapacity()));
    
        if (!isGroupLeaderActive())
        {
            return null;
        }
    
        AssignedGroupManager assignment = backend_.getGroupLeaderInit()
                                                  .assignLocalController(localControllerDescription);
        log_.debug(String.format("Assigned group manager reference is: %s", assignment));
        return assignment;
    }
    
    /**
     * Routine to discover the group manager hosting a virtual machine.
     * 
     * @param virtualMachineId   The virtual machine identifier
     * @return                   The discovery response
     */
    @Override
    public VirtualMachineDiscoveryResponse discoverVirtualMachine(String virtualMachineId) 
    {
        Guard.check(virtualMachineId);
        log_.debug(String.format("Starting virtual machine discovery for: %s", virtualMachineId));
        
        if (!isGroupLeaderActive())
        {
            return null;
        }
        
        VirtualMachineDiscovery discovery = backend_.getGroupLeaderInit().getVirtualMachineDiscovery();
        VirtualMachineDiscoveryResponse discoveryResponse = discovery.startVirtualMachineDiscovery(virtualMachineId);
        return discoveryResponse;
    }
    
    /**
     * Routine the group leader information.
     * 
     * @param numberOfMonitoringEntries     The number of monitoring entries
     * @return                              The group leader repository information
     */
    @Override
    public GroupLeaderRepositoryInformation getGroupLeaderRepositoryInformation(int numberOfMonitoringEntries) 
    {
        log_.debug("Received a request to return group leader repository information");
        
        if (!isGroupLeaderActive())
        {
            return null;
        }
        
        GroupLeaderRepositoryInformation groupLeaderInformation = new GroupLeaderRepositoryInformation();
        try
        {
            ArrayList<GroupManagerDescription> groupManagers = 
                backend_.getGroupLeaderInit().getRepository().getGroupManagerDescriptions(numberOfMonitoringEntries);
            groupLeaderInformation.setGroupManagerDescriptions(groupManagers);
        }
        catch (Exception exception) 
        {
            log_.error("Exception during group leader information retrieval", exception);
        }   
        
        log_.debug(String.format("Returning reference: %s", groupLeaderInformation));
        return groupLeaderInformation;
    }
    
    /**
     * Starts virtual machines on the group manager.
     * (called by group leader)
     * 
     * @param submissionRequest        The virtual machine submission
     * @return                         The task identifier
     */
    @Override
    public String startVirtualMachines(VirtualMachineSubmissionRequest submissionRequest) 
    {
        Guard.check(submissionRequest);
        log_.debug("Received start virtual machines command from group leader");
        
        if (!isGroupManagerActive())
        {
            return null;
        }
        
        String taskIdentifier = backend_.getGroupManagerInit()
                                        .getStateMachine()
                                        .startVirtualMachines(submissionRequest);
        return taskIdentifier;
    }
   
    /** 
     * Routine to join the group manager.
     * (called by a local controller).
     *  
     * @param  localController   The local controller description
     * @return                   true if everything ok, false otherwise
     */
    @Override
    public boolean joinGroupManager(LocalControllerDescription localController) 
    {
        Guard.check(localController);
        log_.debug(String.format("Received join request from %s local controller %s at address %s " +
                                 "with total capacity: %s", 
                                 localController.getStatus(),
                                 localController.getId(), 
                                 localController.getControlDataAddress().getAddress(),
                                 localController.getTotalCapacity()));   
        
        if (!isGroupManagerActive())
        {
            return false;
        }
        
        boolean isAdded = backend_.getGroupManagerInit()
                                  .getRepository()
                                  .addLocalControllerDescription(localController);
        return isAdded;
    }
    
    /**
     * Routine to suspend a virtual machine.
     * 
     * @param location      The virtual machine location
     * @return              true if everything ok, false otherwise
     */
    @Override
    public boolean suspendVirtualMachine(VirtualMachineLocation location) 
    {
        Guard.check(location);
        log_.debug(String.format("Received virtual machine suspend request for: %s", location.getVirtualMachineId()));
        
        if (!isGroupManagerActive())
        {
            return false;
        }
        
        boolean isSuspended = backend_.getGroupManagerInit()
                                      .getStateMachine()
                                      .controlVirtualMachine(VirtualMachineCommand.SUSPEND, location);
        return isSuspended;
    }
    
    /**
     * Routine to resume a virtual machine.
     * 
     * @param location      The virtual machine location
     * @return              true if everything ok, false otherwise
     */
    @Override
    public boolean resumeVirtualMachine(VirtualMachineLocation location) 
    {
        Guard.check(location);
        log_.debug(String.format("Received virtual machine resume request for: %s",  location.getVirtualMachineId()));
        
        if (!isGroupManagerActive())
        {
            return false;
        }
        
        boolean isResumed = backend_.getGroupManagerInit()
                                    .getStateMachine()
                                    .controlVirtualMachine(VirtualMachineCommand.RESUME, location);
        
        return isResumed;
    }
    
    /**
     * Routine to shutdown a virtual machine.
     * 
     * @param location      The virtual machine location
     * @return              true if everything ok, false otherwise
     */
    @Override
    public boolean shutdownVirtualMachine(VirtualMachineLocation location) 
    {
        Guard.check(location);
        log_.debug(String.format("Received virtual machine shutdown request for: %s", 
                                 location.getVirtualMachineId()));
        
        if (!isGroupManagerActive())
        {
            return false;
        }
        
        boolean isShutdown = backend_.getGroupManagerInit()
                                     .getStateMachine()
                                     .controlVirtualMachine(VirtualMachineCommand.SHUTDOWN, location);
        return isShutdown;
    }
    
    /**
     * Routine to reboot a virtual machine.
     * 
     * @param location      The virtual machine location
     * @return              true if everything ok, false otherwise
     */
    @Override
    public boolean rebootVirtualMachine(VirtualMachineLocation location) 
    {
        Guard.check(location);
        log_.debug(String.format("Received virtual machine reboot request for: %s", 
                                 location.getVirtualMachineId()));
        
        if (!isGroupManagerActive())
        {
            return false;
        }
        
        boolean isRebooted = backend_.getGroupManagerInit()
                                     .getStateMachine()
                                     .controlVirtualMachine(VirtualMachineCommand.REBOOT, location);
        return isRebooted;
    }
    
    /**
     * Routine to shutdown a virtual machine.
     * 
     * @param location      The virtual machine location
     * @return              true if everything ok, false otherwise
     */
    @Override
    public boolean destroyVirtualMachine(VirtualMachineLocation location) 
    {
        Guard.check(location);
        log_.debug(String.format("Received virtual machine destroy request for: %s on %s", 
                                 location.getVirtualMachineId(), location.getLocalControllerId()));
        
        if (!isGroupManagerActive())
        {
            return false;
        }
        
        boolean isDestroyed = backend_.getGroupManagerInit()
                                      .getStateMachine()
                                      .controlVirtualMachine(VirtualMachineCommand.DESTROY, location);
        return isDestroyed;
    }
    
    /**
     * Checks if a virtual machine is active on all local controller.
     * 
     * @param virtualMachineId      The virtual machine identifier
     * @return                      The local controller identifier
     */
    @Override
    public String searchVirtualMachine(String virtualMachineId)
    {
        Guard.check(virtualMachineId);
        log_.debug(String.format("Checking if virtual machine %s is hosted on any local controller", 
                                 virtualMachineId));
        
        if (!isGroupManagerActive())
        {
            return null;
        }
        
        String localControllerId = backend_.getGroupManagerInit()
                                           .getRepository()
                                           .searchVirtualMachine(virtualMachineId);
        return localControllerId;
    }
       
    /**
     * Checks if a virtual machine is active a particular local controller.
     * 
     * @param location      The virtual machine location
     * @return              true if everything ok, false otherwise
     */
    @Override
    public boolean hasVirtualMachine(VirtualMachineLocation location) 
    {
        Guard.check(location);
        log_.debug(String.format("Checking if virtual machine %s is hosted on local controller %s", 
                                 location.getVirtualMachineId(), location.getLocalControllerId()));
        
        if (!isGroupManagerActive())
        {
            return false;
        }
        
        boolean hasVirtualMachine = backend_.getGroupManagerInit()
                                            .getRepository()
                                            .hasVirtualMachine(location);
        return hasVirtualMachine;
    }
 
    /**
     * Checks if a virtual machine is active a particular local controller.
     * 
     * @param localControllerAddress      The virtual machine location
     * @return                            true if everything ok, false otherwise
     */
    @Override
    public String hasLocalController(NetworkAddress localControllerAddress) 
    {
        Guard.check(localControllerAddress);
        log_.debug(String.format("Checking if information for local controller %s: %s exists", 
                                 localControllerAddress.getAddress(), localControllerAddress.getPort()));
        
        if (!isGroupManagerActive())
        {
            return null;
        }
        
        String hasLocalControllerId = backend_.getGroupManagerInit()
                                              .getRepository()
                                              .hasLocalController(localControllerAddress);
        return hasLocalControllerId;
    }    
    
    /**
     * Routine to get virtual machine information.
     * 
     * @param request     The meta data request
     * @return            The virtual machine meta data
     */
    @Override
    public VirtualMachineMetaData getVirtualMachineMetaData(MetaDataRequest request) 
    {
        Guard.check(request);
        String virtualMachineId = request.getVirtualMachineLocation().getVirtualMachineId();
        log_.debug(String.format("Received virtual machine information request from client for: %s", 
                                 virtualMachineId));
           
        if (!isGroupManagerActive())
        {
            return null;
        }
        
        VirtualMachineLocation location = request.getVirtualMachineLocation();
        int numberOfMonitoringEntries = request.getNumberOfMonitoringEntries();
        GroupManagerRepository repository = backend_.getGroupManagerInit().getRepository();
        VirtualMachineMetaData virtualMachine = repository.getVirtualMachineMetaData(location, 
                                                                                     numberOfMonitoringEntries);
        if (virtualMachine == null)
        {
            log_.debug("No meta data available for this virtual machine!");   
            virtualMachine = new VirtualMachineMetaData();
            virtualMachine.getVirtualMachineLocation().setVirtualMachineId(virtualMachineId);
            virtualMachine.setStatus(VirtualMachineStatus.OFFLINE);
            return virtualMachine;
        } 
        
        return virtualMachine;
    }
                
    /**
     * Return the group leader information.
     * 
     * @param numberOfMonitoringEntries    The number of monitoring entries
     * @return                             The group manager repository information
     */
    @Override
    public GroupManagerRepositoryInformation getGroupManagerRepositoryInformation(int numberOfMonitoringEntries) 
    {
        log_.debug("Received a request to return group manager repository information");
        
        if (!isGroupManagerActive())
        {
            return null;
        }
        
        GroupManagerRepositoryInformation groupManagerInformation = new GroupManagerRepositoryInformation();
        ArrayList<LocalControllerDescription> localControllers = 
          backend_.getGroupManagerInit().getRepository().getLocalControllerDescriptions(numberOfMonitoringEntries,
                                                                                         false);
        groupManagerInformation.setLocalControllerDescriptions(localControllers);
        
        log_.debug(String.format("Returning reference: %s, number of local controllers: %d", 
                                 groupManagerInformation,
                                 groupManagerInformation.getLocalControllerDescriptions().size()));
        return groupManagerInformation;
    }
        
    /**
     * Checks group manager initialization.
     * 
     * @return  true if initialized, false otherwise
     */
    private boolean isGroupManagerActive()
    {
        if (backend_ == null)
        {
            log_.debug("Backend is not initialized yet!");
            return false;
        }
        
        if (backend_.getGroupManagerInit() == null)
        {
            log_.debug("Group manager logic is not available yet!");
            return false;
        }    
                
        return true;
    }
    
    /**
     * Checks group leader initialization.
     * 
     * @return  true if initialized, false otherwise
     */
    private boolean isGroupLeaderActive() 
    {
        if (backend_ == null)
        {
            log_.debug("Backend is not initialized yet!");
            return false;
        }
        
        if (backend_.getGroupLeaderInit() == null)
        {
            log_.debug("Group leader logic is not available yet!");
            return false;
        }
        
        return true;
    }
    
    /**
     * Suspends the energy saver.
     * 
     * @return   The group manager repository information
     */
    @Override
    public boolean suspendEnergySaver() 
    {
        log_.debug("Received a request to suspend the energy saver");
        
        if (!isGroupManagerActive())
        {
            return false;
        }
        
        backend_.getGroupManagerInit().getEnergySaver().setSuspend();
        return true;
    }
    
    /**
     * Resumes the energy saver.
     * 
     * @return   The group manager repository information
     */
    @Override
    public boolean resumeEnergySaver() 
    {
        log_.debug("Received a request to resume the energy saver");
        
        if (!isGroupManagerActive())
        {
            return false;
        }
        
        backend_.getGroupManagerInit().getEnergySaver().wakeup();
        log_.debug("Energy saver wakeup done!");
        return true;
    }
    
    /**
     * Drops virtual machine meta data.
     * 
     * @param virtualMachineLocation        The virtual machine location
     * @return                              true if everything ok, false otherwise
     */
    @Override
    public boolean dropVirtualMachineMetaData(VirtualMachineLocation virtualMachineLocation)
    {
        Guard.check(virtualMachineLocation);
        boolean isDropped = backend_.getGroupManagerInit()
                                        .getRepository()
                                        .dropVirtualMachineData(virtualMachineLocation);
        return isDropped;
    }

    /**
     * Returns the virtual machine submission finish.
     * 
     * @param taskIdentifier    The virtual machine task identifier
     * @return                  The virtual machine submission response
     */
    @Override
    public VirtualMachineSubmissionResponse getVirtualMachineSubmissionResponse(String taskIdentifier) 
    {
        Guard.check(taskIdentifier);
        log_.debug(String.format("Received a request for virtual machine %s response lookup", taskIdentifier));
        
        if (!isGroupManagerActive())
        {
            return null;
        }
 
        VirtualMachineSubmissionResponse submissionResponse = backend_.getGroupManagerInit()
                                                                  .getStateMachine()
                                                                  .getVirtualMachineSubmissionResponse(taskIdentifier);
        log_.debug(String.format("Returning virtual machine response: %s", submissionResponse));
        return submissionResponse;
    }
    
    /**
     * Returns the virtual cluster response if available.
     * 
     * @param taskIdentifier    The task identifier
     * @return                  The virtual cluster response
     */
    @Override
    public VirtualClusterSubmissionResponse getVirtualClusterResponse(String taskIdentifier) 
    {
        Guard.check(taskIdentifier);
        log_.debug(String.format("Received a request for virtual cluster %s response lookup", taskIdentifier));
        
        if (!isGroupLeaderActive())
        {
            return null;
        }
 
        VirtualClusterSubmissionResponse respomse = backend_.getGroupLeaderInit()
                                                            .getVirtualClusterManager()
                                                            .getVirtualClusterResponse(taskIdentifier);
        log_.debug(String.format("Returning virtual cluster response: %s", respomse));
        return respomse;
    }


    /**
     * Migrate a virtual machine.
     * (call by the client)
     * 
     * @param clientMigrationRequest     The client migration Request
     * @return                           true if ok false otherwise
     */
    public boolean migrateVirtualMachine(ClientMigrationRequest clientMigrationRequest) 
    {
        Guard.check(clientMigrationRequest);
        if (!isGroupManagerActive())
        {
            return false;
        }
        
        boolean isMigrated = backend_.getGroupManagerInit()
                .getStateMachine().startMigration(clientMigrationRequest);

        return isMigrated;
        
    }

   
}
