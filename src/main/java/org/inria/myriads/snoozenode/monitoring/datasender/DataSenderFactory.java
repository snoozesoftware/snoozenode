package org.inria.myriads.snoozenode.monitoring.datasender;

import org.inria.myriads.snoozenode.configurator.monitoring.external.ExternalNotifierSettings;
import org.inria.myriads.snoozenode.monitoring.TransportProtocol;
import org.inria.myriads.snoozenode.monitoring.datasender.api.DataSender;
import org.inria.myriads.snoozenode.monitoring.datasender.api.impl.RabbitMQExternalSender;
import org.inria.snoozenode.external.notifier.ExternalNotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Data sender factory class.
 * 
 * @author msimonin
 *
 */
public final class DataSenderFactory
{
    /** Logging instance. */
    private static final Logger log_ = LoggerFactory.getLogger(DataSenderFactory.class);
    
    /** Hide constructor. */
    private DataSenderFactory()
    {
        throw new UnsupportedOperationException();
    }
    
    
    

    /**
     * 
     * Build an external data sender.
     * 
     * @param externalNotificationType      Type.
     * @param externalNotifierSettings      Settings.
     * @return  dataSender
     */
    public static DataSender newExternalDataSender(ExternalNotificationType externalNotificationType,
            ExternalNotifierSettings externalNotifierSettings)
    {
        TransportProtocol transport = externalNotifierSettings.getTransportProtocol();
        switch(transport)
        {
            case RABBITMQ :
                log_.debug("Initializing the RabbitMQ external sender");
                return new RabbitMQExternalSender(externalNotificationType.toString(), externalNotifierSettings);
            default : 
                return null;
        }
    }



    
}
