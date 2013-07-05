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
package org.inria.myriads.snoozenode.localcontroller.monitoring.listener;

import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;

/**
 * Virtual machine monitoring callback.
 * 
 * @author Eugen Feller
 */
public interface VirtualMachineMonitoringListener 
{
    /**
     * Drops virtual machine meta data.
     * 
     * @param location      The virtual machine location
     * @return              true if everything ok, false otherwise
     */
    boolean onMonitoringFailure(VirtualMachineLocation location);
        
    /**
     * Returns the number of virtual machines.
     * 
     * @return      The number of virtual machines
     */
    int getNumberOfActiveVirtualMachines();
}
