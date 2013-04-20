package org.inria.myriads.snoozenode.utils;

import java.io.IOException;

import org.inria.myriad.snoozenode.eventmessage.EventMessage;
import org.inria.myriads.snoozenode.monitoring.datasender.api.DataSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventUtils
{
    
    
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(EventUtils.class);
    
    /**
     * Hide the consturctor.
     */
    private EventUtils() 
    {
        throw new UnsupportedOperationException();
    }
    
    public static void send(DataSender sender, EventMessage message)
    {
        try
        {
            sender.send(message);
        }
        catch (Exception e)
        {
            log_.error("Failed to send external datas" + e.getMessage());
        } 
    }
}
