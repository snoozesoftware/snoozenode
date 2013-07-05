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
package org.inria.myriads.snoozenode.configurator.faulttolerance;

/**
 * Fault tolenrace settings.
 * 
 * @author Eugen Feller
 */
public final class FaultToleranceSettings 
{    
    /** ZooKeeper parameters. */
    private ZooKeeperSettings zooKeeper_;
    
    /** Heartbeat parameters. */
    private HeartbeatSettings heartbeat_;

    /** Constructor. */
    public FaultToleranceSettings()
    {
        zooKeeper_ = new ZooKeeperSettings();
        heartbeat_ = new HeartbeatSettings();
    }
    
    /**
     * Sets the ZooKeeper parameters.
     * 
     * @param zooKeeper  The ZooKeeper parameters
     */
    public void setZooKeeper(ZooKeeperSettings zooKeeper)
    {
        zooKeeper_ = zooKeeper;
    }

    /**
     * Returns the ZooKeeper parameters.
     * 
     * @return  The ZooKeeper parameters
     */
    public ZooKeeperSettings getZooKeeper() 
    {
        return zooKeeper_;
    }

    /**
     * Sets the heartbeat parameters.
     * 
     * @param heartbeat   The heartbeat parameters
     */
    public void setHeartbeat(HeartbeatSettings heartbeat) 
    {
        heartbeat_ = heartbeat;
    }

    /**
     * Returns the heartbeat parameters.
     * 
     * @return  The heartbeat parameters
     */
    public HeartbeatSettings getHeartbeat() 
    {
        return heartbeat_;
    }
}
