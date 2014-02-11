package org.inria.myriads.snoozenode.monitoring.comunicator.api;

import java.io.IOException;

/**
 * 
 * Communicator.
 * 
 * @author msimonin
 *
 */
public interface MonitoringCommunicator
{

    /**
     * 
     * Sends Regular data.
     * 
     * @param data              The data to send.    
     * @throws IOException      Exception
     */
    void sendRegularData(Object data) throws IOException;
    
    
    /**
     * 
     * Sends Heartbeat Data.
     *      
     * @param data              The data to send as heartbeat.
     * @throws IOException      Exception.
     */
    void sendHeartbeatData(Object data) throws IOException;
    
    
    /**
     * Closes the communicator.
     */
    void close();
    
    
}
