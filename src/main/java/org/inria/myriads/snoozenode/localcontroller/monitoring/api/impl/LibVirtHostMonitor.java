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
package org.inria.myriads.snoozenode.localcontroller.monitoring.api.impl;

import java.util.ArrayList;

import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.NetworkDemand;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.util.MathUtils;
import org.inria.myriads.snoozenode.exception.HostMonitoringException;
import org.inria.myriads.snoozenode.localcontroller.connector.Connector;
import org.inria.myriads.snoozenode.localcontroller.monitoring.api.HostMonitor;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Libvirt based host monitoring implementation.
 * 
 * @author Eugen Feller
 */
public final class LibVirtHostMonitor 
    implements HostMonitor 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LibVirtHostMonitor.class);
        
    /** Connection to the hypervisor. */
    private Connect connect_;

    /** Network throughput. */
    private NetworkDemand networkCapacity_;
    
    /**
     * Constructor.
     * 
     * @param connector                     The hypervisor connector
     * @param networkCapacity               The network capacity
     * @throws HostMonitoringException      The monitoring exception
     */ 
    public LibVirtHostMonitor(Connector connector, NetworkDemand networkCapacity) 
        throws HostMonitoringException
    {
        Guard.check(connector, networkCapacity);
        log_.debug("Initializing the libvirt based host monitoring");
        connect_ = (Connect) connector.getConnector();
        networkCapacity_ = networkCapacity;
    }
    
    /**
     * Returns a vector of total host capacity.
     * 
     * @return                          The list of double values
     * @throws HostMonitoringException 
     */
    @Override
    public ArrayList<Double> getTotalCapacity() 
        throws HostMonitoringException
    {
        log_.debug("Computing the total host capacity");
        
        double memorySize = 0.0;    
        int numberOfCPUs;
        try 
        {
            NodeInfo nodeInfo = connect_.nodeInfo();
            numberOfCPUs = nodeInfo.cpus;
            memorySize = Double.valueOf(nodeInfo.memory);            
        } 
        catch (LibvirtException exception) 
        {
            throw new HostMonitoringException(String.format("Error getting host monitoring information: %s",
                                                            exception.getMessage()));
        }

        ArrayList<Double> totalCapacity = MathUtils.createCustomVector(numberOfCPUs, 
                                                                       memorySize, 
                                                                       networkCapacity_);       
        log_.debug(String.format("Total host capacity is: %s", totalCapacity));   
        return totalCapacity;
    }
}
