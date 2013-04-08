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
package org.inria.myriads.snoozenode.heartbeat;

import java.io.IOException;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozenode.heartbeat.discovery.GroupLeaderDiscovery;
import org.inria.myriads.snoozenode.heartbeat.handler.GroupManagerHeartbeatHandler;
import org.inria.myriads.snoozenode.heartbeat.listener.HeartbeatListener;
import org.inria.myriads.snoozenode.heartbeat.message.HeartbeatMessage;
import org.inria.myriads.snoozenode.heartbeat.receiver.HeartbeatMulticastReceiver;
import org.inria.myriads.snoozenode.heartbeat.sender.HeartbeatMulticastSender;
import org.inria.myriads.snoozenode.localcontroller.LocalControllerBackend;

/**
 * Heartbeat factory.
 * 
 * @author Eugen Feller
 */
public final class HeartbeatFactory 
{
    /**
     * Hide the consturctor.
     */
    private HeartbeatFactory() 
    {
        throw new UnsupportedOperationException();
    }
    
    /** 
     * Creates a new group leader discovery.
     * 
     * @param heartbeatAddress          The heartbeat address
     * @param timeout                   The time out    
     * @param localControllerBackend    The local controller backend
     * @return                          The group leader discovery object
     * @throws Exception                The exception
     */
    public static GroupLeaderDiscovery newGroupLeaderDiscovery(NetworkAddress heartbeatAddress,
                                                               int timeout,
                                                               LocalControllerBackend localControllerBackend)
        throws Exception
    {
        return new GroupLeaderDiscovery(heartbeatAddress, timeout, localControllerBackend);
    }
    
    /**
     * Creates a new group manager heartbeat handler.
     * 
     * @param heartbeatAddress      The heartbeat address
     * @param timeout               The timeout
     * @param backend               The local controller backend
     * @return                      The group manager heartbeat handler
     * @throws Exception            The exception
     */
    public static GroupManagerHeartbeatHandler newGroupManagerHeartbeatHandler(NetworkAddress heartbeatAddress,
                                                                               String groupManagerId,
                                                                               int timeout,
                                                                               LocalControllerBackend backend)
        throws Exception
    {
        return new GroupManagerHeartbeatHandler(heartbeatAddress, groupManagerId,  timeout, backend);
    }
    
    /**
     * Creates a new heartbeat multicast listener.
     * 
     * @param heartbeatAddress    The heartbeat address
     * @param timeout             The timeout
     * @param heartbeatEvent      The heartbeat event
     * @return                    The heartbeat multicast listener
     * @throws Exception          The exception         
     */
    public static HeartbeatMulticastReceiver newHeartbeatMulticastListener(NetworkAddress heartbeatAddress,
                                                                           int timeout,
                                                                           HeartbeatListener heartbeatEvent)
        throws Exception
    {
        return new HeartbeatMulticastReceiver(heartbeatAddress, timeout, heartbeatEvent);
    }

    /**
     * Creates a new heartbeat multicast sender.
     * 
     * @param heartbeatAddress          The heartbeat address
     * @param interval                  The interval
     * @param heartbeatMessage          The heartbeat messsage
     * @return                          The heartbeat multicast sender
     * @throws IOException              Exception
     */
    public static HeartbeatMulticastSender newHeartbeatMulticastSender(NetworkAddress heartbeatAddress, 
                                                                       int interval,
                                                                       HeartbeatMessage heartbeatMessage) 
        throws IOException 
    {
        return new HeartbeatMulticastSender(heartbeatAddress, interval, heartbeatMessage);
    }
}
