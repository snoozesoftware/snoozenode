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
package org.inria.myriads.snoozenode.configurator.networking;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;

/**
 * Multicast settings.
 * 
 * @author Eugen Feller
 */
public class MulticastSettings 
{
    /** Group leader heartbeat address. */
    private NetworkAddress groupLeaderHeartbeatAddress_;
    
    /** Group manager heartbeat address. */
    private NetworkAddress groupManagerHeartbeatAddress_;
    
    /** Constructor. */
    public MulticastSettings()
    {
        groupLeaderHeartbeatAddress_ = new NetworkAddress();
        groupManagerHeartbeatAddress_ = new NetworkAddress();
    }
    
    /**
     * Sets the group leader heartbeat address.
     * 
     * @param groupLeaderHeartbeatAddress   The group leader heartbeat address
     */
    public void setGroupLeaderHeartbeatAddress(NetworkAddress groupLeaderHeartbeatAddress) 
    {
        groupLeaderHeartbeatAddress_ = groupLeaderHeartbeatAddress;
    }

    /**
     * Returns the group leader heartbeat address.
     * 
     * @return  The heartbeat address
     */
    public NetworkAddress getGroupLeaderHeartbeatAddress() 
    {
        return groupLeaderHeartbeatAddress_;
    }
    
    /**
     * Sets the group manager heartbeat address.
     * 
     * @param groupManagerHeartbeatAddress      The group manager heartbeat address
     */
    public void setGroupManagerHeartbeatAddress(NetworkAddress groupManagerHeartbeatAddress) 
    {
        groupManagerHeartbeatAddress_ = groupManagerHeartbeatAddress;
    }

    /**
     * Returns the group manager heartbeat address.
     * 
     * @return  The group manager heartbeat address
     */
    public NetworkAddress getGroupManagerHeartbeatAddress() 
    {
        return groupManagerHeartbeatAddress_;
    }
}
