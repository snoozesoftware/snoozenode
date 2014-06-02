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
package org.inria.myriads.snoozenode.heartbeat.receiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.heartbeat.listener.HeartbeatListener;
import org.inria.myriads.snoozenode.heartbeat.message.HeartbeatMessage;
import org.inria.myriads.snoozenode.util.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Heartbeat multicast listener.
 * 
 * @author Eugen Feller
 */
public final class HeartbeatMulticastReceiver 
    implements Runnable
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(HeartbeatMulticastReceiver.class);
    
    /** Size of buffer. */
    private static final int BUFF_SIZE = 65535;
      
    /** Multicast socket reference. */
    private MulticastSocket mcastSocket_;
    
    /** Heartbeat event holder. */
    private HeartbeatListener heartbeatEvent_;
    
    /** Timeout. */
    private int timeout_;
    
    /** Signals termination. */
    private boolean isTerminated_;
    
    /**
     * Heartbeat multicast listener constructor.
     *  
     * @param heartbeatAddress      The heartbeat address
     * @param timeout               The timeout
     * @param heartbeatEvent        The  heartbeat callback
     * @throws IOException          Exception
     */
    public HeartbeatMulticastReceiver(NetworkAddress heartbeatAddress, 
                                      int timeout, 
                                      HeartbeatListener heartbeatEvent) 
        throws IOException 
    {  
        Guard.check(heartbeatAddress, timeout, heartbeatEvent);
        log_.debug(String.format("Starting heartbeat listener on the group: %s, port: %d, and timeout: %d",
                                 heartbeatAddress.getAddress(), heartbeatAddress.getPort(), timeout));
        timeout_ = timeout;
        heartbeatEvent_ = heartbeatEvent;
        isTerminated_ = false;
        joinMulticastGroup(heartbeatAddress);
    }
    
    /** 
     * Listen for group leader heartbeat multicast packets.
     */
    public void run() 
    {
        log_.debug("Heartbeat multicast listener waiting for packets");           
        while (!isTerminated_) 
        {      
            try
            {    
                byte[] byteArray = receive();
                HeartbeatMessage heartbeat = (HeartbeatMessage) SerializationUtils.deserializeObject(byteArray);
                heartbeatEvent_.onHeartbeatArrival(heartbeat); 
            }
            catch (IOException exception) 
            {
                if (!isTerminated_)
                {
                    heartbeatEvent_.onHeartbeatFailure();
                }
            } 
            catch (ClassNotFoundException exception) 
            {
                log_.error(String.format("Class not found exception: %s", exception.getMessage()));
            } 
        }
        log_.debug("Heartbeat multicast listened is stopped!");
    }

    /** 
     * Join a multicast group.
     * 
     * @param heartbeatAddress  The heartbeat address
     * @throws IOException      The I/O exception
     */
    private void joinMulticastGroup(NetworkAddress heartbeatAddress)   
        throws IOException
    {        
        log_.debug(String.format("Joining multicast group: %d", heartbeatAddress.getPort()));
        mcastSocket_ = new MulticastSocket(heartbeatAddress.getPort());
        mcastSocket_.setSoTimeout(timeout_); 
        InetAddress address = InetAddress.getByName(heartbeatAddress.getAddress());     
        mcastSocket_.joinGroup(address);  
    }
    
    /** 
     * Listen for multicast packets.
     *  
     * @return                  The byte array
     * @throws IOException      The I/O exception
     */
    private byte[] receive() 
        throws IOException 
    {
        byte[] buffer = new byte[BUFF_SIZE];            
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);      
        mcastSocket_.receive(packet);   
        packet.setLength(buffer.length);
        return buffer;
    }
    
    /**
     * Closes the socket.
     */
    private void close()
    {
        if (mcastSocket_ != null)
        {
            mcastSocket_.close();
        }
    }
    
    /**
     * Terminates the listener.
     */
    public synchronized void terminate() 
    {
        log_.debug("Terminating the heartbeat multicast listener");
        isTerminated_ = true;
        close();
    }
}
