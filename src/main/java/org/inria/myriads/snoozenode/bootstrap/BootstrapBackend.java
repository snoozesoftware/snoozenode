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
package org.inria.myriads.snoozenode.bootstrap;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.heartbeat.HeartbeatFactory;
import org.inria.myriads.snoozenode.heartbeat.listener.HeartbeatListener;
import org.inria.myriads.snoozenode.heartbeat.message.HeartbeatMessage;
import org.inria.myriads.snoozenode.util.ManagementUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bootstrap backend logic.
 * 
 * @author Eugen Feller
 */
public final class BootstrapBackend 
    implements HeartbeatListener
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(BootstrapBackend.class);
    
    /** The group leader description. */
    private GroupManagerDescription groupLeaderDescription_;
    
    /**
     * Bootstrap backend constructor.
     * 
     * @param nodeParameters  The node parameters
     * @throws Exception 
     */
    public BootstrapBackend(NodeConfiguration nodeParameters) 
        throws Exception 
    {
        Guard.check(nodeParameters);
        log_.debug("Starting bootstrap backend");
        NetworkAddress address = nodeParameters.getNetworking().getMulticast().getGroupLeaderHeartbeatAddress();
        int heartbeatTimeout = nodeParameters.getFaultTolerance().getHeartbeat().getTimeout();
        new Thread(HeartbeatFactory.newHeartbeatMulticastListener(address, 
                                                                  heartbeatTimeout,
                                                                  this)).start();
    }
    
    /** 
     * Return current group leader.
     *  
     * @return   Group leader information
     */
    public GroupManagerDescription getGroupLeaderDescription() 
    {
        return groupLeaderDescription_;
    }
    
    /**
     * Called by the heartbeat listener upon heartbeat arrival.
     * 
     * @param heartbeatMessage    Heartbeat message
     */
    public void onHeartbeatArrival(HeartbeatMessage heartbeatMessage) 
    {
        Guard.check(heartbeatMessage);
        log_.debug(String.format("Received group leader heartbeat message from: %s, " +
                                 "listen port: %d, " +
                                 "monitoring data port: %d",
                                 heartbeatMessage.getListenSettings().getControlDataAddress().getAddress(),
                                 heartbeatMessage.getListenSettings().getControlDataAddress().getPort(),
                                 heartbeatMessage.getListenSettings().getMonitoringDataAddress().getPort()));
    
        if (groupLeaderDescription_ == null || 
            groupLeaderDescription_.getId().compareTo(heartbeatMessage.getId()) != 0) 
        {
            log_.debug("Updating group leader information");        
            groupLeaderDescription_ = ManagementUtils.createGroupLeaderDescriptionFromHeartbeat(heartbeatMessage);
        }
    }

    /**
     * Called by the heartbeat listener upon heartbeat failure.
     */
    public void onHeartbeatFailure() 
    {
        log_.debug("Group leader does not exist or has failed");
        
        if (groupLeaderDescription_ != null)
        {
            groupLeaderDescription_ = null;
        }
    }
}
