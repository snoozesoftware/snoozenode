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
package org.inria.myriads.snoozenode.localcontroller.monitoring.information;

import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.NetworkDemand;
import org.inria.myriads.snoozecommon.guard.Guard;

/**
 * Holds the network traffic information.
 * 
 * @author Eugen Feller
 */
public final class NetworkTrafficInformation 
{
    /** Interface name. */
    private String interfaceName_;
    
    /** Network demand. */
    private NetworkDemand demand_;

    /**
     * Network traffic information constructor.
     *
     * @param interfaceName     The interface name
     */
    public NetworkTrafficInformation(String interfaceName)
    {
        Guard.check(interfaceName);     
        interfaceName_ = interfaceName;
        demand_ = new NetworkDemand();
    }
    
    /**
     * Returns the network demand.
     * 
     * @return      The network demand
     */
    public NetworkDemand getNetworkDemand()
    {
        return demand_;
    }
    
    /**
     * Returns the interface name.
     * 
     * @return  The interface name
     */
    public String getInterfaceName() 
    {
        return interfaceName_;
    }
}
