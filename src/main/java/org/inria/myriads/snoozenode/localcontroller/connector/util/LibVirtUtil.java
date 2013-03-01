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
package org.inria.myriads.snoozenode.localcontroller.connector.util;

import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.HypervisorDriver;
import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.HypervisorSettings;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.exception.ConnectorException;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connector utility.
 * 
 * @author Eugen Feller
 */
public final class LibVirtUtil 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LibVirtUtil.class);
    
    /**
     * Hide the consturctor.
     */
    private LibVirtUtil() 
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Establish a connection to the hypervisor.
     * 
     * @param listenAddress         Listen address
     * @param settings              The hypervisor settings
     * @return                      Connection instance
     * @throws ConnectorException   Connector exception
     */
    public static Connect connectToHypervisor(String listenAddress, HypervisorSettings settings) 
        throws ConnectorException 
    {        
        Guard.check(listenAddress, settings);
        HypervisorDriver driver = settings.getDriver();
        log_.debug(String.format("Estabilishing connection to the: %s hypervisor",  driver));
        
        String hypervisorConnection = ""; 
        Connect connect;
        try 
        {
            hypervisorConnection = gethypervisorConnection(listenAddress, settings);
            connect = new Connect(hypervisorConnection, false);
            
            log_.debug(String.format("Hostname: %s", connect.getHostName()));
            //log_.debug(String.format("Max number of VCPUs: %s", connect.getMaxVcpus("kvm")));
            log_.debug(String.format("Type: %s", connect.getType()));
            log_.debug(String.format("URI: %s", connect.getURI()));
            log_.debug(String.format("LibVirt version: %s", connect.getLibVirVersion())); 
        }
        catch (LibvirtException exception)
        {
            throw new ConnectorException(String.format("Error connecting to the hypervisor: %s",
                                                       exception.getMessage()));
        }
        
        return connect;
    }
    
    /**
     * Build the connection Address to the Hypervisor.
     * 
     * @param listenAddress         Listen address
     * @param settings              The hypervisor settings
     * @return                      Address String
     */
    private static String gethypervisorConnection(String listenAddress, HypervisorSettings settings)
    {
        Guard.check(listenAddress, settings);
        log_.debug("Building the hypervisorConnection");
        String hypervisorConnection = "";
        HypervisorDriver driver = settings.getDriver();
        switch(driver)
        {
        case test:
            hypervisorConnection = driver + ":///default";
            break;
        case xen: 
            hypervisorConnection  = driver + "+" + settings.getTransport() + "://" + 
                    listenAddress + "/";
            break;
        default:
            String connectionAddress = listenAddress + ":" + settings.getPort();
            hypervisorConnection = driver + "+" + settings.getTransport() + "://" + 
                                          connectionAddress + "/system";
            break;
        }
        log_.debug(String.format("hypervisorConnection : %s",  hypervisorConnection));
        return hypervisorConnection;
    }
}
