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
import java.util.List;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.groupmanager.repository.GroupLeaderRepositoryInformation;
import org.inria.myriads.snoozecommon.communication.groupmanager.repository.GroupManagerRepositoryInformation;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerList;
import org.inria.myriads.snoozecommon.communication.rest.CommunicatorFactory;
import org.inria.myriads.snoozecommon.communication.rest.api.GroupManagerAPI;
import org.inria.myriads.snoozecommon.communication.rest.api.LocalControllerAPI;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.discovery.VirtualMachineDiscoveryResponse;
import org.inria.myriads.snoozecommon.communication.virtualcluster.migration.ClientMigrationRequestSimple;
import org.inria.myriads.snoozecommon.communication.virtualcluster.migration.MigrationRequest;
import org.inria.myriads.snoozecommon.communication.virtualcluster.requests.MetaDataRequest;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozenode.database.api.BootstrapRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bootstrap "in-memory" repository.
 * 
 * @author msimonin 
 */
public final class BootstrapMemoryRepository 
    implements BootstrapRepository 
{
    /** Logger. */
    private static final Logger log_ = LoggerFactory.getLogger(BootstrapMemoryRepository.class);

    @Override
    public VirtualMachineMetaData getVirtualMachineMetaData(
            String virtualMachineId, 
            int numberOfMonitoringEntries,
            GroupManagerDescription groupLeader)
    {
        NetworkAddress groupLeaderAddress = groupLeader.getListenSettings().getControlDataAddress();
        GroupManagerAPI groupLeaderCommunicator = 
                CommunicatorFactory.newGroupManagerCommunicator(groupLeaderAddress); 
        VirtualMachineDiscoveryResponse discovery = groupLeaderCommunicator.discoverVirtualMachine(virtualMachineId);
        if (discovery == null)
        {
            return null;
        }
        NetworkAddress groupManagerAddress = discovery.getGroupManagerAddress();
        GroupManagerAPI groupManagerCommunicator = 
                CommunicatorFactory.newGroupManagerCommunicator(groupManagerAddress);
        MetaDataRequest metaDataRequest = new MetaDataRequest();
        VirtualMachineLocation location = new VirtualMachineLocation();
        location.setLocalControllerId(discovery.getLocalControllerId());
        location.setVirtualMachineId(virtualMachineId);
        location.setGroupManagerControlDataAddress(discovery.getGroupManagerAddress());
        metaDataRequest.setVirtualMachineLocation(location);
        VirtualMachineMetaData virtualMachine = groupManagerCommunicator.getVirtualMachineMetaData(metaDataRequest);
        return virtualMachine;
    }

   
    /* (non-Javadoc)
     * @see org.inria.myriads.snoozenode.database.api.BootstrapRepository#getLocalControllerList()
     */
    @Override
    public LocalControllerList getLocalControllerList()
    {
        return null;
    }

    /**
     * Gets the groupamanger descriptions.
     * @param firstGroupManagerId           (ignored)
     * @param limit                         (ignored)
     * @param numberOfMonitoringEntries      number of monitoring entries.
     * @param groupLeader                   group leader.
     * @return list of group manager description. 
     */
    @Override
    public List<GroupManagerDescription> getGroupManagerDescriptions(
            String firstGroupManagerId, 
            int limit, 
            int numberOfMonitoringEntries, 
            GroupManagerDescription groupLeader)
    {
        NetworkAddress groupLeaderAddress = groupLeader.getListenSettings().getControlDataAddress();
        GroupManagerAPI groupLeaderCommunicator = 
                CommunicatorFactory.newGroupManagerCommunicator(groupLeaderAddress); 
        GroupLeaderRepositoryInformation information = 
            groupLeaderCommunicator.getGroupLeaderRepositoryInformation(numberOfMonitoringEntries);
        
        return information.getGroupManagerDescriptions();
    }
    

    @Override
    public GroupManagerDescription getGroupManagerDescription(
            String groupManagerId, 
            GroupManagerDescription groupLeader)
    {
        NetworkAddress groupLeaderAddress = groupLeader.getListenSettings().getControlDataAddress();
        GroupManagerAPI groupLeaderCommunicator = 
                CommunicatorFactory.newGroupManagerCommunicator(groupLeaderAddress); 
        GroupManagerDescription groupManager =
            groupLeaderCommunicator.getGroupManagerDescription(groupManagerId);
        
        return groupManager;
    }
    
    @Override
    public List<LocalControllerDescription> getLocalControllerDescriptions(
            String groupManagerId,
            String firstLocalControllerId, 
            int limit, 
            int numberOfMonitoringEntries,
            GroupManagerDescription groupLeader
            )
    {
        
        List<LocalControllerDescription> localControllers = new ArrayList<LocalControllerDescription>();
        try
        {
            NetworkAddress groupLeaderAddress = groupLeader.getListenSettings().getControlDataAddress();
            GroupManagerAPI groupLeaderCommunicator = 
                CommunicatorFactory.newGroupManagerCommunicator(groupLeaderAddress);
            
            if (groupManagerId == null || groupManagerId.equals(""))
            {
                log_.debug("Gets all the local controllers");
                
                localControllers = groupLeaderCommunicator.getLocalControllerList().getLocalControllers();
            }
            else
            {
                log_.debug("Gets the localcontrollers of groupmanager " + groupManagerId);
                GroupManagerDescription groupManager = 
                        groupLeaderCommunicator.getGroupManagerDescription(groupManagerId);
                
                if (groupManager == null)
                {
                    log_.debug("The group manager doesn't exist");
                    return localControllers;
                }
                NetworkAddress groupManagerAddress = groupManager.getListenSettings().getControlDataAddress();
                GroupManagerAPI groupManagerCommunicator = 
                    CommunicatorFactory.newGroupManagerCommunicator(groupManagerAddress);
                localControllers = groupManagerCommunicator
                                        .getGroupManagerRepositoryInformation(numberOfMonitoringEntries)
                                        .getLocalControllerDescriptions();
                
            }
        }
        catch (Exception exception)
        {
            log_.error("Unable to get the local controller list");
        }
        return localControllers;
    }

    
    @Override
    public List<VirtualMachineMetaData> getVirtualMachineDescriptions(
            String groupManagerId, 
            String localControllerId,
            String startVirtualMachine, 
            int limit, 
            int numberOfMonitoringEntries,
            GroupManagerDescription groupLeader
            )
    {
        //return the list of int GMs.
        List<GroupManagerDescription> groupManagers = 
                getGroupManagers(groupLeader, groupManagerId);
        log_.debug("grouManagers = " + groupManagers);
        
        // return the list of int LCs.
        List<LocalControllerDescription> localControllers = 
                getLocalControllers(groupLeader, groupManagers, localControllerId, numberOfMonitoringEntries);

        log_.debug("localControllers = " + localControllers);
        List<VirtualMachineMetaData> virtualMachines = getVirtualMachines(localControllers);
        
        log_.debug("virtualMachines = " + virtualMachines);
        
        return virtualMachines;
    }

    /**
     * 
     * Gets the virtual machines.
     * 
     * @param localControllers  The localControllers
     * @return  The virtual machines list
     */
    protected List<VirtualMachineMetaData> getVirtualMachines(List<LocalControllerDescription> localControllers)
    {
        List<VirtualMachineMetaData> virtualMachines = new ArrayList<VirtualMachineMetaData>();
        for (LocalControllerDescription localController : localControllers)
        {
            virtualMachines.addAll(localController.getVirtualMachineMetaData().values());
        }
        return virtualMachines;
    }


    /**
     * 
     * Gets the localControllers List.
     * 
     * @param groupLeader                   The groupLeader Address
     * @param groupManagers                 The groupManagers Address.
     * @param localControllerId             The localController Id.
     * @param numberOfMonitoringEntries     The number of monitoring entries
     * @return  The localControllers list.
     */
    protected List<LocalControllerDescription> getLocalControllers(
            GroupManagerDescription groupLeader,
            List<GroupManagerDescription> groupManagers, 
            String localControllerId,
            int numberOfMonitoringEntries)
    {
        
        List<LocalControllerDescription> localControllersList = new ArrayList<LocalControllerDescription>();
        
        for (GroupManagerDescription groupManager : groupManagers)
        {
            NetworkAddress groupManagerAddress = groupManager.getListenSettings().getControlDataAddress();  
            GroupManagerAPI groupManagerCommunicator = 
                    CommunicatorFactory.newGroupManagerCommunicator(groupManagerAddress);
            GroupManagerRepositoryInformation groupManagerInformation = groupManagerCommunicator.getGroupManagerRepositoryInformation(numberOfMonitoringEntries);
            ArrayList<LocalControllerDescription> localControllers = 
                    groupManagerInformation.getLocalControllerDescriptions();
            if (!isNullOrEmpty(localControllerId))
            {
                log_.debug("lookup localcontroller");
                for (LocalControllerDescription localController : localControllers)
                {
                    if (localController.getId().equals(localControllerId))
                    {
                        log_.debug("found the localcontrollerid");
                        localControllersList.add(localController);
                        break;
                    }
                }
            }
            else
            {
                log_.debug("add all localcontrollers");
                localControllersList.addAll(localControllers);
            }
        }
        
        return localControllersList;
    }


    /**
     * 
     * Gets the groupmanager list.
     * 
     * @param groupLeader           The group leader.
     * @param groupManagerId        The groupmanager id.
     * @return the group managers list.
     */
    protected List<GroupManagerDescription> getGroupManagers(GroupManagerDescription groupLeader, String groupManagerId)
    {
        NetworkAddress groupLeaderAddress = groupLeader.getListenSettings().getControlDataAddress();
        GroupManagerAPI groupLeaderCommunicator = 
            CommunicatorFactory.newGroupManagerCommunicator(groupLeaderAddress);

        List<GroupManagerDescription> groupManagers = new ArrayList<GroupManagerDescription>();
        
        GroupLeaderRepositoryInformation groupLeaderInformation =
                groupLeaderCommunicator.getGroupLeaderRepositoryInformation(0);
        
        if (!isNullOrEmpty(groupManagerId))
        {
            log_.debug("Lookup the groupmanager");
            for (GroupManagerDescription groupManager : groupLeaderInformation.getGroupManagerDescriptions())
            {
                if (groupManager.getId().equals(groupManagerId))
                {
                    log_.debug("Add the specified group manager");
                    groupManagers.add(groupManager);
                    break;
                }
            }
        }
        else
        {
            log_.debug("Add all group managers");
            groupManagers.addAll(groupLeaderInformation.getGroupManagerDescriptions());
        }
        
        return groupManagers;
    }



    @Override
    public MigrationRequest createMigrationRequest(ClientMigrationRequestSimple migrationRequest)
    {
        return null;
    }
    
    /**
     * 
     * Test if a string is null or empty.
     * 
     * @param s     string to test.
     * @return  true or false.
     */
    private boolean isNullOrEmpty(String s)
    {
        return s == null || s.equals("");
    }



}
