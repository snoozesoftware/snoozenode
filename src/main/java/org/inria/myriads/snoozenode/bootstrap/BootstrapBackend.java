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
import java.util.HashMap;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.groupmanager.repository.GroupLeaderRepositoryInformation;
import org.inria.myriads.snoozecommon.communication.groupmanager.repository.GroupManagerRepositoryInformation;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.rest.CommunicatorFactory;
import org.inria.myriads.snoozecommon.communication.rest.api.GroupManagerAPI;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.configurator.database.DatabaseSettings;
import org.inria.myriads.snoozenode.database.DatabaseFactory;
import org.inria.myriads.snoozenode.database.api.BootstrapRepository;
import org.inria.myriads.snoozenode.groupmanager.statemachine.VirtualMachineCommand;
import org.inria.myriads.snoozenode.heartbeat.HeartbeatFactory;
import org.inria.myriads.snoozenode.heartbeat.listener.HeartbeatListener;
import org.inria.myriads.snoozenode.heartbeat.message.HeartbeatMessage;
import org.inria.myriads.snoozenode.util.ManagementUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bootstrap backend logic.
 * 
 * @author Eugen Feller
 */
public final class BootstrapBackend 
    implements HeartbeatListener
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(BootstrapBackend.class);
    
    /** The group leader description. */
    private GroupManagerDescription groupLeaderDescription_;
    
    /** The node configuration.*/
    private NodeConfiguration nodeConfiguration_;
    
    /** The repository.*/
    private BootstrapRepository repository_;
    
    /** Track backend activity.*/
    private boolean isActive_;
    
    /**
     * Bootstrap backend constructor.
     * 
     * @param nodeParameters  The node parameters
     * @throws Exception      Exception 
     */
    public BootstrapBackend(NodeConfiguration nodeParameters) 
        throws Exception 
    {
        Guard.check(nodeParameters);
        log_.debug("Starting bootstrap backend");
        nodeConfiguration_ = nodeParameters;
        NetworkAddress address = nodeParameters.getNetworking().getMulticast().getGroupLeaderHeartbeatAddress();
        int heartbeatTimeout = nodeParameters.getFaultTolerance().getHeartbeat().getTimeout();
        new Thread(HeartbeatFactory.newHeartbeatMulticastListener(address, 
                                                                  heartbeatTimeout,
                                                                  this),
                "HeartBeatListener"                                                  
                ).start();
        initializeRepository();
        isActive_ = true;
    }
    
    /**
     * 
     * Initialize the backend repository (read only).
     * 
     */
    private void initializeRepository()
    {
        DatabaseSettings settings = nodeConfiguration_.getDatabase();
        repository_ = DatabaseFactory.newBootstrapRepository(settings);
    }

    /** 
     * Return current group leader.
     *  
     * @return   Group leader information
     */
    public GroupManagerDescription getGroupLeaderDescription() 
    {
        return groupLeaderDescription_;
    }
    
    /**
     * Called by the heartbeat listener upon heartbeat arrival.
     * 
     * @param heartbeatMessage    Heartbeat message
     */
    public void onHeartbeatArrival(HeartbeatMessage heartbeatMessage) 
    {
        Guard.check(heartbeatMessage);
        log_.debug(String.format("Received group leader heartbeat message from: %s, " +
                                 "listen port: %d, " +
                                 "monitoring data port: %d",
                                 heartbeatMessage.getListenSettings().getControlDataAddress().getAddress(),
                                 heartbeatMessage.getListenSettings().getControlDataAddress().getPort(),
                                 heartbeatMessage.getListenSettings().getMonitoringDataAddress().getPort()));
    
        if (groupLeaderDescription_ == null || 
            groupLeaderDescription_.getId().compareTo(heartbeatMessage.getId()) != 0) 
        {
            log_.debug("Updating group leader information");        
            groupLeaderDescription_ = ManagementUtils.createGroupLeaderDescriptionFromHeartbeat(heartbeatMessage);
        }
    }

    /**
     * Called by the heartbeat listener upon heartbeat failure.
     */
    public void onHeartbeatFailure() 
    {
        log_.debug("Group leader does not exist or has failed");
        
        if (groupLeaderDescription_ != null)
        {
            groupLeaderDescription_ = null;
        }
    }

    
    /**
     * 
     * Gets the complete hierarchy of the snooze system.
     * 
     * @return      The group leader repository completed with localcontrollers informations.
     */
    public GroupLeaderRepositoryInformation getCompleteHierarchy()
    {

        log_.debug("Starting the hierarchy building");
        
        NetworkAddress groupLeaderAddress = groupLeaderDescription_.getListenSettings().getControlDataAddress();
        
        GroupLeaderRepositoryInformation groupLeaderInformation = 
                getGroupLeaderRepositoryInformation(groupLeaderAddress, 0);

        GroupLeaderRepositoryInformation hierarchy = new GroupLeaderRepositoryInformation();
        ArrayList<GroupManagerDescription> groupManagers = groupLeaderInformation.getGroupManagerDescriptions();
        hierarchy.setGroupManagerDescriptions(groupManagers);
        int i = 0;
        for (GroupManagerDescription groupManager : groupManagers) 
        {
            NetworkAddress address = groupManager.getListenSettings().getControlDataAddress();
            GroupManagerRepositoryInformation information = getGroupManagerRepositoryInformations(address, 0);
            HashMap<String, LocalControllerDescription> localControllers =
                    new HashMap<String, LocalControllerDescription>();
            for (LocalControllerDescription localController : information.getLocalControllerDescriptions())
            {
                localControllers.put(localController.getId(), localController);
            }
            hierarchy.getGroupManagerDescriptions().get(i).setLocalControllers(localControllers);  
            i++;
        }   
        return hierarchy;
    }
    
    /**
     * 
     * Gets the group leader repository informations.
     * 
     * @param groupLeaderAddress                The group leader address
     * @param numberOfBacklogEntries            The number of log wanted
     * @return                                  The group leader repository 
     */
    public GroupLeaderRepositoryInformation getGroupLeaderRepositoryInformation(
            NetworkAddress groupLeaderAddress, 
            int numberOfBacklogEntries)
    {      
        Guard.check(groupLeaderAddress);
        log_.info(String.format("Getting group leader repository information"));
        GroupManagerAPI groupLeaderCommunicator = 
                CommunicatorFactory.newGroupManagerCommunicator(groupLeaderAddress); 
        GroupLeaderRepositoryInformation information = 
            groupLeaderCommunicator.getGroupLeaderRepositoryInformation(numberOfBacklogEntries);
        return information;
    }
    
    /**
     * 
     * Gets the group manager repository informations.
     * 
     * @param groupManagerAddress               The group manager address
     * @param numberOfBacklogEntries            The number of logs wanted
     * @return                                  The group manager repository
     */
    public GroupManagerRepositoryInformation getGroupManagerRepositoryInformations(
            NetworkAddress groupManagerAddress, 
            int numberOfBacklogEntries)
    {
        Guard.check(groupManagerAddress);
        log_.info(String.format("Getting group manager repository informations"));
        
        GroupManagerAPI groupManagerCommunicator = 
                CommunicatorFactory.newGroupManagerCommunicator(groupManagerAddress); 
        GroupManagerRepositoryInformation information = 
                groupManagerCommunicator.getGroupManagerRepositoryInformation(numberOfBacklogEntries);
        return information;        
    }

    /**
     * 
     * Sends a command to a virtual machine.
     * 
     * @param command               command to send.
     * @param virtualMachineId      virtualMachine Id.
     * @return                      true iff everything is ok.
     */
    public synchronized boolean commandVirtualMachine(VirtualMachineCommand command, String virtualMachineId)
    {
       
        VirtualMachineMetaData virtualMachine = 
                getRepository().getVirtualMachineMetaData(virtualMachineId, 0, getGroupLeaderDescription());
        
        if (virtualMachine == null)
        {
            log_.debug(String.format("Virtual Machine %s not found in the system", virtualMachineId));
            return false;
        }
        
        VirtualMachineLocation location = virtualMachine.getVirtualMachineLocation();
        NetworkAddress groupManagerAddress = location.getGroupManagerControlDataAddress();
        GroupManagerAPI groupManagerCommunicator = CommunicatorFactory.newGroupManagerCommunicator(groupManagerAddress);
        boolean isProcessed = false;
        switch(command)
        {
            case DESTROY:
                isProcessed = groupManagerCommunicator.destroyVirtualMachine(location);
                break;
            case REBOOT :
                isProcessed = groupManagerCommunicator.rebootVirtualMachine(location);
                break;
            case SUSPEND :
                isProcessed = groupManagerCommunicator.suspendVirtualMachine(location);
                break;
            case RESUME :
                isProcessed = groupManagerCommunicator.resumeVirtualMachine(location);
                break;
            case SHUTDOWN : 
                isProcessed = groupManagerCommunicator.shutdownVirtualMachine(location);
                break;
            default : 
                log_.debug("Unknown command provided");
        }
        
        return isProcessed;
        
    }
    
    /**
     * 
     * Gets the bootstrap repository.
     * 
     * @return      The repository.
     */
    public BootstrapRepository getRepository()
    {
        return repository_;
    }

    /**
     * 
     * checks if backend is active.
     * 
     * @return  isActive
     */
    public boolean isActive()
    {
        return isActive_;
    }
    
    
}
