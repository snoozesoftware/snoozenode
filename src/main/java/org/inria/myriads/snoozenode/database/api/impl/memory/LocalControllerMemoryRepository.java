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
package org.inria.myriads.snoozenode.database.api.impl.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.Resource;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.HostMonitoringData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.VirtualMachineMonitoringData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.database.api.LocalControllerRepository;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.AggregatedHostMonitoringData;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.AggregatedVirtualMachineData;
import org.inria.myriads.snoozenode.util.OutputUtils;
import org.inria.snoozenode.external.notifier.ExternalNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local controller repository in-memory implementation.
 * 
 * @author Eugen Feller
 */
public final class LocalControllerMemoryRepository 
    implements LocalControllerRepository
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LocalControllerMemoryRepository.class);
    
    /** Host Resources to monitor. */
    private Map<String, Resource> hostResources_;
    
    /** 
     * Virtual machine meta data map. 
     *  
     * Key: Virtual machine identifier
     * Value: Virtual machine meta data
     */
    private HashMap<String, VirtualMachineMetaData> virtualMachineMetaData_;
       
    /**
     * 
     * Local controller memory repository constructor.
     * 
     * @param localController   The localController description
     * @param externalNotifier  The external notifier.
     */
    public LocalControllerMemoryRepository(
            LocalControllerDescription localController, 
            ExternalNotifier externalNotifier)
    {
        log_.debug("Initializing the local controller in-memory repository");
        virtualMachineMetaData_ = new HashMap<String, VirtualMachineMetaData>();
        hostResources_ = localController.getHostResources().getResources();
    }
    
    /**
     * Adds virtual machine meta data.
     * 
     * @param virtualMachineMetaData    The virtual machine meta data
     * @return                          true if added, false otherwise
     */
    @Override
    public synchronized boolean addVirtualMachineMetaData(VirtualMachineMetaData virtualMachineMetaData) 
    {
        Guard.check(virtualMachineMetaData);
        String virtualMachineId = virtualMachineMetaData.getVirtualMachineLocation().getVirtualMachineId();
        log_.debug(String.format("Adding virtual machine meta data for: %s", virtualMachineId));
        
        boolean hasKey = virtualMachineMetaData_.containsKey(virtualMachineId);
        if (hasKey)
        {
            log_.debug("This virtual machine meta data is already in the database!");
            return false;
        }
        virtualMachineMetaData.setIsAssigned(true);
        virtualMachineMetaData_.put(virtualMachineId, virtualMachineMetaData);
        log_.debug("Virtual machine meta data added!");
        log_.debug("Added metadata : " + OutputUtils.toString(virtualMachineMetaData));
        
        return true;
    }
    
    /**
     * Changes the virtual machine status.
     * 
     * @param virtualMachineId   The virtual machine identifier
     * @param status             The virtual machine status
     * @return                   true if everything ok, false otherwise
     */
    public synchronized boolean changeVirtualMachineStatus(String virtualMachineId, VirtualMachineStatus status)
    {
        Guard.check(virtualMachineId, status);
        log_.debug(String.format("Changing virtual machine %s status to %s", virtualMachineId, status));
        
        boolean hasMetaData = virtualMachineMetaData_.containsKey(virtualMachineId);
        if (!hasMetaData)
        {
            log_.debug("No meta data exists for this virtual machine");
            return false;
        }
        
        VirtualMachineMetaData metaData = virtualMachineMetaData_.get(virtualMachineId);
        metaData.setStatus(status);
        return true;
    }
    
    /**
     * Drops the virtual machine meta data.
     * 
     * @param virtualMachineId          The virtual machine identifier
     * @return                          true if dropped, false otherwise
     */
    @Override
    public synchronized boolean dropVirtualMachineMetaData(String virtualMachineId)
    {
        Guard.check(virtualMachineId);
        log_.debug(String.format("Removing virtual machine meta data mapping for: %s", virtualMachineId));
        
        boolean hasMetaData = virtualMachineMetaData_.containsKey(virtualMachineId);
        if (!hasMetaData)
        {
            log_.debug("No meta data exists for this virtual machine");
            return false;
        }
        
        virtualMachineMetaData_.remove(virtualMachineId);
        log_.debug("Virtual machine meta data mapping removed!");
       
        return true;
    }
    
    /**
     * Updates the virtual machine meta data with the given group manager information.
     * 
     * @param groupManagerDescription   The group manager description
     * @return                          The updated meta data map
     */
    @Override
    public synchronized HashMap<String, VirtualMachineMetaData> 
        updateVirtualMachineMetaData(GroupManagerDescription groupManagerDescription) 
    {
        Guard.check(groupManagerDescription);
        log_.debug("Updating virtual machine meta data with new group manager information");
        
        for (VirtualMachineMetaData metaData : virtualMachineMetaData_.values())
        {
            NetworkAddress controlAddress = groupManagerDescription.getListenSettings().getControlDataAddress();
            VirtualMachineLocation location = metaData.getVirtualMachineLocation();
            location.setGroupManagerId(groupManagerDescription.getId());
            location.setGroupManagerControlDataAddress(controlAddress);
        }
        
        return virtualMachineMetaData_;
    }

    /**
     * Returns virtual machine meta data.
     * 
     * @param virtualMachineId      The virtual machine identifier
     * @return                      The virtual machine meta data
     */
    @Override
    public synchronized VirtualMachineMetaData getVirtualMachineMetaData(String virtualMachineId) 
    {
        Guard.check(virtualMachineId);
        log_.debug(String.format("Getting virtual machine meta data for: %s", virtualMachineId));
        
        return virtualMachineMetaData_.get(virtualMachineId);
    }

    
    /**
     * Returns the virtual machine meta data map.
     * 
     * @return      The virtual machine meta data
     */
    @Override
    public Map<String, VirtualMachineMetaData> getVirtualMachineMetaData() 
    {
        return virtualMachineMetaData_;
    }

    @Override
    public List<VirtualMachineMetaData> getVirtualMachines(int numberOfMonitoringEntries)
    {
        log_.debug("Getting all virtual machines");
        List<VirtualMachineMetaData> virtualMachines = new ArrayList<VirtualMachineMetaData>(); 
        virtualMachines.addAll(virtualMachineMetaData_.values());
        return virtualMachines;
    }

    @Override
    public synchronized void addAggregatedHostMonitoringData(List<AggregatedHostMonitoringData> aggregatedData)
    {
        for (AggregatedHostMonitoringData monitorData : aggregatedData)
        {
            for (HostMonitoringData  monitoringData : monitorData.getMonitoringData())
            {
                for (Entry<String, Double> m: monitoringData.getUsedCapacity().entrySet())
                {
                    Resource resource = hostResources_.get(m.getKey());
                    if (resource == null)
                    {
                        log_.debug(String.format("The resource %s is not registered yet ... passing", m.getKey()));
                        continue;
                    }
                    log_.debug(String.format("Adding new hostory value for %s", m.getValue()));
                    resource.getHistory().put(monitoringData.getTimeStamp(), m.getValue());
                }
            }
        }
    }

    @Override
    public Map<String, Resource> getHostResources()
    {
        return hostResources_;
    }

    @Override
    public void addAggregatedVirtualMachineData(ArrayList<AggregatedVirtualMachineData> aggregatedDatas)
    {
        Guard.check(aggregatedDatas);
        try
        {
            for (AggregatedVirtualMachineData aggregatedData : aggregatedDatas)
            {
                String virtualMachineId = aggregatedData.getVirtualMachineId();
                VirtualMachineMetaData virtualMachine = virtualMachineMetaData_.get(virtualMachineId);
                if (virtualMachine == null)
                {
                    log_.error(String.format(
                            "The virtual machine %s doesn't exist in the repo ...skipping", virtualMachineId));
                    continue;
                }
                log_.debug("Adding used Capacity to the vm");
                for (VirtualMachineMonitoringData  virtualMachineMonitoring : aggregatedData.getMonitoringData())
                {
                    virtualMachine.getUsedCapacity().put(
                            virtualMachineMonitoring.getTimeStamp(),
                            virtualMachineMonitoring);
                }
            }
        }
        catch (Exception exception)
        {
            log_.error("Unable to update the repository");
            exception.printStackTrace();
        }
    
    }


    @Override
    public Map<String, Resource> getHostMonitoringValues(int numberOfMonitoringEntries)
    {
        log_.debug(String.format("Getting the last host monitoring %d values", numberOfMonitoringEntries));
        Map<String, Resource> hostResourcesCopy = new HashMap<String, Resource>();
        for (Entry<String, Resource> resourceSet  : hostResources_.entrySet())
        {
            Resource resource = resourceSet.getValue();
            // creating new resource.
            Resource resourceCopy = new Resource(resource, numberOfMonitoringEntries);
            hostResourcesCopy.put(resourceSet.getKey(), resourceCopy);
        }
        return hostResourcesCopy;
    }
    
}
