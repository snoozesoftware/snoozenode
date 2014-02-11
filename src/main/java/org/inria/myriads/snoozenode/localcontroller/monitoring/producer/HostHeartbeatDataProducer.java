package org.inria.myriads.snoozenode.localcontroller.monitoring.producer;

import java.util.concurrent.BlockingQueue;

import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.AggregatedHostMonitoringData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostHeartbeatDataProducer implements Runnable
{

    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(HostHeartbeatDataProducer.class);
    
    
    /** Data queue reference. */
    private BlockingQueue<AggregatedHostMonitoringData> dataQueue_;

    /** Monitoring interval. */
    private int monitoringInterval_;

    /** Lock Object.*/
    private Object lockObject_;

    /** Heartbeat message.*/
    private AggregatedHostMonitoringData heartbeatMessage_;

    /** isTerminated. */
    private boolean isTerminated_;
    
    public HostHeartbeatDataProducer(
            String localControllerId,
            int monitoringInterval,
            BlockingQueue<AggregatedHostMonitoringData> dataQueue)
    {
        Guard.check(localControllerId, dataQueue);
        log_.debug("Initializing the host heartbeat producer");
        monitoringInterval_ = monitoringInterval;
        dataQueue_ = dataQueue;
        heartbeatMessage_ = new AggregatedHostMonitoringData("heartbeat", null);
        lockObject_ = new Object();
    }

    @Override
    public void run()
    {
        try
        {
            while (!isTerminated_)
            {                    
                log_.debug("Host heartbeat data to the queue");
                dataQueue_.add(heartbeatMessage_);
                
                synchronized (lockObject_)
                {
                    lockObject_.wait(monitoringInterval_);
                }
            }            
        }
        catch (InterruptedException exception) 
        {
            log_.error(String.format("Host heartbeat data producer was interruped: %s", 
                                      exception.getMessage()));
        }
        
        log_.debug("Host heartbeat producer is stopped!");
        
    }

    /**
     * Terminates the thread.
     */
    public void terminate()
    {
        log_.debug("Terminating the host heartbeat producer");
        synchronized (lockObject_)
        {
            isTerminated_ = true;
            lockObject_.notify();
        }
        
    }

}
