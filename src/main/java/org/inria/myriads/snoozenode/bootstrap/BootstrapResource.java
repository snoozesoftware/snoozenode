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
package org.inria.myriads.snoozenode.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.groupmanager.repository.GroupLeaderRepositoryInformation;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerList;
import org.inria.myriads.snoozecommon.communication.rest.CommunicatorFactory;
import org.inria.myriads.snoozecommon.communication.rest.api.BootstrapAPI;
import org.inria.myriads.snoozecommon.communication.rest.api.GroupManagerAPI;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.migration.ClientMigrationRequestSimple;
import org.inria.myriads.snoozecommon.communication.virtualcluster.migration.MigrationRequest;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualClusterSubmissionRequest;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.request.HostListRequest;
import org.inria.myriads.snoozenode.groupmanager.statemachine.VirtualMachineCommand;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bootstrap resource.
 * 
 * @author Eugen Feller
 */
public final class BootstrapResource extends ServerResource 
    implements BootstrapAPI
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(BootstrapResource.class);
          
    /** Backend backend reference. */
    private BootstrapBackend backend_;
             
    /**
     * Constructor.
     */
    public BootstrapResource()
    {
        log_.debug("Starting bootstrap resource");
        backend_ = (BootstrapBackend) getApplication().getContext().getAttributes().get("backend");
    }

    /** 
     * Gets the current Group Leader Description.
     * 
     *  
     * @return   The group leader description
     */
    public GroupManagerDescription getGroupLeaderDescription() 
    {
        log_.debug("Received group leader information request");
        
        if (!isBackendActive())
        {
            log_.debug("Backend is not initialized yet!");
            return null;
        }
        
        GroupManagerDescription groupLeaderDescription = backend_.getGroupLeaderDescription();        
        if (groupLeaderDescription != null)
        {
            log_.debug(String.format("Returning group leader %s:%d", 
                                      groupLeaderDescription.getListenSettings().getControlDataAddress().getAddress(),
                                      groupLeaderDescription.getListenSettings().getControlDataAddress().getPort()));
        }
        
        return groupLeaderDescription;
    }
    
    
    /**
     * 
     * Gets the complete hierarchy.
     * 
     * @return                          The complete hierarchy of the system.
     * 
     */
    public synchronized GroupLeaderRepositoryInformation getCompleteHierarchy()
    {
        log_.debug("Received complete hierarchy request");
        
        if (!isBackendActive())
        {
            log_.debug("Backend is not initialized yet!");
            return null;
        }
        
        GroupLeaderRepositoryInformation hierarchy = backend_.getCompleteHierarchy();
        
        return hierarchy;
    }
    
   

    @Override
    public boolean destroyVirtualMachine(String virtualMachineId)
    {
        log_.debug("Processing destroy for virtual machine " + virtualMachineId);
        if (!isBackendActive())
        {
            log_.debug("Backend is not initialized yet!");
            return false;
        }
        return backend_.commandVirtualMachine(VirtualMachineCommand.DESTROY, virtualMachineId);
        
    }
    
    
    @Override
    public boolean suspendVirtualMachine(String  virtualMachineId)
    {
        log_.debug("Processing suspend for virtual machine " + virtualMachineId);
        if (!isBackendActive())
        {
            log_.debug("Backend is not initialized yet!");
            return false;
        }
        return backend_.commandVirtualMachine(VirtualMachineCommand.SUSPEND, virtualMachineId);
    }
    

    @Override
    public boolean rebootVirtualMachine(String virtualMachineId)
    {
        log_.debug("Processing reboot for virtual machine " + virtualMachineId);
        if (!isBackendActive())
        {
            log_.debug("Backend is not initialized yet!");
            return false;
        }
        return backend_.commandVirtualMachine(VirtualMachineCommand.REBOOT, virtualMachineId);
    }

    @Override
    public boolean shutdownVirtualMachine(String virtualMachineId)
    {
        log_.debug("Processing reboot for virtual machine " + virtualMachineId);
        if (!isBackendActive())
        {
            log_.debug("Backend is not initialized yet!");
            return false;
        }
        return backend_.commandVirtualMachine(VirtualMachineCommand.SHUTDOWN, virtualMachineId);
    }

    @Override
    public boolean resumeVirtualMachine(String virtualMachineId)
    {
        log_.debug("Processing reboot for virtual machine " + virtualMachineId);
        if (!isBackendActive())
        {
            log_.debug("Backend is not initialized yet!");
            return false;
        }
        return backend_.commandVirtualMachine(VirtualMachineCommand.RESUME, virtualMachineId);
    }
    
    

    @Override
    public boolean migrateVirtualMachine(ClientMigrationRequestSimple migrationRequest)
    {   
        MigrationRequest internalMigrationRequest = 
                backend_.getRepository().createMigrationRequest(migrationRequest);
        if (internalMigrationRequest == null)
        {
            return false;
        }
        VirtualMachineLocation oldLocation = internalMigrationRequest.getSourceVirtualMachineLocation();
        NetworkAddress groupManagerSource = oldLocation.getGroupManagerControlDataAddress();
        GroupManagerAPI communicator = CommunicatorFactory.newGroupManagerCommunicator(groupManagerSource);
        boolean isMigrating = communicator.migrateVirtualMachine(internalMigrationRequest);
        return isMigrating;
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
        
        GroupManagerDescription groupLeader = getGroupLeaderDescription();
        NetworkAddress groupLeaderAddress = groupLeader.getListenSettings().getControlDataAddress();
        
        GroupManagerAPI groupLeaderCommunicator = CommunicatorFactory.newGroupManagerCommunicator(groupLeaderAddress);
        String taskIdentifier = groupLeaderCommunicator.startVirtualCluster(virtualClusterDescription);  
        log_.debug(String.format("Returning task identifier: %s", taskIdentifier));
        
        return taskIdentifier;  
    }

    
    @Override
    public LocalControllerList geLocalControllerList()
    {
        if (!isBackendActive())
        {
            log_.debug("Backend is not initialized yet!");
            return null;
        }
        LocalControllerList localControllerList = backend_.getRepository().getLocalControllerList();
                 
        return localControllerList;
    }
    
    // admin zone
    @Override
    public List<GroupManagerDescription> getGroupManagerDescriptions(HostListRequest hostListRequest)
    {
        
        if (!isBackendActive())
        {
            log_.debug("Backend is not initialized yet!");
            return null;
        }
        String firstGroupManagerId = hostListRequest.getStart();
        int numberOfMonitoringEntries = hostListRequest.getNumberOfMonitoringEntries();
        int limit = hostListRequest.getLimit();
        List<GroupManagerDescription> groupManagerDescriptions = new ArrayList<GroupManagerDescription>();
        groupManagerDescriptions = backend_.getRepository().getGroupManagerDescriptions(
                firstGroupManagerId, 
                limit, 
                numberOfMonitoringEntries,
                backend_.getGroupLeaderDescription()
                );
        return groupManagerDescriptions;
    }


    @Override
    public List<LocalControllerDescription> getLocalControllerDescriptions(HostListRequest hostListRequest)
    {
        if (!isBackendActive())
        {
            log_.debug("Backend is not initialized yet!");
            return null;
        }
        String groupManagerId = hostListRequest.getGroupManagerId();
        String startLocalController = hostListRequest.getStart();
        int numberOfMonitoringEntries = hostListRequest.getNumberOfMonitoringEntries();
        int limit = hostListRequest.getLimit();
        
        List<LocalControllerDescription> localControllers = 
                backend_.getRepository().getLocalControllerDescriptions(
                        groupManagerId, 
                        startLocalController, 
                        limit, 
                        numberOfMonitoringEntries,
                        backend_.getGroupLeaderDescription()
                        );
        return localControllers;
    }
    

    @Override
    public List<VirtualMachineMetaData> getVirtualMachineDescriptions(HostListRequest hostListRequest)
    {
        if (!isBackendActive())
        {
            log_.debug("Backend is not initialized yet!");
            return null;
        }
        String groupManagerId = hostListRequest.getGroupManagerId();
        String localControllerId = hostListRequest.getLocalControllerId();
        String startVirtualMachine = hostListRequest.getStart();
        int numberOfMonitoringEntries = hostListRequest.getNumberOfMonitoringEntries();
        int limit = hostListRequest.getLimit();
        
        List<VirtualMachineMetaData> virtualMachines = backend_.getRepository().getVirtualMachineDescriptions(
                groupManagerId, 
                localControllerId,
                startVirtualMachine, 
                limit, 
                numberOfMonitoringEntries,
                backend_.getGroupLeaderDescription()
                );
        return virtualMachines;
    }
    
    
    /**
     * Starts a reconfiguration on the given groupManager.
     */
    public boolean startReconfiguration(String groupManagerId)
    {
        if (!isBackendActive())
        {
            log_.debug("Backend is not initialized yet!");
            return false;
        }
        GroupManagerDescription groupLeader = backend_.getGroupLeaderDescription();
        GroupManagerDescription groupManager = 
                backend_.getRepository().getGroupManagerDescription(groupManagerId, groupLeader);
        NetworkAddress groupManagerAddress = groupManager.getListenSettings().getControlDataAddress();
        GroupManagerAPI groupManagerCommunicator = CommunicatorFactory.newGroupManagerCommunicator(groupManagerAddress);
        
        boolean isStarted = groupManagerCommunicator.startReconfiguration();
        return isStarted;
    }
    
    /** 
     * Check backend activity.
     * 
     * @return  true if active, false otherwise
     */
    private boolean isBackendActive()
    {
        if (backend_ == null && backend_.isActive()) 
        {
            return false;
        }
        
        return true;
    }
}
