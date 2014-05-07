package org.inria.myriads.snoozenode.localcontroller.monitoring.consumer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.HostMonitoringData;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.database.api.LocalControllerRepository;
import org.inria.myriads.snoozenode.localcontroller.monitoring.service.HostMonitoringService;
import org.inria.myriads.snoozenode.localcontroller.monitoring.service.InfrastructureMonitoring;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.AggregatedHostMonitoringData;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.LocalControllerDataTransporter;
import org.inria.myriads.snoozenode.monitoring.comunicator.api.MonitoringCommunicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Host Monitor data consumer.
 * 
 * @author msimonin
 *
 */
public class HostMonitorDataConsumer implements Runnable
{

    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(HostMonitorDataConsumer.class);
    
    /** Data queue. */
    private BlockingQueue<AggregatedHostMonitoringData> dataQueue_;
    
    /** Host monitoring callback. */
    //private HostMonitoringListener callback_;

    /** Signals termination. */
    private boolean isTerminated_;
    
    /** Communicator with the upper level. */
    private MonitoringCommunicator communicator_;

    /** Local controller description.*/
    private LocalControllerRepository repository_;
    
    /** Local Controller Id.*/
    private String localControllerId_;
    
   
    /**
     * 
     * Constructor.
     * 
     * @param localController       The local Controller Description.
     * @param repository            The repository.
     * @param communicator          The communicator.
     * @param dataQueue             The data queue. 
     * @param monitoring            The infrastructure monitoring.
     * @param nodeConfiguration     The node configuration
     * @param hostMonitoringService The host monitoring service (listener)
     * @throws IOException          The exception.
     */
    public HostMonitorDataConsumer(
            LocalControllerDescription localController,
            LocalControllerRepository repository,
            MonitoringCommunicator communicator,
            BlockingQueue<AggregatedHostMonitoringData> dataQueue,
            InfrastructureMonitoring monitoring,
            NodeConfiguration nodeConfiguration, 
            HostMonitoringService hostMonitoringService) throws IOException
    {
        log_.debug("Initializing the host monitoring data consumer");
        localControllerId_ = localController.getId();
        repository_ = repository;
        dataQueue_ = dataQueue;
        communicator_ = communicator;
    }

    @Override
    public void run()
    {
        try
        {
            while (!isTerminated_)
            {
                AggregatedHostMonitoringData hostData = new AggregatedHostMonitoringData();
                log_.debug("Waiting for the host monitoring data to arrive...");
                
                
                hostData = dataQueue_.take();
                if (hostData == null)
                {
                    continue;
                }                    
                log_.debug("Received host monitoring data");
                
                if (hostData.getLocalControllerId().equals("heartbeat"))
                {
                    log_.debug("Received heartbeat data form localcontroller");
                    sendHeartbeatData();
                    continue;
                }
                
                sendRegularData(hostData);
                outputMetrics(hostData);
            }
        }
        catch (Exception exception)
        {
            log_.error("Host monitoring data consumer thread was interruped", exception);
        }
        log_.debug("Host monitoring data consumer stopped");
        terminate();
    }

    /**
     * 
     * Sends regular data.
     * 
     * @param hostData          The host data to send
     * @throws IOException      The exception.
     */
    private void sendRegularData(AggregatedHostMonitoringData hostData) throws IOException
    {
        log_.debug("Sending regular datas");
        LocalControllerDataTransporter dataTransporter =
                new LocalControllerDataTransporter(localControllerId_);
        // maybe we should clone the data before.
        // only one monitoring data (corresponding to the current lc).
        List<AggregatedHostMonitoringData> aggregatedData = new ArrayList<AggregatedHostMonitoringData>();
        aggregatedData.add(hostData);
        dataTransporter.setHostMonitoringAggregatedData(aggregatedData);
        communicator_.sendRegularData(dataTransporter);
        repository_.addAggregatedHostMonitoringData(aggregatedData);
        log_.debug("Regular datas sent");

    }

    /**
     * 
     * output metrics.
     * 
     * @param hostData  The host Data.
     */
    private void outputMetrics(AggregatedHostMonitoringData hostData)
    {
        String output = "\n------- metric collected -----\n";
        output += String.format("length : %d", hostData.getMonitoringData().size());
        for (HostMonitoringData m : hostData.getMonitoringData())
        {
            output += "Timestamp :" + m.getTimeStamp() + "\n";
            for (Entry<String, Double> c : m.getUsedCapacity().entrySet())
            {
                output += "metric : " + c.getKey() + "value : " + c.getValue() + "\n";
            }
        }
        output += "------- /metric collected -----" + "\n";
        log_.debug(output);
    }

    /**
     * 
     * Sends heartbeat data.
     * 
     * @throws IOException  exception.
     */
    private void sendHeartbeatData() throws IOException
    {
        log_.debug("Sending heartbeat datas");
        LocalControllerDataTransporter localControllerData = new LocalControllerDataTransporter(localControllerId_);
        communicator_.sendHeartbeatData(localControllerData);
        log_.debug("Heartbeat datas sent");
        
    }

    /**
     * Terminates the consumer.
     */
    public void terminate()
    {
        log_.debug("Terminating the host consumer");
        isTerminated_ = true;
        communicator_.close();
    }

}
