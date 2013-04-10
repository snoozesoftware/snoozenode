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
package org.inria.myriads.snoozenode.heartbeat.handler;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.heartbeat.HeartbeatFactory;
import org.inria.myriads.snoozenode.heartbeat.listener.GroupManagerHeartbeatFailureListener;
import org.inria.myriads.snoozenode.heartbeat.listener.HeartbeatListener;
import org.inria.myriads.snoozenode.heartbeat.message.HeartbeatMessage;
import org.inria.myriads.snoozenode.heartbeat.receiver.HeartbeatMulticastReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Group manager heartbeat handler.
 * 
 * @author Eugen Feller
 */
public final class GroupManagerHeartbeatHandler 
    implements HeartbeatListener 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupManagerHeartbeatHandler.class);
    
    /** Heartbeat listener thread. */
    private HeartbeatMulticastReceiver hearbeatListener_;

    /** Heartbeat failure callback. */
    private GroupManagerHeartbeatFailureListener heartbeatFailure_;

    
    /** GroupManagerId. */
    private String groupManagerId_; 
    
    /**
     * Group manager heartbeat handler constructor.
     * 
     * @param heartbeatAddress      The multicast address
     * @param timeout               The timeout
     * @param heartbeatFailure      The heartbeat failure callback
     * @throws Exception        
     */
    public GroupManagerHeartbeatHandler(NetworkAddress heartbeatAddress,
                                        String groupManagerId,
                                        int timeout,
                                        GroupManagerHeartbeatFailureListener heartbeatFailure) 
        throws Exception
    {
        Guard.check(heartbeatAddress, timeout, heartbeatFailure);
        log_.debug("Initializing the group manager heartbeat listener");
        heartbeatFailure_ = heartbeatFailure;
        hearbeatListener_ = HeartbeatFactory.newHeartbeatMulticastListener(heartbeatAddress, timeout, this);
        groupManagerId_ = groupManagerId;
        new Thread(hearbeatListener_).start();       
    }
    
    /**
     * Processes the heartbeat arrival event.
     * 
     * @param heartbeatMessage  The heartbeat message
     */
    public void onHeartbeatArrival(HeartbeatMessage heartbeatMessage) 
    {
        Guard.check(heartbeatMessage);
        String groupManagerId = heartbeatMessage.getId();
        if (groupManagerId.equals(groupManagerId_))
        {
            log_.debug(String.format("Received group manager hearbeat message from %s, control data port: %s, " +
                    "monitoring data port: %s",
                    heartbeatMessage.getListenSettings().getControlDataAddress().getAddress(),
                    heartbeatMessage.getListenSettings().getControlDataAddress().getPort(),
                    heartbeatMessage.getListenSettings().getMonitoringDataAddress().getPort()));    
        }
        else
        {
            log_.debug("Received wrong group manager heartbeat message ... trigger a rejoin");
            onHeartbeatFailure();
        }
    }

    /**
     * Processes the heartbeat failure event.
     */
    public void onHeartbeatFailure() 
    {
        log_.debug("Failed to receive group manager heartbeat message! Starting the recovery procedure!");
        hearbeatListener_.terminate();       
        try 
        {
            heartbeatFailure_.onGroupManagerHeartbeatFailure();
        }
        catch (Exception exception)
        {
            log_.error("Exception", exception);
        }
    }
}
