package org.inria.myriads.snoozenode.monitoring.datasender.api;

import java.io.IOException;

/**
 * 
 * Data Sender.
 * 
 * @author msimonin
 *
 */
public interface DataSender 
{
    /**
     * 
     * Sends data.
     * 
     * @param data              The data to send
     * @throws IOException      Exception
     */
    void send(Object data) throws IOException;
 
    /**
     * 
     * Sends data.
     * 
     * @param data              The data to send
     * @param senderId          The sender Id
     * @throws IOException      Exception
     */
    void send(Object data, String senderId) throws IOException;
    
    /**
     * Closes the sender.
     */
    void close();
    
}