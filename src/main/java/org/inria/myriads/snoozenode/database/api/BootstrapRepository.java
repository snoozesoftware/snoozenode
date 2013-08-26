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

import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerList;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.migration.ClientMigrationRequestSimple;
import org.inria.myriads.snoozecommon.communication.virtualcluster.migration.MigrationRequest;

/**
 * Bootstrap repository interfaces
 * Compare to the other repo it's only get here.
 *   
 * @author Matthieu Simonin
 */
public interface BootstrapRepository 
{
    /**
     * 
     * Gets the virtual machine meta data.
     * 
     * @param virtualMachineId              The virtual machine id.
     * @param numberOfMonitoringEntries     The number of wanted monitoring entries.
     * @return                              The virtualMachine metadata.
     */
    public VirtualMachineMetaData getVirtualMachineMetaData(String virtualMachineId, int numberOfMonitoringEntries);

    /**
     * 
     * Gets all the local controllers (passive included, unassigned excluded)
     * 
     * @return LocalControllerList
     */
    public LocalControllerList getLocalControllerList();
    
    //admin zone
    /**
     * 
     * Gets all the group managers (unassigned included) 
     * 
     * @param firstGroupManagerId
     * @param limit
     * @return GroupManagers
     */
    public List<GroupManagerDescription> getGroupManagerDescriptions(String firstGroupManagerId, int limit, int numberOfMonitoringEntries, String groupManagerId);
    
    /**
     * 
     * Gets all the local controllers (passive included, unassigned included)
     * 
     * @return LocalControllers
     */
    public List<LocalControllerDescription> getLocalControllerDescriptions(String groupManagerId, String firstLocalControllerId, int limit, int numberOfMonitoringEntries);

    
    /**
     * 
     * Gets all the virtual machines (passive included, unassigned included)
     * 
     * @return virtualMachines
     */
    public List<VirtualMachineMetaData> getVirtualMachineDescriptions(String groupManagerId, String localControllerId,
            String startVirtualMachine, int limit, int numberOfMonitoringEntries);

    
    /**
     * 
     * Creates the migration request.
     * 
     * @param migrationRequest
     * @return
     */
    public MigrationRequest createMigrationRequest(ClientMigrationRequestSimple migrationRequest);
    
}
