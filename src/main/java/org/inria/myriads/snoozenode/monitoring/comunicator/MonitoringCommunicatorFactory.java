package org.inria.myriads.snoozenode.monitoring.comunicator;

import java.io.IOException;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozenode.configurator.database.DatabaseSettings;
import org.inria.myriads.snoozenode.database.enums.DatabaseType;
import org.inria.myriads.snoozenode.monitoring.comunicator.api.MonitoringCommunicator;
import org.inria.myriads.snoozenode.monitoring.comunicator.api.impl.GroupManagerCassandraCommunicator;
import org.inria.myriads.snoozenode.monitoring.comunicator.api.impl.LocalControllerMemoryCommunicator;
import org.inria.myriads.snoozenode.monitoring.comunicator.api.impl.LocalControllerCassandraCommunicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Communicator factory.
 * 
 * @author msimonin
 *
 */
public final class MonitoringCommunicatorFactory
{
    /** Logging instance. */
    private static final Logger log_ = LoggerFactory.getLogger(MonitoringCommunicatorFactory.class);
    
    /** Hide constructor. */
    private MonitoringCommunicatorFactory()
    {
        throw new UnsupportedOperationException();
    }
    
    
    /**
     * 
     * Build a communicator.
     * 
     * @param groupLeader   node address
     * @param databaseSettings 
     * @return  DataSender
     * @throws IOException  Exception
     */
    public static MonitoringCommunicator newGroupManagerCommunicator(
            NetworkAddress groupLeader,
            DatabaseSettings databaseSettings
            ) throws IOException
    {
        DatabaseType database = databaseSettings.getType();
        switch(database)
        {
            case memory:
                return new LocalControllerMemoryCommunicator(groupLeader);
            case cassandra:
                return new GroupManagerCassandraCommunicator(groupLeader, databaseSettings);
            default:
                return new LocalControllerMemoryCommunicator(groupLeader);
        }
        
    }
    
    /**
     * 
     * Returns a new Virtual Machine Communicator.
     * 
     * @param groupManagerAddress   The groupManager address.
     * @param databaseSettings      The database Settings
     * @return  a data sender
     * @throws IOException          Exception
     */
    public synchronized static MonitoringCommunicator newVirtualMachineCommunicator(NetworkAddress groupManagerAddress,
            DatabaseSettings databaseSettings) throws IOException
    {
        
       
        DatabaseType database = databaseSettings.getType();
        switch(database)
        {
            case memory:
                log_.debug("Creating a new virtual machine memory communicator instance");
                return new LocalControllerMemoryCommunicator(groupManagerAddress);
                
            case cassandra:
                log_.debug("Creating a new virtual machine cassandra communicator instance");
                return new LocalControllerCassandraCommunicator(groupManagerAddress, databaseSettings);
                
            default:
                log_.debug("Creating a new virtual machine default communicator instance");
                return new LocalControllerMemoryCommunicator(groupManagerAddress);
        }
        

    }

}
