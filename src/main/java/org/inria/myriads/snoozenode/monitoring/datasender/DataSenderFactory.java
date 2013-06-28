package org.inria.myriads.snoozenode.monitoring.datasender;

import java.io.IOException;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozenode.configurator.monitoring.external.ExternalNotifierSettings;
import org.inria.myriads.snoozenode.monitoring.TransportProtocol;
import org.inria.myriads.snoozenode.monitoring.datasender.api.DataSender;
import org.inria.myriads.snoozenode.monitoring.datasender.api.impl.RabbitMQExternalSender;
import org.inria.myriads.snoozenode.monitoring.datasender.api.impl.TCPDataSender;
import org.inria.myriads.snoozenode.monitoring.datasender.api.impl.TestExternalSender;
import org.inria.snoozenode.external.notifier.ExternalNotificationType;
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
    
    
    public static DataSender newExternalDataSender(ExternalNotificationType externalNotificationType,
            ExternalNotifierSettings externalNotifierSettings)
    {
        TransportProtocol transport = externalNotifierSettings.getTransportProtocol();
        switch(transport)
        {
            case RABBITMQ :
                log_.debug("Initializing the RabbitMQ external sender");
                return new RabbitMQExternalSender(externalNotificationType.toString(), externalNotifierSettings);
            case TEST:
                log_.debug("Initializing the TEST external sender");
                return new TestExternalSender();
            default : 
                return null;
        }
    }
    
}
