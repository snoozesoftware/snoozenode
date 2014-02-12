package org.inria.myriads.snoozenode.monitoring.comunicator.api.impl;

import java.io.IOException;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozenode.configurator.database.DatabaseSettings;
import org.inria.myriads.snoozenode.monitoring.comunicator.api.MonitoringCommunicator;
import org.inria.myriads.snoozenode.monitoring.datasender.api.DataSender;
import org.inria.myriads.snoozenode.monitoring.datasender.api.impl.CassandraGroupManagerDataSender;
import org.inria.myriads.snoozenode.monitoring.datasender.api.impl.TCPDataSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Group Manager Cassandra Communicator.
 * 
 * @author msimonin
 *
 */
public class GroupManagerCassandraCommunicator implements MonitoringCommunicator
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupManagerCassandraCommunicator.class);
    
    /** Heartbeat sender.*/
    private DataSender heartbeatSender_;
    
    /** Monitoring sender.*/
    private DataSender monitoringSender_;
    
    /**
     * 
     * Constructor.
     * 
     * @param groupLeaderAddress    The groupLeader address.
     * @param databaseSettings      The database settings.
     * @throws IOException          Connection Exception.
     */
    public GroupManagerCassandraCommunicator(NetworkAddress groupLeaderAddress, DatabaseSettings databaseSettings)
            throws IOException
    {
        heartbeatSender_ = new TCPDataSender(groupLeaderAddress);
        monitoringSender_ = new CassandraGroupManagerDataSender(databaseSettings);
        log_.debug("GroupManagerCassandraCommunicator initialized");
    }
    
    @Override
    public void sendRegularData(Object data) throws IOException
    {
        log_.debug("Seding regular data");
        monitoringSender_.send(data);
    }

    @Override
    public void sendHeartbeatData(Object data) throws IOException
    {
        log_.debug("Seding heartbeat data");
        heartbeatSender_.send(data);
    }

    @Override
    public void sendAnomalyData(Object data) throws IOException
    {
        log_.debug("Sending anomaly data");
        log_.debug("Anomaly sender not implemented.");
    }
    
    @Override
    public void close()
    {
        log_.debug("Closing the communicator");
        monitoringSender_.close();
        heartbeatSender_.close();
    }

}
