package org.inria.myriads.snoozenode.monitoring.datasender.api.impl;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.inria.myriads.snoozenode.configurator.monitoring.external.MonitoringExternalSettings;
import org.inria.myriads.snoozenode.monitoring.datasender.api.DataSender;
import org.inria.myriads.snoozenode.util.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQExternalSender implements DataSender
{
    
    /** Logging instance. */
    private static final Logger log_ = LoggerFactory.getLogger(RabbitMQExternalSender.class);
    
    /** Connection factory. */
    private ConnectionFactory factory = new ConnectionFactory();
    
    /** The connection.*/
    private Connection connection_ = null;
    
    /** The channel.*/
    private Channel channel_ = null;
    
    /***/
    private String identifier_ = null;
    
    /** Rabbitmq host. */
    private String host_ = "localhost";
    
    /** Rabbitmq port. */
    private int port_ = 5762;
    
    /** RabbitMq username. */
    private String username_ = "guest";
    
    /** RabbitMq Password. */
    private String password_ = "guest";
    
    /** Virtual Host.*/
    private String virtualHost_ = "/";
    
    /** Exchange name. */
    private String exchange_ = "amqp-exchange";
    
    /** Queue name.*/
    private String queue_ = "amqp-queue";
    
    /** Exchange type. */
    private String type_ = "topic";
    
    /** Durable option.*/ 
    private boolean durable_ = false;
    
    /** Default routing key to use. */
    private String routingKey_ = "";

    /** Thread pool for sending.*/
    private ExecutorService threadPool = Executors.newSingleThreadExecutor();
    
    /**
     * 
     * Constructor.
     * 
     * @param monitoringExternalSettings
     */
    public RabbitMQExternalSender(MonitoringExternalSettings monitoringExternalSettings)
    {
        this("amqp-exchange", monitoringExternalSettings);
    }

    /**
     * 
     * Constructor.
     * 
     * @param exchange
     * @param monitoringExternalSettings
     */
    public RabbitMQExternalSender(String exchange, MonitoringExternalSettings monitoringExternalSettings)
    {
        host_ = monitoringExternalSettings.getAddress().getAddress();
        port_ = monitoringExternalSettings.getAddress().getPort();
        username_ = monitoringExternalSettings.getUsername(); 
        password_ = monitoringExternalSettings.getPassword(); 
        virtualHost_ = monitoringExternalSettings.getVhost();        
        exchange_ = exchange;
        activateOptions();
    }

    public void activateOptions() 
    {
        //== creating connection
        try 
        {
            this.createConnection();
        } 
        catch (IOException ioe) 
        {
            log_.error("Unable to create the connection " + ioe.getMessage());
        }

        //== creating channel
        try 
        {
            this.createChannel();
        } 
        catch (IOException ioe) 
        {
            log_.error("Unable to create the channel " + ioe.getMessage());
        }
        
        //== create exchange
        try 
        {
            this.createExchange();
        }
        catch (Exception ioe) 
        {
            log_.error("Unable to create the exchange " + ioe.getMessage());
            ioe.printStackTrace();
        }

//        // create queue
//        try 
//        {
//            this.createQueue();
//        } 
//        catch (Exception ioe) 
//        {
//            log_.error("Unable to create the queue " +  ioe.getMessage());
//        }
    }

    
    /**
     * Sets the ConnectionFactory parameters
     */
    private void setFactoryConfiguration() {
        factory.setHost(this.host_);
        factory.setPort(this.port_);
        factory.setVirtualHost(this.virtualHost_);
        factory.setUsername(this.username_);
        factory.setPassword(this.password_);
    }
    
    /**
     * Creates connection to RabbitMQ server according to properties
     * @return
     * @throws IOException
     */
    private Connection createConnection() throws IOException 
    {
        setFactoryConfiguration();
        if (this.connection_ == null || !this.connection_.isOpen()) 
        {
            this.connection_ = factory.newConnection();
        }

        return this.connection_;
    }
    
    /**
     * Creates channel on RabbitMQ server
     * @return
     * @throws IOException
     */
    private Channel createChannel() throws IOException 
    {
        if (this.connection_ != null) 
        {
            if (this.channel_ == null || !this.channel_.isOpen() && (this.connection_ != null && this.connection_.isOpen()) ) 
            {
                this.channel_ = this.connection_.createChannel();
            }
        }
        return this.channel_;
    }



    
    
    /**
     * Declares the exchange on RabbitMQ server according to properties set
     * @throws IOException
     */
    private void createExchange() throws IOException 
    {
        if (this.channel_ != null && this.channel_.isOpen()) 
        {
            synchronized (this.channel_) 
            {
                this.channel_.exchangeDeclare(this.exchange_, this.type_, this.durable_);
            }
        }
    }


//    /**
//     * Declares and binds queue on rabbitMQ server according to properties
//     * @throws IOException
//     */
//    private void createQueue() throws IOException 
//    {
//        if (this.channel_ != null && this.channel_.isOpen()) 
//        {
//            synchronized (this.channel_) 
//            {
//                this.channel_.queueDeclare(this.queue_, false, false, false, null);
//                this.channel_.queueBind(this.queue_, this.exchange_, this.routingKey_);
//            }
//        }
//    }

    /**
     * Closes the channel and connection to RabbitMQ when shutting down the appender
     */
    @Override
    public void close() {
        if (channel_ != null && channel_.isOpen()) {
            try {
                channel_.close();
            } catch (IOException ioe) {
                
            }
        }

        if (connection_ != null && connection_.isOpen()) {
            try {
                this.connection_.close();
            } catch (IOException ioe) {
                
            }
        }
    }
    
    
    

    @Override
    public void send(Object data) 
    {
        
    }

    @Override
    public void send(Object data, String routingKey) 
    {
        setRoutingKey(routingKey);
        threadPool.submit( new SenderTask(data) );
    }


    /**
     * @return the factory
     */
    public ConnectionFactory getFactory()
    {
        return factory;
    }


    /**
     * @param factory the factory to set
     */
    public void setFactory(ConnectionFactory factory)
    {
        this.factory = factory;
    }


    /**
     * @return the connection
     */
    public Connection getConnection()
    {
        return connection_;
    }


    /**
     * @param connection the connection to set
     */
    public void setConnection(Connection connection)
    {
        this.connection_ = connection;
    }


    /**
     * @return the channel
     */
    public Channel getChannel()
    {
        return channel_;
    }


    /**
     * @param channel the channel to set
     */
    public void setChannel(Channel channel)
    {
        this.channel_ = channel;
    }


    /**
     * @return the identifier
     */
    public String getIdentifier()
    {
        return identifier_;
    }


    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier)
    {
        this.identifier_ = identifier;
    }


    /**
     * @return the host
     */
    public String getHost()
    {
        return host_;
    }


    /**
     * @param host the host to set
     */
    public void setHost(String host)
    {
        this.host_ = host;
    }


    /**
     * @return the port
     */
    public int getPort()
    {
        return port_;
    }


    /**
     * @param port the port to set
     */
    public void setPort(int port)
    {
        this.port_ = port;
    }


    /**
     * @return the username
     */
    public String getUsername()
    {
        return username_;
    }


    /**
     * @param username the username to set
     */
    public void setUsername(String username)
    {
        this.username_ = username;
    }


    /**
     * @return the password
     */
    public String getPassword()
    {
        return password_;
    }


    /**
     * @param password the password to set
     */
    public void setPassword(String password)
    {
        this.password_ = password;
    }


    /**
     * @return the virtualHost
     */
    public String getVirtualHost()
    {
        return virtualHost_;
    }


    /**
     * @param virtualHost the virtualHost to set
     */
    public void setVirtualHost(String virtualHost)
    {
        this.virtualHost_ = virtualHost;
    }


    /**
     * @return the exchange
     */
    public String getExchange()
    {
        return exchange_;
    }


    /**
     * @param exchange the exchange to set
     */
    public void setExchange(String exchange)
    {
        this.exchange_ = exchange;
    }


    /**
     * @return the queue
     */
    public String getQueue()
    {
        return queue_;
    }


    /**
     * @param queue the queue to set
     */
    public void setQueue(String queue)
    {
        this.queue_ = queue;
    }


    /**
     * @return the type
     */
    public String getType()
    {
        return type_;
    }


    /**
     * @param type the type to set
     */
    public void setType(String type)
    {
        this.type_ = type;
    }


    /**
     * @return the durable
     */
    public boolean isDurable()
    {
        return durable_;
    }


    /**
     * @param durable the durable to set
     */
    public void setDurable(boolean durable)
    {
        this.durable_ = durable;
    }


    /**
     * @return the routingKey
     */
    public String getRoutingKey()
    {
        return routingKey_;
    }


    /**
     * @param routingKey the routingKey to set
     */
    public void setRoutingKey(String routingKey)
    {
        this.routingKey_ = routingKey;
    }

    
    /**
     * Simple Callable class that publishes messages to RabbitMQ server
     */
    class SenderTask implements Callable<Object> 
    {

        /** Event message to send */
        Object eventMessage__;

        /**
         * 
         * Constructor.
         * 
         * @param data
         */
        SenderTask(Object data) 
        {
            eventMessage__ = data;
        }

        /**
         * 
         * Method is called by ExecutorService and publishes message on RabbitMQ
         * 
         * @return
         * @throws Exception
         */
        @Override
        public Object call() throws Exception 
        {
            
//            AMQP.BasicProperties.Builder b = new AMQP.BasicProperties().builder();
//            b.appId(identifier)
//                    .type(loggingEvent.getLevel().toString())
//                    .correlationId(id)
//                    .contentType("text/json");
            
            activateOptions();
               
            if (channel_ != null && channel_.isOpen()) 
            {                    
                String message =  SerializationUtils.serializeObjectToJSON(eventMessage__);
                channel_.basicPublish(exchange_, routingKey_, null, message.getBytes());
            }

            return eventMessage__;
        }
    }
    
}
