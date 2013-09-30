package org.inria.myriads.snoozenode.groupmanager.monitoring.consumer;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.database.DatabaseSettings;
import org.inria.myriads.snoozenode.groupmanager.monitoring.transport.GroupManagerDataTransporter;
import org.inria.myriads.snoozenode.monitoring.datasender.DataSenderFactory;
import org.inria.myriads.snoozenode.monitoring.datasender.api.DataSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Group Manager Monitoring Data Consumer.
 * 
 * @author msimonin
 *
 */
public class GroupManagerMonitoringDataConsumer implements Runnable
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupManagerMonitoringDataConsumer.class);
  
    /** Group manager id. */
    private String groupManagerId_;
    
    /** Data queue. */
    private BlockingQueue<GroupManagerDataTransporter> dataQueue_;

    /** Signals termination. */
    private boolean isTerminated_;
    
    /** heartBeat Sender.*/
    private DataSender heartbeatSender_;
    
    /**monitoring Sender.*/
    //private DataSender monitoringSender_;
    private DataSender monitoringSender_;
     
    
    
    /**
     * 
     * Constructor.
     * 
     * @param groupManagerId        The group Manager Id.
     * @param groupLeaderAddress    The group Leader address.
     * @param databaseSettings      The databse Settings.
     * @param dataQueue             The data queue.
     * @throws IOException      IOException
     */
    public GroupManagerMonitoringDataConsumer(
            String groupManagerId, 
            NetworkAddress groupLeaderAddress,
            DatabaseSettings databaseSettings,
            BlockingQueue<GroupManagerDataTransporter> dataQueue 
            ) throws IOException
    {
        Guard.check(groupManagerId, databaseSettings);
        groupManagerId_ = groupManagerId;
        dataQueue_ = dataQueue;
        isTerminated_ = false;
        heartbeatSender_ = DataSenderFactory.newHeartbeatSender(groupLeaderAddress);
        monitoringSender_ = DataSenderFactory.newGroupManagerMonitoringDataSender(groupLeaderAddress, databaseSettings);
    }

    @Override
    public void run()
    {
        
        try
        {
            while (!isTerminated_)
            {                               
                log_.debug("Waiting for group manager monitoring data to arrive...");
                GroupManagerDataTransporter groupManagerData = dataQueue_.take();   

                if (groupManagerData.getSummary() == null)
                {
                    
                    sendHeartbeatData(groupManagerId_);
                    continue;
                }
                   
                sendRegularData(groupManagerId_, groupManagerData);
            }
        } 
        catch (InterruptedException exception)
        {
            log_.error("group manager monitoring data consumer thread was interruped", exception);
        }  
        
        log_.debug("Group monitoring data consumer stopped!");
        terminate();
        
    }
    
    /**
     * 
     * Sends regular data (monitoring data).
     * 
     * @param groupManagerId            The group manager id.
     * @param groupManagerData          The monitoring data.
     * @throws InterruptedException     Exception
     */
    private void sendRegularData(
            String groupManagerId,
            GroupManagerDataTransporter groupManagerData
            ) throws InterruptedException
    {
        try
        {
            monitoringSender_.send(groupManagerData);    
        }
        catch (Exception exception)
        {
            log_.debug(String.format("I/O error during data sending (%s)! Did the group manager close " +
                    "its connection unexpectedly?", exception.getMessage()));
            throw new InterruptedException();
        }
    }

    /**
     * 
     * Sends heartbeat datas.
     * 
     * @param groupManagerId            The group manager id.
     * @throws InterruptedException     Exception
     */
    private void sendHeartbeatData(String groupManagerId) throws InterruptedException
    {
        GroupManagerDataTransporter data = new GroupManagerDataTransporter(groupManagerId, null);
        try
        {
            heartbeatSender_.send(data);
        }
        catch (Exception exception)
        {
            log_.debug(String.format("I/O error during data sending (%s)! Did the group manager close " +
                    "its connection unexpectedly?", exception.getMessage()));
            throw new InterruptedException();
        }
        
    }

    /**
     * Terminates the consumer.
     */
    public void terminate()
    {
        log_.debug("Terminating the virtual machine monitoring data consumer");
        isTerminated_ = true;
    }

}
