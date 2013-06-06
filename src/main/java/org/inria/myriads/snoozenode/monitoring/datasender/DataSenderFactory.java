package org.inria.myriads.snoozenode.monitoring.datasender;

import java.io.IOException;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozenode.configurator.monitoring.external.ExternalNotifierSettings;
import org.inria.myriads.snoozenode.monitoring.TransportProtocol;
import org.inria.myriads.snoozenode.monitoring.datasender.api.DataSender;
import org.inria.myriads.snoozenode.monitoring.datasender.api.impl.RabbitMQExternalSender;
import org.inria.myriads.snoozenode.monitoring.datasender.api.impl.TCPDataSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author msimonin
 *
 */
public class DataSenderFactory
{
    /** Logging instance. */
    private static final Logger log_ = LoggerFactory.getLogger(DataSenderFactory.class);
    
    /**
     * Constructor.
     */
    public DataSenderFactory()
    {
        throw new UnsupportedOperationException();
    }
    
    
    public static DataSender newInternalDataSender(NetworkAddress nodeAddress) throws IOException
    {
        return new TCPDataSender(nodeAddress);
    }
    
    
    /**
     * 
     * Create external data sender.
     * 
     * @param externalNotifierSettings
     * @return
     */
    public static DataSender newExternalDataSender(String exchange, ExternalNotifierSettings externalNotifierSettings)
    {
        TransportProtocol transport = externalNotifierSettings.getTransportProtocol();
        switch(transport)
        {
            case RABBITMQ :
                log_.debug("Initializing the RabbitMQ external sender");
                return new RabbitMQExternalSender(exchange, externalNotifierSettings);
             default : 
                return null;
        }
    }
    
}
