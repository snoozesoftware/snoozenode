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
package org.inria.myriads.snoozenode.groupmanager.virtualnetworkmanager.api.impl;

import java.util.List;

import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.database.api.GroupLeaderRepository;
import org.inria.myriads.snoozenode.groupmanager.virtualclusterparser.VirtualClusterParserFactory;
import org.inria.myriads.snoozenode.groupmanager.virtualclusterparser.api.VirtualClusterParser;
import org.inria.myriads.snoozenode.groupmanager.virtualnetworkmanager.api.VirtualNetworkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Virtual network manager.
 * 
 * @author Eugen Feller
 */
public final class HostVirtualNetworkManager 
    implements VirtualNetworkManager
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(HostVirtualNetworkManager.class);
        
    /** MAC Address prefix. */
    private static final String MAC_PREFIX = "54:56:";
    
    /** Holds the MAC prefix length. */
    private static int MAC_PREFIX_LENGTH = 2;

    /** Group leader repository. */
    private GroupLeaderRepository groupLeaderRepository_;
        
    /**
     * Virtual network manager  constructor.
     * 
     * @param groupLeaderRepository  The group leader repository
     */
    public HostVirtualNetworkManager(GroupLeaderRepository groupLeaderRepository) 
    {
        Guard.check(groupLeaderRepository);
        log_.debug("Starting virtual network manager");
        groupLeaderRepository_ = groupLeaderRepository;
    }

    /**
     * Assigns IP addresses to virtual machines.
     * 
     * @param virtualMachines   The virtual machines
     * @return                  true if everything is ok, false otherwise
     */
    public boolean assignIpAddresses(List<VirtualMachineMetaData> virtualMachines)
    {
        int size = groupLeaderRepository_.getNumberOfFreeIpAddresses();
        if (size < virtualMachines.size())
        {
            log_.warn("Not enough IP addresses avaialble!");
            return false;
        }
        
        for (VirtualMachineMetaData virtualMachine : virtualMachines)
        {
            String virtualMachineId = virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
            boolean isAssigned = assignIpAddress(virtualMachine);    
            if (!isAssigned)
            {
                log_.debug("Unable to assign IP address to virtual machine: %s!", virtualMachineId);
                return false;
            }        
        }
        
        return true;
    }
    
    /**
     * Assign MAC address to the virtual machines.
     * 
     * @param virtualMachineMetaData    The virtual machine meta data
     * @return                          true if assigned, false otherwise
     */
    private boolean assignIpAddress(VirtualMachineMetaData virtualMachineMetaData) 
    {
        Guard.check(virtualMachineMetaData);
        log_.debug(String.format("Assigning MAC address to virtual machine %s and updating IP pool",
                                 virtualMachineMetaData.getVirtualMachineLocation().getVirtualMachineId()));
        
        String freeIpAddress = groupLeaderRepository_.getFreeIpAddress();
        if (freeIpAddress == null)
        {
            return false;
        }
        
        log_.debug(String.format("Free IP address: %s", freeIpAddress));
        virtualMachineMetaData.setIpAddress(freeIpAddress);            
        String newMacAddress = embedIpToMac(freeIpAddress);
        log_.debug(String.format("Embedded MAC address: %s", newMacAddress));
        
        
        VirtualClusterParser parser = VirtualClusterParserFactory.newVirtualClusterParser();
        String newXmlDescription = parser.replaceMacAddressInTemplate(virtualMachineMetaData.getXmlRepresentation(), 
                                                                                            newMacAddress);

        virtualMachineMetaData.setXmlRepresentation(newXmlDescription);
        groupLeaderRepository_.removeIpAddress(freeIpAddress);
        return true;
    }  
         
    /**
     * Embeds IP into MAC.
     * 
     * @param freeIpAddress     The free IP address
     * @return                  The embedded MAC
     */
    private String embedIpToMac(String freeIpAddress) 
    {
        Guard.check(freeIpAddress);
        log_.debug(String.format("Embedding IP address %s into MAC", freeIpAddress));
                
        String macAddress = MAC_PREFIX;
        String [] ipParts = freeIpAddress.split("\\.");
        for (int i = 0; i < ipParts.length; i++)
        {
            int intValue = Integer.parseInt(ipParts[i]);
            String hexValue = Integer.toHexString(intValue);
            
            if (i == (ipParts.length - 1)) 
            {
                macAddress += hexValue;
            } else 
            {
                macAddress += hexValue + ":";
            }
        }
        
        return macAddress;
    }
    
    /**
     * Releases an IP address based on virtual machine meta data.
     * 
     * @param virtualMachineMetaData    The virtual machine meta data
     * @return                          true if released, false otherwise
     */
    public boolean releaseIpAddress(VirtualMachineMetaData virtualMachineMetaData)
    {
        Guard.check(virtualMachineMetaData);
        log_.debug("Releasing assigned IP addresses");
        
        VirtualClusterParser parser = VirtualClusterParserFactory.newVirtualClusterParser();
        String macAddress = parser.getMacAddress(virtualMachineMetaData.getXmlRepresentation());
        
        log_.debug(String.format("The MAC address is: %s", macAddress));
        
        String ipAddress = convertMacToIp(macAddress);
        log_.debug(String.format("The decoded IP address is: %s", ipAddress));
        
        virtualMachineMetaData.setIpAddress("UNKNOWN");
        boolean isAdded = groupLeaderRepository_.addIpAddress(ipAddress);
        return isAdded;
    }

    /**
     * Converts a MAC to IP address.
     * 
     * @param macAddress    The MAC address
     * @return              The IP address
     */
    public static String convertMacToIp(String macAddress) 
    {
        Guard.check(macAddress);
        log_.debug("Converting MAC to IP address");
        
        String tmpIpAddress = "";
        String[] macParts = macAddress.split(":");
        for (int i = MAC_PREFIX_LENGTH; i < macParts.length; i++)
        {
            int intValue = Integer.valueOf(macParts[i], 16);
            String stringValue = String.valueOf(intValue);
                
            if (i == (macParts.length - 1)) 
            {
                tmpIpAddress += stringValue;
            } else 
            {
                tmpIpAddress += stringValue + ".";
            }
        }     
        
        return tmpIpAddress;
    }
}
