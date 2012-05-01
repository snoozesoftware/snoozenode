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
package org.inria.myriads.snoozenode.database.api.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.groupmanager.summary.GroupManagerSummaryInformation;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
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
     * 
     * @param virtualMachineSubnet    The virtual machine subnet
     * @param maxCapacity             The maximum capacity
     */
    public GroupLeaderMemoryRepository(String virtualMachineSubnet, int maxCapacity)
    {
        Guard.check(virtualMachineSubnet);
        log_.debug("Initializing the group leader memory repository");
        
        ipAddressPool_ = generateAddressPool(virtualMachineSubnet);
        maxCapacity_ = maxCapacity;
        groupManagerDescriptions_ = new HashMap<String, GroupManagerDescription>();
    }

    /**
     * Generates the address pool.
     * 
     * @param virtualMachineSubnet     The virtual machine subnet
     * @return                         The list of IP addresses
     */
    private List<String> generateAddressPool(String virtualMachineSubnet)
    {
        log_.debug("Generating address pool");
        
        SubnetUtils subnetUtils = new SubnetUtils(virtualMachineSubnet);
        SubnetInfo subnetInfo = subnetUtils.getInfo();
        List<String> addressPool = new ArrayList<String>(Arrays.asList(subnetInfo.getAllAddresses()));
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
        
        Map<Long, GroupManagerSummaryInformation> historyData = groupManagerDescription.getSummaryInformation();
        historyData.put(summary.getTimeStamp(), summary);
        updateNetworkingInformation(summary);
    }
   
    /**
     * Updates the networking information.
     * 
     * @param groupManagerData     The group manager data
     */
    private void updateNetworkingInformation(GroupManagerSummaryInformation groupManagerData) 
    {
        log_.debug("Processing the group manager summary information");
        
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
     * Returns the number of free IP addresses.
     * 
     * @return  The number of IP addresses
     */
    @Override
    public int getNumberOfFreeIpAddresses() 
    {
        if (ipAddressPool_.size() > 1)
        {
            return ipAddressPool_.size();
        }
        
        return 0;
    }
}
