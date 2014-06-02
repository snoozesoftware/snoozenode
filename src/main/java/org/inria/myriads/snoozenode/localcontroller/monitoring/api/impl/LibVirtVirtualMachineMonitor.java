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
package org.inria.myriads.snoozenode.localcontroller.monitoring.api.impl;

import java.util.ArrayList;
import java.util.List;

import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.parser.VirtualClusterParserFactory;
import org.inria.myriads.snoozecommon.parser.api.VirtualClusterParser;
import org.inria.myriads.snoozenode.exception.VirtualMachineMonitoringException;
import org.inria.myriads.snoozenode.localcontroller.connector.Connector;
import org.inria.myriads.snoozenode.localcontroller.monitoring.api.VirtualMachineMonitor;
import org.inria.myriads.snoozenode.localcontroller.monitoring.information.NetworkTrafficInformation;
import org.inria.myriads.snoozenode.localcontroller.monitoring.information.VirtualMachineInformation;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;
import org.libvirt.DomainInterfaceStats;
import org.libvirt.LibvirtException;
import org.libvirt.MemoryStatistic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Libvirt based virtual machine monitor.
 * 
 * @author Eugen Feller
 */
public final class LibVirtVirtualMachineMonitor
    implements VirtualMachineMonitor
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LibVirtVirtualMachineMonitor.class);
                
    /** Connection to the hypervisor. */
    private Connect connect_;
    
    /**
     * Constructor.
     * 
     * @param connector                     The hypervisor connector
     * @throws VirtualMachineMonitoringException Exception 
     */
    public LibVirtVirtualMachineMonitor(Connector connector) 
        throws VirtualMachineMonitoringException
    {
        Guard.check(connector);
        log_.debug("Initializing the libvirt based virtual machine monitoring");
        connect_ = (Connect) connector.getConnector();
    }
    
    /**
     * Returns the resource usage information of a domain.
     * 
     * @param virtualMachineId                       The virtual machine identifier
     * @return                                       The virtual machine information
     * @throws VirtualMachineMonitoringException     Exception 
     */
    @Override
    public VirtualMachineInformation getVirtualMachineInformation(String virtualMachineId) 
        throws VirtualMachineMonitoringException
    {
        Guard.check(virtualMachineId);
        log_.debug(String.format("Getting information for virtual machine: %s", virtualMachineId));
        
        Domain domain;
        DomainInfo domainInformation;
        
        try 
        {
            domain = connect_.domainLookupByName(virtualMachineId);
            domainInformation = domain.getInfo();
        } 
        catch (LibvirtException exception) 
        {
            throw new VirtualMachineMonitoringException(String.format("Failed to lookup domain information: %s", 
                                                                      exception.getMessage()));
        }
                            
        long currentMemoryUsage = getCurrentMemoryUsage(domain);
        List<NetworkTrafficInformation> networkTraffic = getCurrentNetworkUsage(domain);
        VirtualMachineInformation information = new VirtualMachineInformation(domainInformation.nrVirtCpu,
                                                                              domainInformation.cpuTime,
                                                                              currentMemoryUsage,
                                                                              networkTraffic);
        return information;
    }
    
    /**
     * Gets the network traffic information for all interfaces.
     * 
     * @param domain                                The domain
     * @return                                      The traffic information
     * @throws VirtualMachineMonitoringException    The virtual machine monitoring exception
     */
    private List<NetworkTrafficInformation> getCurrentNetworkUsage(Domain domain) 
        throws VirtualMachineMonitoringException 
    {
        Guard.check(domain);
        log_.debug("Getting the network traffic information for all interfaces");
        
        List<String> networkInterfaces = null;
        try 
        {   
            VirtualClusterParser parser = VirtualClusterParserFactory.newVirtualClusterParser();
            networkInterfaces = parser.getNetworkInterfaces(domain.getXMLDesc(1));
        } 
        catch (Exception exception) 
        {
            throw new VirtualMachineMonitoringException(String.format("Unable to get domain XML description: %s",
                                                                      exception.getMessage()));
        } 
        
        
        log_.debug(String.format("Size of the network list: %s", networkInterfaces.size()));
        
        List<NetworkTrafficInformation> networkTrafficInformation = 
            computeNetworkTrafficInformation(networkInterfaces, domain); 
        return networkTrafficInformation;
    }
    
    /**
     * Computes the network traffic information.
     * 
     * @param networkInterfaces     The network interfaces
     * @param domain                The domain
     * @return                      The list of network traffic information
     */
    private List<NetworkTrafficInformation> computeNetworkTrafficInformation(List<String> networkInterfaces,
                                                                             Domain domain)
    {
        Guard.check(networkInterfaces, domain);
        log_.debug(String.format("Computing the network traffic information for: %s", 
                                 networkInterfaces.toString()));
        
        List<NetworkTrafficInformation> networkTrafficInformation = new ArrayList<NetworkTrafficInformation>();
        for (int i = 0; i < networkInterfaces.size(); i++)
        {
            DomainInterfaceStats domainInterfaceStats;
            try 
            {
                domainInterfaceStats = domain.interfaceStats(networkInterfaces.get(i));
            } 
            catch (LibvirtException exception) 
            {
                log_.debug(String.format("Failed to get interface information for: %s, exception: %s",
                                         networkInterfaces.get(i), 
                                         exception.getMessage()));
                continue;
            }              
            
            NetworkTrafficInformation networkTraffic = new NetworkTrafficInformation(networkInterfaces.get(i));
            networkTraffic.getNetworkDemand().setRxBytes(domainInterfaceStats.rx_bytes);
            networkTraffic.getNetworkDemand().setTxBytes(domainInterfaceStats.tx_bytes);
            networkTrafficInformation.add(networkTraffic);          
        }
        
        return networkTrafficInformation;         
    }
    
    /**
     * Returns the current memory usage of a domain.
     * 
     * Note: xen driver doesn't support virDomainMemoryStats called by domain.memoryStats (libvirt 0.9.8)
     * see http://libvirt.org/hvsupport.html  
     * 
     * @param domain                                    The domain
     * @return                                          The memory usage
     * @throws VirtualMachineMonitoringException 
     */
    private long getCurrentMemoryUsage(Domain domain) 
        throws VirtualMachineMonitoringException
    {
        Guard.check(domain);
        log_.debug("Getting current domain memory usage information");
        
        long usedMemory;
        MemoryStatistic[] memStats;
        try 
        {
            try 
            {
                memStats = domain.memoryStats(1);
                log_.debug(String.format("Size of memory stats: %d", memStats.length));
            }
            catch (LibvirtException exception)
            {
               log_.debug("No dynamic memory usage information available! Falling back to fixed memory allocation! : ");
               return domain.getInfo().memory;
            }
            
            if (memStats.length > 0)
            {
                MemoryStatistic memory = memStats[0];
                log_.debug(String.format("Good news! Dynamic memory usage information is available: %d", 
                                         memory.getValue()));
                return memory.getValue();
            }
            
            log_.debug("No dynamic memory usage information available! Falling back to fixed memory allocation!");
            usedMemory = domain.getInfo().memory;
        } 
        catch (LibvirtException exception) 
        {
            throw new 
                VirtualMachineMonitoringException(String.format("Unable to capture current memory information: %s", 
                                                                exception.getMessage()));
        } 
        
        return usedMemory;
    }
}
