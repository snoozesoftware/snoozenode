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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.HostMonitoringData;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.parser.VirtualClusterParserFactory;
import org.inria.myriads.snoozecommon.parser.api.VirtualClusterParser;
import org.inria.myriads.snoozenode.exception.ConnectorException;
import org.inria.myriads.snoozenode.exception.HostMonitoringException;
import org.inria.myriads.snoozenode.exception.VirtualMachineMonitoringException;
import org.inria.myriads.snoozenode.localcontroller.actuator.ActuatorFactory;
import org.inria.myriads.snoozenode.localcontroller.connector.Connector;
import org.inria.myriads.snoozenode.localcontroller.monitoring.api.HostMonitor;
import org.inria.myriads.snoozenode.localcontroller.monitoring.api.VirtualMachineMonitor;
import org.inria.myriads.snoozenode.localcontroller.monitoring.enums.NetworkDirection;
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
public final class LibVirtVirtualMachineHostMonitor
    extends  HostMonitor
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LibVirtVirtualMachineHostMonitor.class);

    private static final int NETWORK_INTERFACE_POSITION = 0;
                
    /** Connection to the hypervisor. */
    private Connect connect_;

    /** last check.*/
    Map<String, VirtualMachinePastInformation> virtualMachines_;
    

    /**
     * Constructor.
     */
    public LibVirtVirtualMachineHostMonitor()
    {   
        log_.debug("Building new libvirtVirtualMAchineHostMonitor");
        virtualMachines_ = new HashMap<String, VirtualMachinePastInformation>();
    }


    @Override
    public void initialize() throws HostMonitoringException
    {
        String address = settings_.getOptions().get("hostname");
        if (address == null)
        {
            throw new HostMonitoringException("address options is missing");
        }
        
        try
        {
            Connector connector = ActuatorFactory.newHypervisorConnector(address, localController_.getHypervisorSettings());
            connect_ = (Connect) connector.getConnector();
        }
        catch (ConnectorException e)
        {
            throw new HostMonitoringException(e.getMessage());
        }
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

    @Override
    public ArrayList<Double> getTotalCapacity() throws HostMonitoringException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HostMonitoringData getResourceData() throws HostMonitoringException
    {
        
        int[] domainIds;
        HostMonitoringData monitoringData = new HostMonitoringData();
        List<HostMonitoringData> domainMonitoringDatas = new ArrayList<HostMonitoringData>();
        try
        {
            List<String> domainNames = lookupDomains();
            log_.debug(String.format("Found %d domains", domainNames.size()));
            
            for (String domainName : domainNames)
            {
                HostMonitoringData domainMonitoringData = getVirtualMachinesMonitoring(domainName);
                domainMonitoringDatas.add(domainMonitoringData);
                monitoringData = sum(domainMonitoringDatas);
            }
        }
        catch(Exception e)
        {
            log_.debug("Unable to fetch the metrics " + e.getMessage());
        }
        
        return monitoringData;
    }
    
  

    
    private HostMonitoringData sum(List<HostMonitoringData> domainMonitoringDatas)
    {
        double cpu = 0; 
        double mem = 0;
        double rx = 0;
        double tx = 0;
        for (HostMonitoringData monitoringData : domainMonitoringDatas)
        {
           cpu += monitoringData.getUsedCapacity().get("VMS_CPU");
           mem += monitoringData.getUsedCapacity().get("VMS_MEM");
           rx += monitoringData.getUsedCapacity().get("VMS_RX");
           tx += monitoringData.getUsedCapacity().get("VMS_TX");
        }
        HostMonitoringData monitoringData = new HostMonitoringData();
        monitoringData.getUsedCapacity().put("VMS_CPU", cpu);
        monitoringData.getUsedCapacity().put("VMS_MEM", mem);
        monitoringData.getUsedCapacity().put("VMS_RX", rx);
        monitoringData.getUsedCapacity().put("VMS_TX", tx);
        return monitoringData;
    }

    private List<String> lookupDomains() throws LibvirtException
    {
        List<String> domainNames = new ArrayList<String>();
        int[] domainIds = connect_.listDomains();
        for (int domainId : domainIds)
        {
            try
            {
                Domain domain = connect_.domainLookupByID(domainId);
                domainNames.add(domain.getName());
            }
            catch (LibvirtException e)
            {
               
                e.printStackTrace();
            }
            
        }
        return domainNames;
    }

    /**
     * Computes the CPU utilization.
     * 
     * @param currentCpuTime                        The current cpu time
     * @return                                      The prozessor utilzation
     * @throws VirtualMachineMonitoringException 
     * @throws HostMonitoringException 
     */
    private double computeProzessorUtilization(long currentCpuTime, long cpuTimeStamp, long samplingTimeDifference) 
        throws VirtualMachineMonitoringException, HostMonitoringException
    {                  
        
        long cpuTimeDiff = currentCpuTime - cpuTimeStamp;
        double cpuUsagePercentage = cpuTimeDiff / (samplingTimeDifference * 1.0);
        log_.debug("computeProzessorUtilization :"  + currentCpuTime + "-" + cpuTimeStamp + "-" + samplingTimeDifference);
        if (cpuUsagePercentage < 0.0)
        {
            log_.debug("CPU utilization is NEGATIVE!");
            cpuUsagePercentage = 0.0;
        }
              
        return cpuUsagePercentage;
    }
    
    /**
     * Computes the network utilization.
     * 
     * @param currentNetworkTraffic     The current network utilization
     * @param networkDirection          The network direction
     * @return                          Number of kilobytes
     */
//    private double computeNetworkUtilization(double currentNetworkTraffic, NetworkDirection networkDirection) 
//    {                
//        double networkTrafficDifference = 0.0;
//        switch (networkDirection)
//        {
//            case Rx :
//                networkTrafficDifference = currentNetworkTraffic - networkRxBytes_;
//                break;
//                
//            case Tx :
//                networkTrafficDifference = currentNetworkTraffic - networkTxBytes_;
//                break;
//                
//            default :
//                log_.error(String.format("Unknown network direction selected: %s", networkDirection));
//                return 0;
//        }
//        
//        double utilization = networkTrafficDifference / 1024;        
//        return utilization;
//    }

    public HostMonitoringData getVirtualMachinesMonitoring(String virtualMachineId)
            throws VirtualMachineMonitoringException
    {
        Domain domain;
        log_.debug("Fetching the vms metric for " + virtualMachineId);
        HostMonitoringData domainMonitoringData = new HostMonitoringData();
        try
        {
            domain = connect_.domainLookupByName(virtualMachineId);
            log_.debug("Connection done with domain " + virtualMachineId);
            long currentSystemTime = System.nanoTime();
            DomainInfo domainInformation = domain.getInfo();
            VirtualMachinePastInformation pastInformation = virtualMachines_.get(virtualMachineId);

            if (pastInformation==null)
            {
                //first time we see this vm
                VirtualMachinePastInformation previousInformation = new VirtualMachinePastInformation();
                previousInformation.setPreviousCpuTime(domainInformation.cpuTime);
                previousInformation.setPreviousSystemTime(currentSystemTime);
                virtualMachines_.put(virtualMachineId, previousInformation);
                return domainMonitoringData;
            }
            
            double cpuUtilization =  computeCpuUtilization(domainInformation, pastInformation, currentSystemTime);
            double memUtilization = 4;// computeMemUtilization(domainInformation, pastInformation);
            double rxUtilization = 5;// computeRxUtilization(domainInformation, pastInformation, NetworkDirection.Rx);
            double txUtilization = 6;// computeTxUtilization(domainInformation, pastInformation, NetworkDirection.Tx);
            
            pastInformation.setPreviousCpuTime(domainInformation.cpuTime);
            pastInformation.setPreviousSystemTime(currentSystemTime);
            
            domainMonitoringData.setTimeStamp(currentSystemTime);
            HashMap<String, Double> usedCapacity = domainMonitoringData.getUsedCapacity();
            usedCapacity.put("VMS_CPU", cpuUtilization);
            usedCapacity.put("VMS_MEM", memUtilization);
            usedCapacity.put("VMS_RX", rxUtilization);
            usedCapacity.put("VMS_TX", txUtilization);
        }
        catch (LibvirtException e)
        {
            e.printStackTrace();
        }

        return domainMonitoringData;
    }

    private double computeCpuUtilization(DomainInfo domainInformation, VirtualMachinePastInformation pastInformation,
            long currentSystemTime)
    {
        log_.debug("Computing cpu utilization with : ");
        
        long cpuTimeStamp = pastInformation.getPreviousCpuTime();
        log_.debug(cpuTimeStamp  + "");
        long previousSystemTime = pastInformation.getPreviousSystemTime();
        log_.debug("previous time" + previousSystemTime + "");
        log_.debug(domainInformation.cpuTime + "");
        log_.debug("current time" + currentSystemTime + "");
        double utilization  = (domainInformation.cpuTime - cpuTimeStamp)/(currentSystemTime*1.0 - previousSystemTime);
        log_.debug("Returning cpu utilization : " + utilization);
        return utilization;
    }

}
