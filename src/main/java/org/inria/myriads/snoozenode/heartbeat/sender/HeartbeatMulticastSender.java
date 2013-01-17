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
package org.inria.myriads.snoozenode.heartbeat.sender;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.heartbeat.message.HeartbeatMessage;
import org.inria.myriads.snoozenode.util.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Heartbeat message sender.
 * 
 * @author Eugen Feller
 */
public final class HeartbeatMulticastSender
    implements Runnable
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(HeartbeatMulticastSender.class);
     
    /** Heartbeat message. */
    private HeartbeatMessage hearbeatMessage_;

    /** Heartbeat address. */
    private NetworkAddress heartbeatAddress_;
    
    /** Datagram socket. */
    //private DatagramSocket socket_;
    private MulticastSocket socket_;
    
    /** Lock object. */
    private Object lockObject_;
    
    /** Heartbeat interval. */
    private int heartbeatInterval_;

    /** Terminated. */
    private boolean isTerminated_;
    
    /**
     * Constructor.
     * 
     * @param heartbeatAddress      The heartbeat address
     * @param heartbeatInterval     The heartbeat interval
     * @param hearbeatMessage       The heartbeat message
     * @throws IOException 
     */
    public HeartbeatMulticastSender(NetworkAddress heartbeatAddress,
                                    int heartbeatInterval,
                                    HeartbeatMessage hearbeatMessage) 
        throws IOException 
    {
        Guard.check(heartbeatAddress, heartbeatInterval, hearbeatMessage);
        log_.debug(String.format("Starting multicast heartbeat sender on the group %s with port %d",
                                  heartbeatAddress.getAddress(), heartbeatAddress.getPort()));
        
        heartbeatAddress_ = heartbeatAddress;
        heartbeatInterval_ = heartbeatInterval;
        hearbeatMessage_ = hearbeatMessage;
        //socket_ = new DatagramSocket();
        socket_ = new MulticastSocket();
        //socket_.setNetworkInterface(NetworkInterface.getByName("eth1"));
        lockObject_ = new Object();
        log_.debug("sending on interface : " + socket_.getNetworkInterface().getDisplayName());
    }

    /**
     * Run method.
     */
    public void run() 
    {
        try 
        {
            while (!isTerminated_) 
            {
                log_.debug(String.format("Sending heartbeat message to: %s:%s",
                                         heartbeatAddress_.getAddress(), 
                                         heartbeatAddress_.getPort()));
                send(SerializationUtils.serializeObject(hearbeatMessage_));
                synchronized (lockObject_)
                {
                    lockObject_.wait(heartbeatInterval_);
                }
            }
        } 
        catch (IOException exception) 
        {
            log_.error(String.format("I/O exception during sending: %s", exception.getMessage()));
        } 
        catch (InterruptedException exception) 
        {
            log_.error("Heartbeat multicast sender was interrupted", exception);
        } 
        finally
        {
            close();
        }

        log_.debug("Heartbeat multicast sender is stopped!");
    }

    /** 
     * Send a multicast message.
     *  
     * @param message                   The message in bytes
     * @throws IOException          
     * @throws InterruptedException 
     */
    private void send(byte[] message)
        throws IOException, InterruptedException 
    {   
        Guard.check(message);
        InetAddress address = InetAddress.getByName(heartbeatAddress_.getAddress());     
        DatagramPacket outPacket = new DatagramPacket(message, 
                                                      message.length, 
                                                      address, 
                                                      heartbeatAddress_.getPort());       

        socket_.send(outPacket);
    }
    
    /**
     * Close socket.
     */
    private void close()
    {
        if (socket_ != null)
        {
            socket_.close();
        }
    }
    
    /** 
     * Terminates the thread.
     */
    public synchronized void terminate() 
    {
        log_.debug("Terminating the heartbeat multicast sender");
        synchronized (lockObject_)
        {
            isTerminated_ = true;
            lockObject_.notify();
        }
    }
}
