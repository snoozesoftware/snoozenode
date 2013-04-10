package org.inria.myriads.snoozenode.monitoring.datasender.api.impl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.monitoring.datasender.api.DataSender;
import org.inria.myriads.snoozenode.util.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;


/**
 * 
 * UDP Data Sender.
 * 
 * @author msimonin
 *
 */
public class DevNullDataSender implements DataSender 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(DevNullDataSender.class);
    
    
    /**
     * TCP data sender consturctor.
     * 
     * @param networkAddress          The network address
     * @throws IOException            The I/O exception
     */
    public DevNullDataSender() 
        throws IOException 
    {
        
        log_.debug(String.format("Initializing the dev null data sender"));

    }           

   
    /** 
     * Main routine to send data.
     *  
     * @param data          The data object
     * @throws IOException  The I/O exception
     */
    public void send(Object data)
        throws IOException 
    {
   
    }
    
    /**
     * Closes the sender.
     */
    public void close() 
    {
        log_.debug("Closing the connection to dev/null ");        
    }

    @Override
    public void send(Object data, String senderId) throws IOException
    {
        Guard.check(data, senderId);
        
    }

}