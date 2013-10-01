package org.inria.myriads.snoozenode.monitoring.datasender;

import java.io.IOException;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozenode.configurator.database.DatabaseSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.external.ExternalNotifierSettings;
import org.inria.myriads.snoozenode.database.enums.DatabaseType;
import org.inria.myriads.snoozenode.monitoring.TransportProtocol;
import org.inria.myriads.snoozenode.monitoring.datasender.api.DataSender;
import org.inria.myriads.snoozenode.monitoring.datasender.api.impl.CassandraGroupManagerDataSender;
import org.inria.myriads.snoozenode.monitoring.datasender.api.impl.CassandraVirtualMachineDataSender;
import org.inria.myriads.snoozenode.monitoring.datasender.api.impl.RabbitMQExternalSender;
import org.inria.myriads.snoozenode.monitoring.datasender.api.impl.TCPDataSender;
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
    public DataSenderFactory()
    {
        throw new UnsupportedOperationException();
    }
    
    
    /**
     * 
     * Build a monitoring dataSender.
     * 
     * @param nodeAddress   node address
     * @param databaseSettings 
     * @return  DataSender
     * @throws IOException  Exception
     */
    public static DataSender newGroupManagerMonitoringDataSender(
            NetworkAddress nodeAddress,
            DatabaseSettings databaseSettings
            ) throws IOException
    {
        DatabaseType database = databaseSettings.getType();
        switch(database)
        {
            case memory:
                return new TCPDataSender(nodeAddress);
            case cassandra:
                return new CassandraGroupManagerDataSender(databaseSettings);
            default:
                return new TCPDataSender(nodeAddress);
        }
        
    }
    
    /**
     * 
     * Build an internal dataSender.
     * 
     * @param nodeAddress   node address
     * @return  DataSender
     * @throws IOException  Exception
     */
    public static DataSender newHeartbeatSender(NetworkAddress nodeAddress) throws IOException
    {
        return new TCPDataSender(nodeAddress);
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


    /**
     * 
     * Returns a new Virtual Machine Monitoring Sender.
     * 
     * @param groupManagerAddress   The groupManager address.
     * @param databaseSettings      The database Settings
     * @return  a data sender
     * @throws IOException          Exception
     */
    public static DataSender newVirtualMachineMonitoringSender(NetworkAddress groupManagerAddress,
            DatabaseSettings databaseSettings) throws IOException
    {
        DatabaseType database = databaseSettings.getType();
        switch(database)
        {
            case memory:
                return new TCPDataSender(groupManagerAddress);
            case cassandra:
                return new CassandraVirtualMachineDataSender(databaseSettings);
            default:
                return new TCPDataSender(groupManagerAddress);
        }
    }
    
}
