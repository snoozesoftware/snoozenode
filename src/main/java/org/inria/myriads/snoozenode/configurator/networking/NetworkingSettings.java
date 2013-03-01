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
package org.inria.myriads.snoozenode.configurator.networking;

import org.inria.myriads.snoozecommon.communication.groupmanager.ListenSettings;


/**
 * Networking settings.
 * 
 * @author Eugen Feller
 */
public final class NetworkingSettings 
{
    /** Virtual machine subnet. */
    private String virtualMachineSubnet_;
   
    /** Listen parameters. */
    private ListenSettings listen_;

    /** Multicast settings. */
    private MulticastSettings multicast_;
    
    /** Constructor. */
    public NetworkingSettings()
    {
        listen_ = new ListenSettings();
        multicast_ = new MulticastSettings();
    }
    
    /**
     * Returns the multicast parameters.
     * 
     * @return  The multicast parameters
     */
    public MulticastSettings getMulticast()
    {
        return multicast_;
    }
    
    /**
     * Returns the listen parameters.
     * 
     * @return  The listen parameters
     */
    public ListenSettings getListen()
    {
        return listen_;
    }
    
    /**
     * Sets the virtual machine subnet.
     * 
     * @param virtualMachineSubnet  The virtual machine subnet
     */
    public void setVirtualMachineSubnet(String virtualMachineSubnet) 
    {
        virtualMachineSubnet_ = virtualMachineSubnet;
    }

    /**
     * Returns the virtual machine subnet.
     * 
     * @return  The virtual machine subnet
     */
    public String getVirtualMachineSubnet() 
    {
        return virtualMachineSubnet_;
    }
}
