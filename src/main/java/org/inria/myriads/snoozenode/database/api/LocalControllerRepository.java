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
package org.inria.myriads.snoozenode.database.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.Resource;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineStatus;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.AggregatedHostMonitoringData;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.AggregatedVirtualMachineData;

/**
 * Local controller repository interface.
 * 
 * @author Eugen Feller
 */
public interface LocalControllerRepository 
{
    /**
     * Returns the virtual machine meta data list.
     * 
     * @return      The virtual machine meta data
     */
    Map<String, VirtualMachineMetaData> getVirtualMachineMetaData();
    
    /**
     * Changes the virtual machine status.
     * 
     * @param virtualMachineId   The virtual machine identifier
     * @param status             The virtual machine status
     * @return                   true if everything ok, false otherwise
     */
    boolean changeVirtualMachineStatus(String virtualMachineId, VirtualMachineStatus status);
    
    /**
     * Adds virtual machine meta data.
     * 
     * @param virtualMachineMetaData    The virtual machine meta data
     * @return                          true if added, false otherwise
     */
    boolean addVirtualMachineMetaData(VirtualMachineMetaData virtualMachineMetaData);
    
    /**
     * Drops virual machine meta data.
     * 
     * @param virtualMachineId          The virtual machine meta data
     * @return                          true if removed, false otherwise
     */
    boolean dropVirtualMachineMetaData(String virtualMachineId);
    
    /**
     * Updates the virtual machine meta data with the given group manager information.
     * 
     * @param groupManagerDescription   The group manager description
     * @return                          The updated meta data map
     */
   
    
    HashMap<String, VirtualMachineMetaData> 
        updateVirtualMachineMetaData(GroupManagerDescription groupManagerDescription);

    /**
     * Get virtual machine meta data.
     * 
     * @param virtualMachineId      The virtual machine identifier
     * @return                      The virtual machine meta data
     */
    VirtualMachineMetaData getVirtualMachineMetaData(String virtualMachineId);
    
    /**
     * Get virtual machines meta data.
     * 
     * @param numberOfMonitoringEntries The virtual machine identifier
     * @return                          The virtual machines meta data
     */
    List<VirtualMachineMetaData> getVirtualMachines(int numberOfMonitoringEntries);

    /**
     * 
     * Add aggregated metric data in the repository.
     * 
     * @param aggregatedData
     */
    void addAggregatedHostMonitoringData(List<AggregatedHostMonitoringData> aggregatedData);

    
    /**
     * Get the host monitoring datas.
     */
    HashMap<String, Resource> getHostResources();

    /**
     * 
     * Adds the vm monitoring data.
     * 
     * @param clonedData
     */
    void addAggregatedVirtualMachineData(ArrayList<AggregatedVirtualMachineData> clonedData);

    /**
     * 
     * Get the monitoring since a given timestamp.
     * 
     * @param pastTimestamp
     * @return
     */
    Map<String, Resource> getLastHostMonitoringValues(long pastTimestamp);

    /**
     * 
     * Gets the virtual machine metadata with the latest monitoring datas.
     * 
     * @param pastTimestamp
     * @return
     */
    List<VirtualMachineMetaData> getLastVirtualMachineMetaData(long pastTimestamp);

    
    /**
     * 
     * Gets the host monitoring values.
     * 
     * @param numberOfMonitoringEntries
     * @return
     */
    Map<String, Resource> getHostMonitoringValues(int numberOfMonitoringEntries);


}
