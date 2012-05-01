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
package org.inria.myriads.snoozenode.groupmanager.virtualmachinediscovery;

import java.util.List;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.rest.CommunicatorFactory;
import org.inria.myriads.snoozecommon.communication.rest.api.GroupManagerAPI;
import org.inria.myriads.snoozecommon.communication.virtualcluster.discovery.VirtualMachineDiscoveryResponse;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.database.api.GroupLeaderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Virtual machine discovery logic.
 * 
 * @author Eugen Feller
 */
public final class VirtualMachineDiscovery
{
    /** Logging instance. */
    private static final Logger log_ = LoggerFactory.getLogger(VirtualMachineDiscovery.class);
    
    /** Number of monitoring entries. */
    private static final int NUMBER_OF_MONITORING_ENTRIES = 0;
    
    /** The group leader repository. */
    private GroupLeaderRepository groupLeaderRepository_;
    
    /**
     * Virtual machine discovery constructor.
     * 
     * @param groupLeaderRepository     The  group leader repository
     */
    public VirtualMachineDiscovery(GroupLeaderRepository groupLeaderRepository) 
    {
        Guard.check(groupLeaderRepository);
        log_.debug("Initializing the virtual machine discovery service");
        groupLeaderRepository_ = groupLeaderRepository;
    }

    /**
     * Starts the virtual machine discovery.
     * 
     * @param virtualMachineId      The virtual machine identifier
     * @return                      The discovery response
     */
    public VirtualMachineDiscoveryResponse startVirtualMachineDiscovery(String virtualMachineId)
    {
        Guard.check(virtualMachineId);
        log_.debug(String.format("Starting virtual machine discovery for: %s", virtualMachineId));
        
        List<GroupManagerDescription> groupManagerDescriptions = 
            groupLeaderRepository_.getGroupManagerDescriptions(NUMBER_OF_MONITORING_ENTRIES);
        for (GroupManagerDescription groupManager : groupManagerDescriptions) 
        {
            NetworkAddress address = groupManager.getListenSettings().getControlDataAddress();
            GroupManagerAPI groupManagerCommunicator = CommunicatorFactory.newGroupManagerCommunicator(address);
            String localControllerId = groupManagerCommunicator.searchVirtualMachine(virtualMachineId);  
            if (localControllerId != null)
            {
                log_.debug(String.format("Group manager %s has the virtual machine! Great!", 
                                         groupManager.getId()));
                VirtualMachineDiscoveryResponse response = new VirtualMachineDiscoveryResponse();
                response.setLocalControllerId(localControllerId);
                response.setGroupManagerAddress(address);
                return response;
            }
        }
        
        return null;
    }
}
