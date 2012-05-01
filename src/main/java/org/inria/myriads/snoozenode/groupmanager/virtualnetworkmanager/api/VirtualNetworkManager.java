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
package org.inria.myriads.snoozenode.groupmanager.virtualnetworkmanager.api;

import java.util.List;

import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;

/**
 * Virtual network manager interface.
 * 
 * @author Eugen Feller
 */
public interface VirtualNetworkManager 
{
    /**
     * Assigns IP addresses to virtual machines.
     * 
     * @param virtualMachines   The virtual machines
     * @return                  true if everything is ok, false otherwise
     */
    boolean assignIpAddresses(List<VirtualMachineMetaData> virtualMachines);
    
    /**
     * Releases an IP address based on virtual machine meta data.
     * 
     * @param virtualMachineMetaData    The virtual machine meta data
     * @return                          true if released, false otherwise
     */
    boolean releaseIpAddress(VirtualMachineMetaData virtualMachineMetaData);
}
