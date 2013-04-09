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
public class RabbitMQDataSender implements DataSender 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(RabbitMQDataSender.class);
    
    /** Server address address. */
    private NetworkAddress server_;

    private Channel channel_;
    
    private Connection connection_;
    
    private String exchangeName_;
    
    private String routingKey_;
    
    /**
     * TCP data sender consturctor.
     * 
     * @param networkAddress          The network address
     * @throws IOException            The I/O exception
     */
    public RabbitMQDataSender(String exchangeName, String routingKey) 
        throws IOException 
    {
        
        log_.debug(String.format("Initializing the RabbitMQ data sender"));
        
        exchangeName_ = exchangeName;
        routingKey_ = routingKey;
        ConnectionFactory factory = new ConnectionFactory();
        //hard coded
        factory.setHost("localhost");
        factory.setUsername("snooze");
        factory.setPassword("snooze");
        factory.setVirtualHost("snooze-vhost");
        connection_ = factory.newConnection();
        channel_ = connection_.createChannel();
        channel_.exchangeDeclare(exchangeName_, "topic");
    }           

    
    public RabbitMQDataSender(String exchangeName) throws IOException
    {
        this(exchangeName,"0");
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
        Guard.check(data);
        send(data, routingKey_);
    }
    
    /**
     * Closes the sender.
     */
    public void close() 
    {
        log_.debug("Closing the connection to rabbit mq ");
        if (connection_ != null)
        {
            try
            {
                connection_.close();
            }
            catch (Exception e)
            {
                log_.error(e.getMessage());
                //e.printStackTrace();
            }
        }
        
    }

    @Override
    public void send(Object data, String routingKey) throws IOException
    {
        Guard.check(data, routingKey);
        String message =  SerializationUtils.serializeObjectToJSON(data);
        log_.debug(String.format("Sending on the exchange %s with routing key %s", exchangeName_, routingKey));
        channel_.basicPublish(exchangeName_, routingKey, null, message.getBytes());
        
    }

}