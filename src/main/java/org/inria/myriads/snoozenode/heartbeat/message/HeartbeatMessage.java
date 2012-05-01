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
package org.inria.myriads.snoozenode.heartbeat.message;

import java.io.Serializable;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.ListenSettings;

/**
 * Heartmeat message.
 * 
 * @author Eugen Feller
 */
public final class HeartbeatMessage 
    implements Serializable 
{
    /** Default version. */
    private static final long serialVersionUID = 1750768760432934260L;
    
    /** Identifier. */
    private String id_;
    
    /** Control data address. */
    private ListenSettings listenSettings_;
    
    /** Heartbeat address. */
    private NetworkAddress heartbeatAddress_;

    /**
     * Sets the identifier.
     * 
     * @param id    The identifier
     */
    public void setId(String id) 
    {
        id_ = id;
    }
    
    /**
     * Returns the identifier.
     * 
     * @return  The identifier
     */
    public String getId() 
    {
        return id_;
    }

    /**
     * Sets the listen settings.
     * 
     * @param listenSettings    The listen settings
     */
    public void setListenSettings(ListenSettings listenSettings) 
    {
        listenSettings_ = listenSettings;
    }

    /**
     * Returns the listen settings.
     * 
     * @return  The listen settings
     */
    public ListenSettings getListenSettings() 
    {
        return listenSettings_;
    }

    /**
     * Sets the heartbeat address.
     * 
     * @param heartbeatAddress  The heartbeat address
     */
    public void setHeartbeatAddress(NetworkAddress heartbeatAddress) 
    {
        heartbeatAddress_ = heartbeatAddress;
    }

    /**
     * Returns the heartbeat address.
     * 
     * @return  The heartbeat address
     */
    public NetworkAddress getHeartbeatAddress() 
    {
        return heartbeatAddress_;
    }
}
