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
package org.inria.myriads.snoozenode.localcontroller.actuator;

import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.HypervisorSettings;
import org.inria.myriads.snoozenode.exception.ConnectorException;
import org.inria.myriads.snoozenode.localcontroller.actuator.api.VirtualMachineActuator;
import org.inria.myriads.snoozenode.localcontroller.actuator.api.impl.LibVirtVirtualMachineActuator;
import org.inria.myriads.snoozenode.localcontroller.connector.Connector;
import org.inria.myriads.snoozenode.localcontroller.connector.impl.LibVirtConnector;

/**
 * Actuator factory.
 * 
 * @author Eugen Feller
 */
public final class ActuatorFactory 
{
    /**
     * Hide the consturctor.
     */
    private ActuatorFactory() 
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Creates a new hypervisor connector.
     * 
     * @param listenAddress         The listen address
     * @param settings              The hypervisor settings
     * @return                      The connector object
     * @throws ConnectorException   The connector exception
     */
    public static Connector newHypervisorConnector(String listenAddress, HypervisorSettings settings) 
        throws ConnectorException
    {
        return new LibVirtConnector(listenAddress, settings);
    }
    
    /**
     * Creates the host actuator.
     * 
     * @param connector                             The connector object
     * @return                                      The Virtual machine actuator
     */
    public static VirtualMachineActuator newVirtualMachineActuator(Connector connector)
    {
        return new LibVirtVirtualMachineActuator(connector);
    }
}
