package org.inria.myriads.snoozenode.comunicator;

import java.io.IOException;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozenode.comunicator.api.Communicator;
import org.inria.myriads.snoozenode.comunicator.api.impl.GroupManagerCassandraCommunicator;
import org.inria.myriads.snoozenode.comunicator.api.impl.MemoryCommunicator;
import org.inria.myriads.snoozenode.comunicator.api.impl.VirtualMachineCassandraCommunicator;
import org.inria.myriads.snoozenode.configurator.database.DatabaseSettings;
import org.inria.myriads.snoozenode.database.enums.DatabaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Communicator factory.
 * 
 * @author msimonin
 *
 */
public final class CommunicatorFactory
{
    /** Logging instance. */
    private static final Logger log_ = LoggerFactory.getLogger(Communicator.class);
    
    /** Hide constructor. */
    private CommunicatorFactory()
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
    public static Communicator newGroupManagerCommunicator(
            NetworkAddress groupLeader,
            DatabaseSettings databaseSettings
            ) throws IOException
    {
        DatabaseType database = databaseSettings.getType();
        switch(database)
        {
            case memory:
                return new MemoryCommunicator(groupLeader);
            case cassandra:
                return new GroupManagerCassandraCommunicator(groupLeader, databaseSettings);
            default:
                return new MemoryCommunicator(groupLeader);
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
    public static Communicator newVirtualMachineCommunicator(NetworkAddress groupManagerAddress,
            DatabaseSettings databaseSettings) throws IOException
    {
        DatabaseType database = databaseSettings.getType();
        switch(database)
        {
            case memory:
                return new MemoryCommunicator(groupManagerAddress);
            case cassandra:
                return new VirtualMachineCassandraCommunicator(groupManagerAddress, databaseSettings);
            default:
                return new MemoryCommunicator(groupManagerAddress);
        }
    }
}
