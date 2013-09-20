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
package org.inria.myriads.snoozenode.heartbeat.discovery;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.exception.VirtualMachineMonitoringException;
import org.inria.myriads.snoozenode.heartbeat.HeartbeatFactory;
import org.inria.myriads.snoozenode.heartbeat.listener.GroupLeaderHeartbeatArrivalListener;
import org.inria.myriads.snoozenode.heartbeat.listener.HeartbeatListener;
import org.inria.myriads.snoozenode.heartbeat.message.HeartbeatMessage;
import org.inria.myriads.snoozenode.heartbeat.receiver.HeartbeatMulticastReceiver;
import org.inria.myriads.snoozenode.util.ManagementUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Group leader discovery.
 * 
 * @author Eugen Feller
 */
public final class GroupLeaderDiscovery 
    implements HeartbeatListener 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupLeaderDiscovery.class);
    
    /** Heartbeat listener thread. */
    private HeartbeatMulticastReceiver hearbeatListener_;

    /** Group leader heartbeat listener. */
    private GroupLeaderHeartbeatArrivalListener groupLeaderHeartbeat_;

    /**
     * Group leader heartbeat handler.
     * 
     * @param heartbeatAddress          The heartbeat address
     * @param timeout                   The timeout
     * @param groupLeaderHeartbeat      The group leader heartbeat
     * @throws Exception                The exception
     */
    public GroupLeaderDiscovery(NetworkAddress heartbeatAddress,
                                int timeout,
                                GroupLeaderHeartbeatArrivalListener groupLeaderHeartbeat) 
        throws Exception
    {
        Guard.check(heartbeatAddress, timeout, groupLeaderHeartbeat);
        log_.debug("Starting the group leader discovery");
        
        groupLeaderHeartbeat_ = groupLeaderHeartbeat;
        hearbeatListener_ = HeartbeatFactory.newHeartbeatMulticastListener(heartbeatAddress, 
                                                                           timeout, 
                                                                           this);
        new Thread(hearbeatListener_, "GroupLeaderDiscovery").start();       
    }
    
    
    /**
     * Group leader heartbeat arrival event handler.
     * 
     * @param heartbeatMessage     The heartbeat message
     */
    public void onHeartbeatArrival(HeartbeatMessage heartbeatMessage) 
    {
        log_.debug(String.format("Group leader heartbeat message received from: %s, control data port: %s, " +
                                 "monitoring data port: %s", 
                                 heartbeatMessage.getListenSettings().getControlDataAddress().getAddress(),
                                 heartbeatMessage.getListenSettings().getControlDataAddress().getPort(),
                                 heartbeatMessage.getListenSettings().getMonitoringDataAddress().getPort()));  
        
        GroupManagerDescription groupLeaderDescription = 
            ManagementUtils.createGroupLeaderDescriptionFromHeartbeat(heartbeatMessage);         
        boolean isAssigned = false;
        try
        {
            isAssigned = groupLeaderHeartbeat_.onGroupLeaderHeartbeatArrival(groupLeaderDescription);
        }
        catch (VirtualMachineMonitoringException exception)
        {
            log_.error(String.format("Error in monitoring logic: %s", exception.getMessage()));
        }
        catch (Exception exception)
        {
            log_.error("Exception happened", exception);
        } 
        finally
        {
            if (!isAssigned)
            {
                log_.debug("Unable to join the network");
                return;
            } 
            
            log_.debug("Join procedure was successfull");
            hearbeatListener_.terminate(); 
        }
    }
    
    /** 
     *  Handle the heartbeat failure event.
     */
    public void onHeartbeatFailure() 
    {
        log_.debug("No valid group leader available! Waiting..");  
    }
}
