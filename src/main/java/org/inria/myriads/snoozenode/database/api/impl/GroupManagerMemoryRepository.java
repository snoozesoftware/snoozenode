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
package org.inria.myriads.snoozenode.database.api.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.inria.myriad.snoozenode.eventmessage.EventMessage;
import org.inria.myriad.snoozenode.eventmessage.EventType;
import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.VirtualMachineMonitoringData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozecommon.datastructure.LRUCache;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.monitoring.external.MonitoringExternalSettings;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.AggregatedVirtualMachineData;
import org.inria.myriads.snoozenode.monitoring.datasender.DataSenderFactory;
import org.inria.myriads.snoozenode.monitoring.datasender.api.DataSender;
import org.inria.myriads.snoozenode.utils.EventUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Group manager in-memory repository.
 * 
 * @author Eugen Feller
 */
public final class GroupManagerMemoryRepository 
    implements GroupManagerRepository 
{    
    /** Logger.*/
    private static final Logger log_ = LoggerFactory.getLogger(GroupManagerMemoryRepository.class);
    
    /** Group manager id. */
    private String groupManagerId_;
    
    /**
     * Virtual machine to local controller assignment map.
     *  
     * Key: Local controller identifier
     * Value: Local controller description
     */
    private HashMap<String, LocalControllerDescription> localControllerDescriptions_;
        
    /** List for the legacy IP addresses. */
    private List<String> legacyIpAddresses_;

    /** The maximum capacity. */
    private int maxCapacity_;
        
    
    /** External notifier. */
    private DataSender externalSender_;
    
    /** 
     * Constructor.
     * 
     * @param groupManagerId    The group manager identifier
     * @param maxCapacity       The maximum capacity
     * @param monitoringExternalSettings 
     */
    public GroupManagerMemoryRepository(String groupManagerId, int maxCapacity, MonitoringExternalSettings monitoringExternalSettings) 
    {
        Guard.check(groupManagerId);
        log_.debug("Initializing the group manager memory repository");
        
        groupManagerId_ = groupManagerId;
        maxCapacity_ = maxCapacity;
        localControllerDescriptions_ = new HashMap<String, LocalControllerDescription>();
        legacyIpAddresses_ = new ArrayList<String>();
        externalSender_ = DataSenderFactory.newExternalDataSender("event", monitoringExternalSettings);
    }
    
    /**
     * Returns the algorithm input data.
     * 
     * @param numberOfMonitoringEntries     The number of monitoring entries
     * @param isActiveOnly                  true if active only controllers are required
     * @param withVirtualMachines           true if virtual machines are needed
     * @return                              The local controller descriptions
     */
    @Override
    public synchronized ArrayList<LocalControllerDescription> 
        getLocalControllerDescriptions(int numberOfMonitoringEntries, boolean isActiveOnly, boolean withVirtualMachines)
    {
        Guard.check(numberOfMonitoringEntries);
        log_.debug(String.format("Getting all local controllers, number of monitoring entries: %d",     
                                  numberOfMonitoringEntries));
        
        ArrayList<LocalControllerDescription> localControllers = new ArrayList<LocalControllerDescription>();       
        for (LocalControllerDescription localController : localControllerDescriptions_.values())
        {   
            LocalControllerStatus status = localController.getStatus();
            if (status.equals(LocalControllerStatus.WOKENUP))
            {
                log_.debug(String.format("Skipping WOKENUP local controller", status));
                continue;
            }
            
            if (isActiveOnly && status.equals(LocalControllerStatus.PASSIVE))
            {
                log_.debug(String.format("Skipping PASSIVE local controller", status));
                continue;
            }
            
            log_.debug(String.format("Gettung local controller description for %s", localController.getId()));
            LocalControllerDescription copy = new LocalControllerDescription(localController, 
                                                                             numberOfMonitoringEntries,
                                                                             withVirtualMachines
                                                                              );
            localControllers.add(copy);
        }
        
        return localControllers;
    }
    
    /**
     * Cleans the repository.
     */
    @Override
    public synchronized void clean()
    {
        log_.debug("Cleaning repository");
        localControllerDescriptions_.clear();
        legacyIpAddresses_.clear();
    }
        
    /**
     * Returns the virtual machine meta data of a local controller.
     * 
     * @param localControllerId     The local controller identifier
     * @return                      The virtual machine meta data list
     */
    private Map<String, VirtualMachineMetaData> getLocalControllerVirtualMachineMetaData(String localControllerId)
    {
        log_.debug(String.format("Returning local controller virtual machine meta data for: %s", localControllerId));
        
        LocalControllerDescription description = localControllerDescriptions_.get(localControllerId);
        if (description == null)
        {
            log_.debug("The local controller description is NULL");
            return null;
        }
        
        return description.getVirtualMachineMetaData();
    }
    
    /**
     * Fills the group manager description.
     * 
     * @param groupManager      The group manager description
     */
    @SuppressWarnings("unchecked")
    @Override
    public synchronized void fillGroupManagerDescription(GroupManagerDescription groupManager) 
    {   
        Guard.check(groupManager);
        log_.debug("Adding possible virtual machine meta data to group manager description");
        //it should be a copy ?  
        groupManager.setLocalControllers((HashMap<String, LocalControllerDescription>) localControllerDescriptions_.clone());   
    }
    
    /**
     * Add local controller description.
     * 
     * @param localController    The local controller description
     * @return                              true if everything ok, false otherwise
     */
    @Override
    public synchronized boolean addLocalControllerDescription(LocalControllerDescription localController) 
    {
        Guard.check(localController);
        String localControllerId = localController.getId();
        log_.debug(String.format("Adding description for local controller: %s", localControllerId));
        
        localControllerDescriptions_.put(localControllerId, localController);
        boolean isUpdated = updateVirtualMachineAssignmens(localController);
        if (!isUpdated)
        {
            log_.debug("Failed to update the virtual machine assignment set!");
            return false;
        }
                     
        log_.debug("Local controller description added successfully!");
        EventUtils.send(externalSender_, new EventMessage(EventType.LC_JOIN, localController));
        return true;
    }
    
    /**
     * Updates the virtual machine assignment set.
     *
     * @param localController   The local controller description
     * @return                  true if everything ok, false otherwise
     */
    private boolean updateVirtualMachineAssignmens(LocalControllerDescription localController) 
    {
        log_.debug(String.format("Starting to update the virtual machine assignment set for local controller: %s",
                                 localController.getId()));
        
        Map<String, VirtualMachineMetaData> metaData = localController.getVirtualMachineMetaData();
        if (metaData == null)
        {
            log_.debug("No meta data available on this local controller!");
            return false;
        }
        
        for (VirtualMachineMetaData entry : metaData.values())
        {
            entry.getVirtualMachineLocation().setLocalControllerId(localController.getId());
            boolean isAdded = addVirtualMachine(entry);
            if (!isAdded)
            {
                log_.debug("Failed to add virtual machine meta data!");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Returns the local controller description associated with a virtual machine.
     * 
     * @param location      The virtual machine location
     * @return              The local controller control data address
     */
    @Override
    public synchronized NetworkAddress getLocalControllerControlDataAddress(VirtualMachineLocation location)
    {
        Guard.check(location);
        
        String virtualMachineId = location.getVirtualMachineId();
        String localControllerId = location.getLocalControllerId();
        log_.debug(String.format("Getting local controller description for virtual machine: %s", virtualMachineId));
        
      
        LocalControllerDescription localController = localControllerDescriptions_.get(localControllerId);
        if (localController == null)
        {
            log_.debug("The local controller description is NULL");
            return null;
        }
        
        VirtualMachineMetaData metaData = localController.getVirtualMachineMetaData().get(virtualMachineId);
        if (metaData == null)
        {
            log_.debug(String.format("No virtual machine %s meta data exists on this local controller!",
                                     virtualMachineId));
            return null;
        }
        
        return localController.getControlDataAddress();
    }
        
    /**
     * Returns the local controller description.
     * 
     * @param localControllerId         The local controller identifier
     * @param numberOfMonitoringEntries The number of monitoring entries
     * @param withVirtualMachines       true if virtual machines are needed
     * @return                          The local controller description
     */
    @Override
    public synchronized LocalControllerDescription getLocalControllerDescription(String localControllerId,
                                                                                 int numberOfMonitoringEntries,
                                                                                 boolean withVirtualMachines
            )
    {
        Guard.check(localControllerId, numberOfMonitoringEntries);       
        log_.debug(String.format("Getting local controller description for %s", localControllerId));
        
        LocalControllerDescription localController = localControllerDescriptions_.get(localControllerId);
        if (localController == null)
        {
            log_.debug("No such local controller available!");
            return null;
        }
        
        LocalControllerDescription localControllerCopy = new LocalControllerDescription(localController,
                                                                                        numberOfMonitoringEntries,
                                                                                        withVirtualMachines
                                                                                        );
        return localControllerCopy;
    }
    
    /**
     * Removes virtual machine meta data mapping.
     * 
     * @param location      The virtual machine location
     * @return              true if everything ok, false otherwise
     */
    private boolean removeVirtualMachineMetaDataMapping(VirtualMachineLocation location)
    {
        log_.debug(String.format("Removing virtual machine meta data mapping for: %s", location.getVirtualMachineId()));
        
        Map<String, VirtualMachineMetaData> metaData = 
            getLocalControllerVirtualMachineMetaData(location.getLocalControllerId());
        if (metaData == null)
        {
            log_.debug("Failed to get virtual machine meta data");
            return false;
        }
        
        metaData.remove(location.getVirtualMachineId());
        return true;
    }
    
    /**
     * Drops virtual machine data.
     * 
     * @param location      The virtual machine location
     * @return              true if everything ok, false otherwise
     */
    @Override
    public synchronized boolean dropVirtualMachineData(VirtualMachineLocation location)
    {
        Guard.check(location);
        log_.debug(String.format("Removing virtual machine data for: %s", location.getVirtualMachineId()));
        
        boolean isReleased = releaseVirtualMachineNetworkInformation(location);
        if (!isReleased)
        {
            log_.debug("Failed to release virtual machine networking information!");
            return false;
        }
        
        boolean isRemoved = removeVirtualMachineMetaDataMapping(location);
        if (!isRemoved)
        {
            return false;
        }
        EventUtils.send(externalSender_, new EventMessage(EventType.VM_DESTROYED, location));
        return true;
    }
    
    /**
     * Adds virtual machine meta data.
     * 
     * @param virtualMachine      The virtual machine meta data
     * @return                    true if everything ok, false otherwise
     */
    @Override
    public synchronized boolean addVirtualMachine(VirtualMachineMetaData virtualMachine)
    {
        Guard.check(virtualMachine);
        String virtualMachineId = virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
        String localControllerId = virtualMachine.getVirtualMachineLocation().getLocalControllerId();
        log_.debug(String.format("Adding virtual machine %s to local controller %s", 
                                 virtualMachineId, localControllerId));
                       
        Map<String, VirtualMachineMetaData> metaData = getLocalControllerVirtualMachineMetaData(localControllerId);  
        if (metaData == null)
        {
            log_.debug("Virtual machine meta data is NULL!");
            return false;
        }
        
        virtualMachine.setUsedCapacity(new LRUCache<Long, VirtualMachineMonitoringData>(maxCapacity_));
        metaData.put(virtualMachineId, virtualMachine); 
        
        EventUtils.send(externalSender_, new EventMessage(EventType.VM_STARTED, virtualMachine));
        return true;
    }
    
    /**
     * Add monitoring data for a virtual machine.
     * 
     * @param localControllerId   The local controller identifier
     * @param aggregatedData      The aggregated virtual machine data
     */
    @Override
    public synchronized void addAggregatedMonitoringData(String localControllerId, 
                                                         List<AggregatedVirtualMachineData> aggregatedData)
    {
        Guard.check(aggregatedData);   
        log_.debug(String.format("Adding aggregated virtual machine monitoring data to the database for %d VMs", 
                                 aggregatedData.size()));
        
        for (AggregatedVirtualMachineData aggregatedVirtualMachineData : aggregatedData) 
        {
            String virtualMachineId = aggregatedVirtualMachineData.getVirtualMachineId();                        
            List<VirtualMachineMonitoringData> dataList = aggregatedVirtualMachineData.getMonitoringData();
            if (dataList.isEmpty())
            {
                log_.debug("The virtual machine monitoring data list is empty");
                continue;
            }
           
            VirtualMachineLocation location = new VirtualMachineLocation();
            location.setLocalControllerId(localControllerId);
            location.setVirtualMachineId(virtualMachineId);
            VirtualMachineMetaData metaData = getVirtualMachineMetaData(location);
            if (metaData == null)
            {
                log_.debug("No meta data exist for this virtual machine!");
                continue;
            }
            
            Map<Long, VirtualMachineMonitoringData> monitoringData = metaData.getUsedCapacity();             
            for (VirtualMachineMonitoringData virtualMachineData : dataList) 
            {
                log_.debug(String.format("Adding history data %s for virtual machine: %s",
                                         virtualMachineData.getUsedCapacity(),   
                                         virtualMachineId));
                monitoringData.put(virtualMachineData.getTimeStamp(), virtualMachineData);
            }
        }
    }
        
    /**
     * Returns a list of legacy IP addresses.
     * 
     * @return  The list of legacy IP addresses
     */
    @Override
    public synchronized ArrayList<String> getLegacyIpAddresses()
    {
        log_.debug(String.format("Returning the current list of legacy IP addresses: %s", 
                                 legacyIpAddresses_.toString()));
        ArrayList<String> newList = new ArrayList<String>(legacyIpAddresses_);
        legacyIpAddresses_.clear();
        return newList;
    }
 
    /**
     * Adds a legacy ip address to the database.
     * 
     * @param ipAddress     The legacy address to add
     * @return              true if everything ok, false otherwise
     */
    private boolean addLegacyIpAddress(String ipAddress)
    {
        Guard.check(ipAddress);
        
        if (legacyIpAddresses_.contains(ipAddress))
        {
            log_.debug(String.format("IP address %s already exists!", ipAddress));
            return false;
        }
        
        log_.debug(String.format("Legacy IP address %s added", ipAddress));
        legacyIpAddresses_.add(ipAddress);
        return true;
    }
                  
    /**
     * Returns the virtual machine meta data.
     * 
     * @param location                      The virtual machine location
     * @param numberOfMonitoringEntries     The number of monitoring entries
     * @return                              The virtual machine meta data
     */
    @Override
    public synchronized VirtualMachineMetaData getVirtualMachineMetaData(VirtualMachineLocation location,
                                                                         int numberOfMonitoringEntries) 
    {
        Guard.check(location); 
        log_.debug(String.format("Generating virtual machine information for: %s", location.getVirtualMachineId()));
        
        VirtualMachineMetaData virtualMachine = getVirtualMachineMetaData(location);
        if (virtualMachine == null)
        {
            log_.debug("No virtual machine meta data available!");
            return null;
        }
        
        VirtualMachineMetaData copy = new VirtualMachineMetaData(virtualMachine, numberOfMonitoringEntries);
        log_.debug(String.format("Returning virtual machine %s meta data, monitoring data size: %d", 
                                 copy.getVirtualMachineLocation().getVirtualMachineId(),
                                 copy.getUsedCapacity().size()));
        return copy; 
    }
    
    /**
     * Returns the virtual machine meta data.
     * 
     * @param location      The virtual machine location
     * @return              The virtual machine meta data
     */
    private synchronized VirtualMachineMetaData getVirtualMachineMetaData(VirtualMachineLocation location) 
    {        
        String virtualMachineId = location.getVirtualMachineId();
        String localControllerId = location.getLocalControllerId();
        log_.debug(String.format("Getting meta data for virtual machine %s on local controller %s", 
                                 virtualMachineId, localControllerId));
        
        LocalControllerDescription localControllerDescription = localControllerDescriptions_.get(localControllerId);
        if (localControllerDescription == null)
        {
            log_.debug("Local controller description is NULL!");
            return null;
        }
        
        Map<String, VirtualMachineMetaData> metaData = localControllerDescription.getVirtualMachineMetaData();
        if (metaData == null)
        {
            log_.debug("No meta data available on this local controller!");
            return null;
        }
        
        VirtualMachineMetaData data = metaData.get(virtualMachineId);
        if (data == null)
        {
            log_.debug("No virtual machine meta data exists!");
            return null;
        }
        
        return data; 
    }
    
    /**
     * Checls if a particular virtual machine is active on a particular local controller.
     * 
     * @param location      The virtual machine location
     * @return              true if exists, false otherwise
     */
    @Override
    public synchronized boolean hasVirtualMachine(VirtualMachineLocation location)
    {
        Guard.check(location);
        log_.debug(String.format("Performing virtual machine lookup for: %s on %s", 
                                 location.getVirtualMachineId(), location.getLocalControllerId()));
                 
        if (getVirtualMachineMetaData(location) != null)
        {
            log_.debug("Such virtual machine exists!");
            return true;
        }
        
        log_.debug("No such virtual machine exists!");
        return false;
    }
    
    /**
     * Checks virtual machine status.
     * 
     * @param location    The virtual machine location
     * @param status      The virtual machine status
     * @return            true if match, false otherwise
     */
    @Override
    public synchronized boolean checkVirtualMachineStatus(VirtualMachineLocation location, 
                                                          VirtualMachineStatus status)
    {
        Guard.check(location, status);
        VirtualMachineMetaData virtualMachineMetaData = getVirtualMachineMetaData(location);
        if (virtualMachineMetaData == null)
        {
            log_.debug("Unable to get virtual machine meta data!");
            return false;
        }

        VirtualMachineStatus state = virtualMachineMetaData.getStatus();
        if (!state.equals(status))
        {
            log_.debug(String.format("This virtual machine is not in the correct state! Current state: %s", state));
            return false;
        }
        
        return true;        
    }
    
    /**
     * Changes the virtual machine state.
     * 
     * @param location      The virtual machine location
     * @param status        The virtual machine status
     * @return              true if everything ok, false otherwise
     */
    @Override
    public synchronized boolean changeVirtualMachineStatus(VirtualMachineLocation location,
                                                           VirtualMachineStatus status)
    {
        Guard.check(location, status);        
        log_.debug(String.format("Changing virtual machine %s status to %s", 
                                 location.getVirtualMachineId(), status));
        
        VirtualMachineMetaData virtualMachineMetaData = getVirtualMachineMetaData(location);
        if (virtualMachineMetaData == null)
        {
            log_.debug("No meta data exists for this virtual machine!");
            return false;
        }
        
        virtualMachineMetaData.setStatus(status);
        return true;
    }
    
    /**
     * Returns the group manager id.
     * 
     * @return  The group manager identifier
     */
    @Override
    public synchronized String getGroupManagerId() 
    {
        return groupManagerId_;
    }

    /**
     * Changes the local controller status.
     * 
     * @param localControllerId     The local controller identifier
     * @param status                The local controller status
     * @return                      true if everything ok, false otherwise
     */
    @Override
    public boolean changeLocalControllerStatus(String localControllerId,  LocalControllerStatus status) 
    {
        Guard.check(localControllerId, status);
        log_.debug(String.format("Changing local controller %s status to %s", localControllerId, status));
        
        LocalControllerDescription localControllerDescription = localControllerDescriptions_.get(localControllerId);
        if (localControllerDescription == null)
        {
            log_.debug("No local controller description exists");
            return false;
        }
        
        log_.debug(String.format("Local controller %s status changed to %s", localControllerId, status));
        localControllerDescription.setStatus(status);
        return true;        
    }
        
    /**
     * Checks local controller status.
     * 
     * @param localControllerId     The local controller identifier
     * @param status                The status
     * @return                      true if matches, false otherwise
     */
    private boolean checkLocalControllerStatus(String localControllerId, LocalControllerStatus status)
    {
        LocalControllerDescription localControllerDescription = localControllerDescriptions_.get(localControllerId);
        if (localControllerDescription == null)
        {
            log_.debug("No local controller description available for this identifier!");
            return false;
        }
        
        if (!localControllerDescription.getStatus().equals(status))
        {
            log_.debug(String.format("This local controller status does not match: %s", status));
            return false;
        }        
        
        return true;
    }
    
    /**
     * Removes the local controller data.
     * (description and history data)
     * 
     * @param localControllerId     The local controller identifier
     * @param forceDelete           Forces status independent deletion
     * @return                      true if everything ok, false otherwise
     */
    @Override
    public synchronized boolean dropLocalController(String localControllerId, boolean forceDelete)
    {
        Guard.check(localControllerId);
        log_.debug(String.format("Removing local controller: %s, force: %s", localControllerId, forceDelete));
        
        LocalControllerDescription localController = localControllerDescriptions_.get(localControllerId);
        if (localController == null)
        {
            log_.debug("No such local controller available!");
            return false;
        }
        
        if (checkLocalControllerStatus(localControllerId, LocalControllerStatus.PASSIVE) && !forceDelete)
        {
            log_.debug("This local controller is in PASSIVE mode! Will not delete!");
            return false;
        }
        
        boolean isReleased = releaseLocalControllerNetworkingInformation(localControllerId);
        if (isReleased)
        {
            log_.debug("Networking information released successfully!");
            localControllerDescriptions_.remove(localControllerId);        
        }
        
        EventUtils.send(externalSender_, new EventMessage(EventType.LC_FAILED, localController));
        return true;
    }
        
    /**
     * Releases the local controller networking information.
     * 
     * @param localControllerId     The local controller identifier
     * @return                      true if everything ok, false otherwise
     */
    private synchronized boolean releaseLocalControllerNetworkingInformation(String localControllerId)
    {
        log_.debug("Releasing the local controller networking information");
                
        Map<String, VirtualMachineMetaData> metaData = getLocalControllerVirtualMachineMetaData(localControllerId);
        if (metaData == null)
        {
            log_.debug("No virtual machine meta data available on this local controller!");
            return false;
        }
        
        log_.debug(String.format("The size of the virtual machine data map is %s", metaData.size()));       
        for (VirtualMachineMetaData virtualMachine : metaData.values()) 
        {            
            String ipAddress = virtualMachine.getIpAddress();
            if (ipAddress == null)
            {
                log_.debug(String.format("The IP address of virtual machine %s is NULL",
                                         virtualMachine.getVirtualMachineLocation().getVirtualMachineId()));
                continue;
            }
            
            addLegacyIpAddress(ipAddress);
        }

        return true;
    }
    
    /**
     * Releases the networking information of a virtual machine.
     * 
     * @param location      The virtual machine location
     * @return              true if released, false otherwise
     */
    private boolean releaseVirtualMachineNetworkInformation(VirtualMachineLocation location)
    {
        log_.debug(String.format("Release virtual machine %s network information", location.getVirtualMachineId()));
        VirtualMachineMetaData virtualMachineMetaData = getVirtualMachineMetaData(location);
        if (virtualMachineMetaData == null)
        {
            log_.error("No meta information exists for this virtual machine!");
            return false;
        }
        
        String ipAddress = virtualMachineMetaData.getIpAddress();
        if (ipAddress == null)
        {
            log_.error("No IP address is assigned to this virtual machine!");
            return false;
        }
       
        boolean isAdded = addLegacyIpAddress(ipAddress);
        return isAdded;
    }

    /**
     * Returns the local controller identifier for a virtual machine.
     * 
     * @param virtualMachineId      The virtual machine identifier
     * @return                      The local controller identifier
     */
    @Override
    public synchronized String searchVirtualMachine(String virtualMachineId) 
    {
        Guard.check(virtualMachineId);
        log_.debug(String.format("Searching the repository for virtual machine: %s", virtualMachineId));
        
        for (LocalControllerDescription localController : localControllerDescriptions_.values()) 
        {
            if (localController.getVirtualMachineMetaData().containsKey(virtualMachineId))
            {
                return localController.getId();
            }
        }
        
        return null;
    }

    /**
     * Updates virtual machine location.
     * 
     * @param oldLocation     The old virtual machine location
     * @param newLocation     The new virtual machine location
     * @return                true if everything ok, false otherwise
     */
    @Override
    public synchronized boolean updateVirtualMachineLocation(VirtualMachineLocation oldLocation, 
                                                             VirtualMachineLocation newLocation) 
    {
        Guard.check(oldLocation, newLocation);
        log_.debug(String.format("Updating virtual machine location for: %s", 
                                 oldLocation.getVirtualMachineId()));
        
        VirtualMachineMetaData metaData = getVirtualMachineMetaData(oldLocation);
        if (metaData == null)
        {
            log_.error("No meta data exists for this virtual machine!");
            return false;
        }
        
        metaData.setVirtualMachineLocation(newLocation);
        boolean isAdded = addVirtualMachine(metaData);
        if (!isAdded)
        {
            log_.error("Failed to move virtual machine meta data moved to new local controller!");
            return false;
        }
        
        boolean isRemoved = removeVirtualMachineMetaDataMapping(oldLocation);
        if (!isRemoved)
        {
            log_.error("Failed to remove virtual machine meta data mapping");
            return false;
        }
        
        return true;
    }

    /**
     * Returns the number of local controllers.
     * 
     * @return  The number of local controllers
     */
    @Override
    public synchronized int getNumberOfLocalControllers() 
    {
        return localControllerDescriptions_.size();
    }

    /**
     * Returns local controller description.
     * 
     * @param networkAddress    The network address
     * @return                  The local controller description, null otherwise
     */ 
    private LocalControllerDescription getLocalControllerDescription(NetworkAddress networkAddress)
    {
        for (Map.Entry<String, LocalControllerDescription> entry : localControllerDescriptions_.entrySet())
        {
            LocalControllerDescription localController = entry.getValue();
            NetworkAddress tmpAddress = localController.getControlDataAddress();
            boolean isEqualAddress = tmpAddress.getAddress().equals(networkAddress.getAddress());
            boolean isEqualPort = tmpAddress.getPort() == networkAddress.getPort();
            if (isEqualAddress && isEqualPort)
            {
                return localController;
            }
        } 
        
        return null;
    }
    
    /**
     * Checks if local controller exists.
     * @deprecated
     * 
     * @param localControllerAddress     The lcoal controller address
     * @return                           The previous identifier, null otherwise
     */
    @Override
    public synchronized String hasLocalController(NetworkAddress localControllerAddress) 
    {
        log_.debug("Checking for local controller existance");

        LocalControllerDescription localController = getLocalControllerDescription(localControllerAddress);
        if (localController == null)
        {
            log_.debug("No local controller detected!");
            return null;
        }
     
        log_.debug("Local controller detected!");
        return localController.getId();
    }

    @Override
    public boolean updateVirtualMachineMetaData(
            VirtualMachineMetaData virtualMachine) 
    {
        Guard.check(virtualMachine);
        VirtualMachineLocation location = virtualMachine.getVirtualMachineLocation();
        log_.debug(String.format("Updating virtual machine meta data for: %s", 
                                 location.getVirtualMachineId()));
        
        String virtualMachineId = location.getVirtualMachineId();
        String localControllerId = location.getLocalControllerId();
        
        
        LocalControllerDescription localControllerDescription = localControllerDescriptions_.get(localControllerId);
        if (localControllerDescription == null)
        {
            log_.debug("Local controller description is NULL!");
            return false;
        }
        
        Map<String, VirtualMachineMetaData> metaData = localControllerDescription.getVirtualMachineMetaData();
        if (metaData == null)
        {
            log_.debug("No meta data available on this local controller!");
            return false;
        }
        metaData.put(virtualMachineId, virtualMachine);

        return true;   
      
    }
}
