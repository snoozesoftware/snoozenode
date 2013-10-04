package org.inria.myriads.snoozenode.comunicator.api.impl;

import java.io.IOException;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozenode.comunicator.api.Communicator;
import org.inria.myriads.snoozenode.monitoring.datasender.api.impl.TCPDataSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Memory Communicator.
 * 
 * @author msimonin
 *
 */
public class MemoryCommunicator implements Communicator
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupManagerCassandraCommunicator.class);
    
    /** Internal sender.*/
    private TCPDataSender sender_;

    /**
     * 
     * Constructor.
     * 
     * @param groupLeaderAddress        The group Leader address.  
     * @throws IOException              Exception.
     */
    public MemoryCommunicator(NetworkAddress groupLeaderAddress) throws IOException
    {
        sender_ = new TCPDataSender(groupLeaderAddress);
        log_.debug("Memory Communicator initialized");
    }

    @Override
    public void sendRegularData(Object data) throws IOException
    {
       log_.debug("Sending regular data");
       sender_.send(data);
    }

    @Override
    public void sendHeartbeatData(Object data) throws IOException
    {
        log_.debug("Sending heartbeat data");
        sender_.send(data);
    }

    @Override
    public void close()
    {
       log_.debug("Closing the communicator");
       sender_.close();
    }

}
