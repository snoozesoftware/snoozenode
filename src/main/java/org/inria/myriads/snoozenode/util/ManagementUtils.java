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
package org.inria.myriads.snoozenode.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.inria.myriads.snoozecommon.communication.NodeRole;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.groupmanager.ListenSettings;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerStatus;
import org.inria.myriads.snoozecommon.communication.localcontroller.MonitoringThresholds;
import org.inria.myriads.snoozecommon.communication.localcontroller.Resource;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.migration.MigrationRequest;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineErrorCode;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.configurator.monitoring.HostMonitorSettings;
import org.inria.myriads.snoozenode.configurator.networking.NetworkingSettings;
import org.inria.myriads.snoozenode.heartbeat.message.HeartbeatMessage;
import org.inria.myriads.snoozenode.idgenerator.IdGeneratorFactory;
import org.inria.myriads.snoozenode.idgenerator.api.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Management utility.
 * 
 * @author Eugen Feller
 */
public final class ManagementUtils 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(ManagementUtils.class);
 
    /**
     * Hide the consturctor.
     */
    private ManagementUtils() 
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Updates all virtual machine met data.
     * 
     * @param virtualMachines   The virtual machines
     * @param status            The status
     * @param errorCode         The error code
     */
    public static void updateAllVirtualMachineMetaData(List<VirtualMachineMetaData> virtualMachines,
                                                       VirtualMachineStatus status,
                                                       VirtualMachineErrorCode errorCode) 
    {
        for (VirtualMachineMetaData metaData : virtualMachines)
        {
            updateVirtualMachineMetaData(metaData, status, errorCode);
        }   
    }

    /**
     * Updates the virtual machine meta status.
     * 
     * @param virtualMachine    The virtual machine meta data
     * @param status            The virtual machine status
     * @param errorCode         The virtual machine error code
     */
    public static void updateVirtualMachineMetaData(VirtualMachineMetaData virtualMachine,
                                                    VirtualMachineStatus status,
                                                    VirtualMachineErrorCode errorCode)
    {
        log_.debug(String.format("Updating virtual machine %s meta data! Setting status and error code to: %s, %s", 
                                 virtualMachine.getVirtualMachineLocation().getVirtualMachineId(),
                                 status, 
                                 errorCode));
        
        virtualMachine.setStatus(status);
        virtualMachine.setErrorCode(errorCode);
    }
    
    /** 
     * Creates a group manager from node role.
     *  
     * @param nodeRole                           The node role
     * @param networkingSettings                 The node parameters
     * @return                                   The group manager information
     */
    public static GroupManagerDescription createGroupManagerDescription(NodeRole nodeRole,
                                                                        NetworkingSettings networkingSettings) 
    {
        Guard.check(networkingSettings);
        log_.debug("Generating group manager description");
        
        GroupManagerDescription groupManager = new GroupManagerDescription();
        String id = UUID.randomUUID().toString();
        groupManager.setId(id);
        groupManager.setListenSettings(networkingSettings.getListen());
        if (nodeRole.equals(NodeRole.groupmanager))
        {
            groupManager.setHeartbeatAddress(networkingSettings.getMulticast().getGroupManagerHeartbeatAddress());
        } else
        {
            groupManager.setHeartbeatAddress(networkingSettings.getMulticast().getGroupLeaderHeartbeatAddress());
        }

        return groupManager;
    }
        
    /** 
     * Creates group manager info from heartbeat message.
     *  
     * @param heartbeatMessage  The heartbeat message
     * @return                  The group leader information
     */
    public static GroupManagerDescription createGroupLeaderDescriptionFromHeartbeat(HeartbeatMessage heartbeatMessage) 
    {
        Guard.check(heartbeatMessage);
        
        GroupManagerDescription groupManager = new GroupManagerDescription();
        groupManager.setId(heartbeatMessage.getId());
        groupManager.setListenSettings(heartbeatMessage.getListenSettings());
        return groupManager;
    }
        
    /** 
     * Creates a local controller from node configuration.
     *  
     * @param nodeConfiguration    The node configuration
     * @param totalCapacity        The total capacity
     * @return                     The local controller information
     */
    public static LocalControllerDescription createLocalController(NodeConfiguration nodeConfiguration,
                                                                   ArrayList<Double> totalCapacity,
                                                                   MonitoringThresholds thresholds
                                                                   ) 
    {
        Guard.check(nodeConfiguration);
        
        
        LocalControllerDescription localController = new LocalControllerDescription();
        
        log_.debug("Creating local controller description from node parameters for " + localController.getHostname());
        localController.setStatus(LocalControllerStatus.ACTIVE);
        localController.setControlDataAddress(nodeConfiguration.getNetworking().getListen().getControlDataAddress());
        localController.setHypervisorSettings(nodeConfiguration.getHypervisor());
        localController.setWakeupSettings(nodeConfiguration.getEnergyManagement().getDrivers().getWakeup());
        localController.setTotalCapacity(totalCapacity);
        localController.setThresholds(thresholds);
        HashMap<String, Resource> resources = localController.getHostResources();
        // for each monitor.
        for (HostMonitorSettings  monitors: nodeConfiguration.getHostMonitoringSettings().getHostMonitorSettings().values())
        {
            // for each resource managed by this monitor.
            for (Resource resource : monitors.getResources())
            {
                Resource resourceCopy = new Resource(resource, 0);
                resources.put(resourceCopy.getName(), resourceCopy);
            }
        }
        IdGenerator idGenerator = IdGeneratorFactory.createIdGenerator(nodeConfiguration.getNode());
        String id = idGenerator.generate(localController);
        if (StringUtils.isEmpty(id))
        {
            id = UUID.randomUUID().toString();
        }
        localController.setId(id);
        
        return localController;
    }
    
    /**
     * Creates a local controller from migration request.
     *  
     * @param migrationRequest    The migration request request
     * @return                    The local controller information
     */
    public static LocalControllerDescription createLocalController(MigrationRequest migrationRequest) 
    {
        Guard.check(migrationRequest);
        log_.debug("Creating local controller description from migration request");
        
        LocalControllerDescription localController = new LocalControllerDescription();
        localController.setControlDataAddress(migrationRequest.getDestinationVirtualMachineLocation()
                                                              .getLocalControllerControlDataAddress());
        localController.setHypervisorSettings(migrationRequest.getDestinationHypervisorSettings());
        return localController;
    }
    
    /**
     * Creates a heartbeat message from node params.
     * 
     * @param listenSettings       The listen settings
     * @param groupManagerId       The group manager identifier
     * @return                     The heartbeat messsage
     */
    public static HeartbeatMessage createHeartbeatMessage(ListenSettings listenSettings,
                                                          String groupManagerId) 
    {
        Guard.check(listenSettings, groupManagerId);
        
        HeartbeatMessage message = new HeartbeatMessage();
        message.setId(groupManagerId);
        message.setListenSettings(listenSettings);      
        return message;
    }
    
    /**
     * Marks the virtual machine as RUNNING.
     * 
     * @param metaData          The virtual machine meta data
     * @param localController   The local controller description
     */
    public static void setVirtualMachineRunning(VirtualMachineMetaData metaData, 
                                                LocalControllerDescription localController)
    {
        VirtualMachineLocation location = metaData.getVirtualMachineLocation();
        location.setLocalControllerId(localController.getId());
        location.setLocalControllerControlDataAddress(localController.getControlDataAddress());   
        metaData.setStatus(VirtualMachineStatus.RUNNING);  
    }
}
