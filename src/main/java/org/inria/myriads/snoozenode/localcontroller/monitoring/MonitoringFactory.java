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
package org.inria.myriads.snoozenode.localcontroller.monitoring;

import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.NetworkDemand;
import org.inria.myriads.snoozenode.exception.HostMonitoringException;
import org.inria.myriads.snoozenode.exception.VirtualMachineMonitoringException;
import org.inria.myriads.snoozenode.localcontroller.connector.Connector;
import org.inria.myriads.snoozenode.localcontroller.monitoring.api.HostMonitor;
import org.inria.myriads.snoozenode.localcontroller.monitoring.api.VirtualMachineMonitor;
import org.inria.myriads.snoozenode.localcontroller.monitoring.api.impl.LibVirtHostMonitor;
import org.inria.myriads.snoozenode.localcontroller.monitoring.api.impl.LibVirtVirtualMachineMonitor;

/**
 * Infrastructure monitoring factory.
 * 
 * @author Eugen Feller
 */
public final class MonitoringFactory 
{
    /**
     * Hide the consturctor.
     */
    private MonitoringFactory() 
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Creates the virtual machine monitoring.
     * 
     * @param connector                             The connector object
     * @return                                      The virtual machine monitor
     * @throws VirtualMachineMonitoringException    Exception 
     */
    public static VirtualMachineMonitor newVirtualMachineMonitor(Connector connector) 
        throws VirtualMachineMonitoringException
    {
        return new LibVirtVirtualMachineMonitor(connector);
    }
    
    /**
     * Creates the host monitoring.
     * 
     * @param connector                        The connector object
     * @param networkCapacity                  The network capacity
     * @return                                 The host monitor
     * @throws HostMonitoringException         The monitoring exception
     */
    public static HostMonitor newHostMonitoring(Connector connector, NetworkDemand networkCapacity) 
        throws HostMonitoringException
    {
        return new LibVirtHostMonitor(connector, networkCapacity);
    }
}
