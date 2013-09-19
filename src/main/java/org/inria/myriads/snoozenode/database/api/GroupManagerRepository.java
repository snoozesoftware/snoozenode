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
import java.util.List;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.AggregatedVirtualMachineData;

/**
 * Group manager repisitory interface.
 * 
 * @author Eugen Feller
 */
/**
 * @author msimonin
 *
 */
/**
 * @author msimonin
 *
 */
/**
 * @author msimonin
 *
 */
public interface GroupManagerRepository 
{      
    /** 
     * Returns the group manager identifier. 
     * 
     * @return      The group manager identifier
     */
    String getGroupManagerId();
          
    /**
     * Returns all local controller descriptions.
     * 
     * @param numberOfMonitoringEntries    The number of monitoring entries
     * @param isActiveOnly                 true if active only controllers are needed
     * @param withVirtualMachines          true if virtualMachines are needed
     * @return                             The local controller descriptions
     */
    ArrayList<LocalControllerDescription> getLocalControllerDescriptions(int numberOfMonitoringEntries, 
                                                                         boolean isActiveOnly,
                                                                         boolean withVirtualMachines
                                                                         );
    
    /**
     * Returns the local controller control addressed associated with a virtual machine.
     * 
     * @param location      The virtual machine location
     * @return              The local controller description
     */
    NetworkAddress getLocalControllerControlDataAddress(VirtualMachineLocation location);
    
    /**
     * Returns the local controller description.
     * 
     * @param localControllerId          The local controller identifier
     * @param numberOfMonitoringEntries  The number of monitoring entries
     * @param withVirtualMachines        True if virtual machines are needed
     * @return                           The local controller description
     */
    LocalControllerDescription getLocalControllerDescription(String localControllerId, 
                                                             int numberOfMonitoringEntries, 
                                                             boolean withVirtualMachines);
       
    /** 
     * Adds local controller description. 
     * 
     * @param description       The local controller description
     * @return                  true if everything ok, false otherwise
     */
    boolean addLocalControllerDescription(LocalControllerDescription description);
            
    /** 
     * Drops the active local controller. 
     * 
     * @param localControllerId     The local controller identifier
     * @param forceDelete           Forces status independent deletion
     * @return                      true if everything ok, false otherwise
     */
    boolean dropLocalController(String localControllerId, boolean forceDelete);
    
    /**
     * Fills the group manager description.
     * 
     * @param groupManagerDescription      The group manager description
     */
    void fillGroupManagerDescription(GroupManagerDescription groupManagerDescription);
            
    /** 
     * Adds aggregated virtual machine monitoring data. 
     * 
     * @param localControllerId   The local controller identifier
     * @param aggregatedData      The aggregated virtual machine monitoring data
     */
    void addAggregatedMonitoringData(String localControllerId, 
                                     List<AggregatedVirtualMachineData> aggregatedData);
   
    /**
     * Returns a list of legacy IP addresses.
     * 
     * @return  The list of legacy IP addresses
     */
    ArrayList<String> getLegacyIpAddresses();
    
    /** 
     * Drops all virtual machine data. 
     * 
     * @param location      The virtual machine location
     * @return              true if everything ok, false otherwise
     */
    boolean dropVirtualMachineData(VirtualMachineLocation location);
    
    /** 
     * Returns virtual machine information.
     * 
     * @param location                      The virtual machine location
     * @param numberOfMonitoringEntries     The number of monitoring entries
     * @return                              The virtual machine meta data
     */
    VirtualMachineMetaData getVirtualMachineMetaData(VirtualMachineLocation location, 
                                                     int numberOfMonitoringEntries);
    
    /** 
     * Changes virtual machine status. 
     * 
     * @param location                  The virtual machine location
     * @param status                    The virtual machine status
     * @return                          true if everything ok, false otherwise
     */
    boolean changeVirtualMachineStatus(VirtualMachineLocation location, VirtualMachineStatus status);

    /**
     * Checks virtual machine status.
     * 
     * @param location      The virtual machine location
     * @param status        The virtual machine status
     * @return              true if match, false otherwise
     */
    boolean checkVirtualMachineStatus(VirtualMachineLocation location, VirtualMachineStatus status);
                
    /**
     * Checks if a virtual machine is active on a particular local controller.
     * 
     * @param location      The virtual machine location
     * @return              true if active, false otherwise
     */
    boolean hasVirtualMachine(VirtualMachineLocation location);
    
    /**
     * Adds virtual machine.
     * 
     * @param virtualMachineMetaData        The virtual machine meta data
     * @return                              true if added, false otherwise
     */
    boolean addVirtualMachine(VirtualMachineMetaData virtualMachineMetaData);
    
    /**
     * Cleans the repository.
     */
    void clean();

    /**
     * Returns the local controller identifier for a virtual machine.
     * 
     * @param virtualMachineId      The virtual machine identifier
     * @return                      The local controller identifier
     */
    String searchVirtualMachine(String virtualMachineId);

    /**
     * Updates virtual machine location.
     * 
     * @param oldVirtualMachineLocation     The old virtual machine location
     * @param newVirtualMachineLocation     The new virtual machine location
     * @return                              true if everything ok, false otherwise
     */
    boolean updateVirtualMachineLocation(VirtualMachineLocation oldVirtualMachineLocation, 
                                         VirtualMachineLocation newVirtualMachineLocation);

    /**
     * Changes the local controller status.
     * 
     * @param localControllerId     The local controller identifier
     * @param status                The local controller status
     * @return                      true if everything ok, false otherwise
     */
    boolean changeLocalControllerStatus(String localControllerId,  LocalControllerStatus status);

    /**
     * Checks if local controller exists.
     * 
     * @param localControllerAddress     The lcoal controller address
     * @return                           The previous identifier, null otherwise
     */
    String hasLocalController(NetworkAddress localControllerAddress);

    /**
     * 
     * Updates the virtual machine meta data.
     * 
     * @param virtualMachine        New virtual Machine Meta data
     * @return                      true if everything ok
     */
    boolean updateVirtualMachineMetaData(VirtualMachineMetaData virtualMachine);

    
    /**
     * 
     * Return the group manager description.
     * 
     * @return      GroupManager description
     */
    GroupManagerDescription  getGroupManager();

    /**
     * 
     * Returns the list of local controllers to transmit to the group leader.
     * 
     * @return the list of local controllers to transmit.
     */
    ArrayList<LocalControllerDescription> getLocalControllerDescriptionForDataTransporter();

}
