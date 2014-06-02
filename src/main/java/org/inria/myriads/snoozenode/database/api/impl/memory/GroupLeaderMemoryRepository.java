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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.groupmanager.summary.GroupManagerSummaryInformation;
import org.inria.myriads.snoozecommon.communication.localcontroller.AssignedGroupManager;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozecommon.datastructure.LRUCache;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.database.api.GroupLeaderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Group leader in-memory repository.
 * 
 * @author Eugen Feller
 */
public final class GroupLeaderMemoryRepository 
    implements GroupLeaderRepository 
{
    /** Logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupLeaderMemoryRepository.class);

    /** Start index for the IP address pool. */
    private static final int IP_ADDRESS_START_IDEX = 1;

    
    /** 
     * History data of the group managers.
     * 
     * Key: Group manager identifier
     * Value: Group manager description
     */
    private Map<String, GroupManagerDescription> groupManagerDescriptions_;
    
    /** List of available addresses. */
    private List<String> ipAddressPool_;
    
    /** Maximum number of group manager entries. */
    private int maxCapacity_;

    /** 
     * Constructor.
     * @param groupLeaderDescription   The group leader description 
     * @param virtualMachineSubnets    The virtual machine subnet
     * @param maxCapacity              The maximum capacity
     */
    public GroupLeaderMemoryRepository(GroupManagerDescription groupLeaderDescription,
            String[] virtualMachineSubnets,
            int maxCapacity)
    {
        log_.debug("Initializing the group leader memory repository");
        
        ipAddressPool_ = generateAddressPool(virtualMachineSubnets);
        maxCapacity_ = maxCapacity;
        groupManagerDescriptions_ = new HashMap<String, GroupManagerDescription>();
        
    }

    /**
     * Generates the address pool.
     * 
     * @param virtualMachineSubnets     The virtual machine subnet
     * @return                          The list of IP addresses
     */
    protected List<String> generateAddressPool(String[] virtualMachineSubnets)
    {
        log_.debug("Generating address pool");
        List<String> addressPool = new ArrayList<String>();
        for (String virtualMachineSubnet : virtualMachineSubnets)
        {
            if (!virtualMachineSubnet.equals(""))
            {
                SubnetUtils subnetUtils = new SubnetUtils(virtualMachineSubnet);
                SubnetInfo subnetInfo = subnetUtils.getInfo(); 
                addressPool.addAll(Arrays.asList(subnetInfo.getAllAddresses()));
            }
        }
        
        return addressPool;
    }
    
    /**
     * Adds an IP address based on a string.
     * 
     * @param ipAddress     The ip address
     * @return              true if released, false otherwise
     */
    @Override
    public synchronized boolean addIpAddress(String ipAddress)
    {
        Guard.check(ipAddress);
        log_.debug(String.format("Adding IP %s back to the address pool", ipAddress));
        
        if (ipAddressPool_.contains(ipAddress))
        {
            log_.debug("This IP is already in the address pool!");
            return false;
        }
        
        ipAddressPool_.add(ipAddress);
        return true;
    }
    
    /**
     * Removes an IP address from the pool.
     * 
     * @param ipAddress     The ip address
     * @return              true if removed, false otherwise
     */
    @Override
    public synchronized boolean removeIpAddress(String ipAddress)
    {
        Guard.check(ipAddress);
        log_.debug(String.format("Removing IP address %s from the pool", ipAddress));
        
        if (!ipAddressPool_.contains(ipAddress))
        {
            log_.debug("This IP is not in the address pool!");
            return false;
        }
        
        ipAddressPool_.remove(ipAddress);
        return true;
    }
    
    /**
     * Returns a free IP address.
     * 
     * @return  The free ip address
     */
    @Override
    public synchronized String getFreeIpAddress()
    {
        log_.debug("Returning free IP address from pool");
        if (ipAddressPool_.size() <= 1)
        {
            log_.debug("IP address pool is empty!");
            return null;
        }
        
        String ipAddress = ipAddressPool_.get(IP_ADDRESS_START_IDEX);
        return ipAddress;
    }
    
    /**
     * Adds a group manager description.
     * 
     * @param groupManager      The group manager description
     * @return                  true if everything ok, false otherwise
     */
    @Override
    public synchronized boolean addGroupManagerDescription(GroupManagerDescription groupManager) 
    {
        Guard.check(groupManager);
        String groupManagerId = groupManager.getId();
        log_.debug(String.format("Adding description for group manager %s with address: %s",
                                  groupManagerId, 
                                  groupManager.getListenSettings().getControlDataAddress().getAddress()));
        
        boolean hasDescription = groupManagerDescriptions_.containsKey(groupManagerId);
        if (hasDescription)
        {
            log_.debug("Description for this group manager already exists!");
            return false;
        }
            
        groupManager.setSummaryInformation(new LRUCache<Long, GroupManagerSummaryInformation>(maxCapacity_));
        groupManager.setIsAssigned(true);
        groupManagerDescriptions_.put(groupManagerId, groupManager);     
        removeIpAddresses(groupManager.getLocalControllers());
        

        
        return true;
    }
       
    /**
     * Removes the virtual machine IP addresses.
     * 
     * @param localControllers  The virtual machine meta data list
     * @return                  true if everything ok, false otherwise
     */
    private boolean removeIpAddresses(HashMap<String, LocalControllerDescription> localControllers) 
    {        
        if (localControllers.size() == 0)
        {
            log_.debug("No local controller descriptions available on this group manager yet!");
            return false;
        }
                
        for (LocalControllerDescription localControllerDescription : localControllers.values())
        {            
            Map<String, VirtualMachineMetaData> metaData = localControllerDescription.getVirtualMachineMetaData();
            log_.debug(String.format("Attempting to remove the IP addresses already allocated to %s virtual machines",
                                     metaData.size()));
            for (VirtualMachineMetaData entry : metaData.values())
            {
                boolean isRemoved = removeIpAddress(entry.getIpAddress());
                if (!isRemoved)
                {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Returns the group manager descriptions.
     * 
     * @param numberOfBacklogEntries    The number of backlog entries
     * @return                          The group manager descriptions
     */
    @Override
    public synchronized ArrayList<GroupManagerDescription> getGroupManagerDescriptions(int numberOfBacklogEntries)
    {
        log_.debug(String.format("Getting all group manager descriptions, number of monitoring data entries: %d", 
                                 numberOfBacklogEntries));
        
        ArrayList<GroupManagerDescription> groupManagers = new ArrayList<GroupManagerDescription>();       
        for (GroupManagerDescription groupManager : groupManagerDescriptions_.values())
        {   
            log_.debug(String.format("Gettung group manager description of %s", groupManager.getId()));
            GroupManagerDescription copy = new GroupManagerDescription(groupManager, numberOfBacklogEntries);
            groupManagers.add(copy);
        }
        
        return groupManagers;
    }

    @Override
    public GroupManagerDescription getGroupManagerDescription(String groupManagerId, int numberOfBacklogEntries)
    {
        log_.debug(String.format("Getting group manager description, number of monitoring data entries: %d", 
                numberOfBacklogEntries));
        GroupManagerDescription groupManagerDescription = groupManagerDescriptions_.get(groupManagerId);
        if (groupManagerDescription == null)
        {
            log_.debug(String.format("No group manager found with the id %s in the repository", groupManagerId));
            return null;
        }
        return groupManagerDescription;
    }
    /**
     * Adds group manager data.
     * 
     * @param groupManagerId    The group manager identifier
     * @param summary           The group manager data transporter
     */
    @Override
    public synchronized void addGroupManagerSummaryInformation(String groupManagerId, 
                                                               GroupManagerSummaryInformation summary)
    {
        Guard.check(groupManagerId, summary);
        log_.debug(String.format("Updating history data for group manager: %s", groupManagerId));
       
        GroupManagerDescription groupManagerDescription = groupManagerDescriptions_.get(groupManagerId);
        if (groupManagerDescription == null)
        {
            log_.debug("No group manager description available");
            return;
        }
        
        updateLocalControllerInformation(groupManagerId, summary);
        updateNetworkingInformation(summary);
        updateHistoryData(groupManagerId, summary);
    }

    /**
     * 
     * Update the history data of a given group manager.
     * 
     * @param groupManagerId        group manager id.
     * @param summary               summary.
     */
    private void updateHistoryData(String groupManagerId,
            GroupManagerSummaryInformation summary)
    {
        GroupManagerDescription groupManagerDescription = groupManagerDescriptions_.get(groupManagerId);
        Map<Long, GroupManagerSummaryInformation> historyData = groupManagerDescription.getSummaryInformation();
        // remove lcs ? 
        summary.setLocalControllers(new ArrayList<LocalControllerDescription>());
        historyData.put(summary.getTimeStamp(), summary);
    }

    /**
     * 
     * Updates the mapping local controllers - group manager.
     * 
     * @param summary           The summary information
     * @param groupManagerId    The group manager id
     */
    private void updateLocalControllerInformation(String groupManagerId, GroupManagerSummaryInformation summary)
    {
        log_.debug("Updating the local controllers settings");
        GroupManagerDescription groupManagerDescription = groupManagerDescriptions_.get(groupManagerId);
        HashMap<String, LocalControllerDescription> localControllers = 
                new HashMap<String, LocalControllerDescription>();
        for (LocalControllerDescription localController : summary.getLocalControllers())
        {
            log_.debug(String.format("Adding localController %s to the mapping", localController.getId()));
            localControllers.put(localController.getId(), localController);
        }
        groupManagerDescription.setLocalControllers(localControllers);
    }

    /**
     * Updates the networking information.
     * 
     * @param groupManagerData     The group manager data
     */
    private void updateNetworkingInformation(GroupManagerSummaryInformation groupManagerData) 
    {
        log_.debug("Updating the network information");
        
        List<String> legacyIpAddresses = groupManagerData.getLegacyIpAddresses();
        if (legacyIpAddresses == null)
        {
            log_.debug("The list of legacy IP addresses is NULL!");
            return;
        }
        
        addIpAddresses(legacyIpAddresses);
    }
    
    /**
     * Adds the list of IP addresses back to the database.
     * 
     * @param ipAddresses    The list of IP addresses
     * @return               true if everything ok, false otherwise
     */
    private boolean addIpAddresses(List<String> ipAddresses)
    {        
        if (ipAddresses.size() == 0)
        {
            log_.debug("The IP address list is empty!");
            return false;
        }
    
        log_.debug(String.format("Adding back IP addresses: %s", ipAddresses.toString()));
        for (int i = 0; i < ipAddresses.size(); i++)
        {
            boolean isAdded = addIpAddress(ipAddresses.get(i));
            if (!isAdded)
            {
                return false;
            }
        }
        
        return true;
    }
   
    /**
     * Removes a group manager from the repository.
     * 
     * @param groupManagerId       The group manager identifier
     * @return                     true if everything ok, false otherwise
     */
    @Override
    public synchronized boolean dropGroupManager(String groupManagerId)
    {
        Guard.check(groupManagerId);
        log_.debug(String.format("Dropping the group manager %s", groupManagerId));
        
        if (groupManagerDescriptions_.containsKey(groupManagerId))
        {
            log_.debug("Group manager dropped!");

            groupManagerDescriptions_.remove(groupManagerId);
            
            return true;
        }
        
        log_.debug("No such group manager in the database!");
        return false;
    }
    
    /**
     * 
     * Returns the local controllers list.
     * 
     * @return  The local controllers list
     */
    @Override
    public ArrayList<LocalControllerDescription> getLocalControllerList()
    {
        ArrayList<LocalControllerDescription> localControllers = new ArrayList<LocalControllerDescription>();
        for (GroupManagerDescription groupManager : groupManagerDescriptions_.values()) 
        {
            for (LocalControllerDescription localController : groupManager.getLocalControllers().values())
            {
                localControllers.add(localController);
            }
        }
        return localControllers;
    }

    /**
     * 
     * Gets the group manager assigned to the localcontroller identified by its contact information.
     * 
     * @param contactInformation        the contact address/port of the local controller.
     * @return                          The assigned group manager or null if none is found.
     */
    @Override
    public AssignedGroupManager getAssignedGroupManager(NetworkAddress contactInformation)
    {
        for (GroupManagerDescription groupManager : groupManagerDescriptions_.values())
        {
            for (LocalControllerDescription localController : groupManager.getLocalControllers().values())
            {
                NetworkAddress tmpAddress = localController.getControlDataAddress();
                boolean isEqualAddress = tmpAddress.getAddress().equals(contactInformation.getAddress());
                boolean isEqualPort = tmpAddress.getPort() == contactInformation.getPort();
                if (isEqualAddress && isEqualPort)
                {
                    AssignedGroupManager lookup = new AssignedGroupManager();
                    lookup.setLocalControllerId(localController.getId());
                    lookup.setGroupManager(groupManager);
                    return lookup;
                }
            }
        }
        return null;
    }

    /**
     * 
     * Gets the group manager assigned to the localcontroller identified by its Id.
     * 
     * @param localControllerId         The local controller Id.
     * @return                          The assigned group manager or null if none is found.
     */    
    private AssignedGroupManager getAssignedGroupManager(String localControllerId)
    {
        for (GroupManagerDescription groupManager : groupManagerDescriptions_.values())
        {
            for (LocalControllerDescription localController : groupManager.getLocalControllers().values())
            {
                if (localController.getId().equals(localControllerId))
                {
                    AssignedGroupManager lookup = new AssignedGroupManager();
                    lookup.setLocalControllerId(localController.getId());
                    lookup.setGroupManager(groupManager);
                    return lookup;
                }
            }
        }
        return null;
    }
    
    @Override
    public boolean updateLocation(VirtualMachineLocation location)
    {
        String localControllerId = location.getLocalControllerId();
        AssignedGroupManager lookup = getAssignedGroupManager(localControllerId);
        if (lookup == null)
        {
            return false;
        }
        
        location.setGroupManagerId(lookup.getGroupManager().getId());
        location.setGroupManagerControlDataAddress(
                lookup.getGroupManager().getListenSettings().getControlDataAddress());
        return true;
    }

    
    @Override
    public LocalControllerDescription getLocalControllerDescription(String localControllerId)
    {
        
        for (GroupManagerDescription groupManager : groupManagerDescriptions_.values())
        {
            for (LocalControllerDescription localController : groupManager.getLocalControllers().values())
            {
                if (localController.getId().equals(localControllerId))
                {
                    return localController;
                }
            }
        }
        return null;
    }


}
