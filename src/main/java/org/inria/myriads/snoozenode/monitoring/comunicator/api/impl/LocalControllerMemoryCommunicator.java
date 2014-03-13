package org.inria.myriads.snoozenode.monitoring.comunicator.api.impl;

import java.io.IOException;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozenode.monitoring.comunicator.api.MonitoringCommunicator;
import org.inria.myriads.snoozenode.monitoring.datasender.api.impl.TCPDataSender;
import org.inria.myriads.snoozenode.util.OutputUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Memory Communicator.
 * 
 * @author msimonin
 *
 */
public class LocalControllerMemoryCommunicator implements MonitoringCommunicator
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LocalControllerMemoryCommunicator.class);
    
    /** Internal sender.*/
    private TCPDataSender sender_;

    
    /**
     * 
     * Constructor.
     * 
     * @param groupLeaderAddress        The group Leader address.  
     * @throws IOException              Exception.
     */
    public LocalControllerMemoryCommunicator(NetworkAddress groupLeaderAddress) throws IOException
    {
        sender_ = new TCPDataSender(groupLeaderAddress);
        log_.debug("Memory Communicator initialized to " + groupLeaderAddress);
    }

    
    @Override
    public void sendRegularData(Object data) throws IOException
    {
       log_.debug("Sending regular data");
       OutputUtils.dump(data);
       sender_.send(data);
    }

    @Override
    public void sendHeartbeatData(Object data) throws IOException
    {
        log_.debug("Sending heartbeat data");
        sender_.send(data);
    }

    @Override
    public void sendAnomalyData(Object data) throws IOException
    {
        log_.debug("sending anomaly data");
        sender_.send(data);
    }
    
    @Override
    public void close()
    {
       log_.debug("Closing the communicator");
       sender_.close();
    }




}
