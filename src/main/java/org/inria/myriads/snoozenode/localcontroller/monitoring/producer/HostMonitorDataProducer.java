package org.inria.myriads.snoozenode.localcontroller.monitoring.producer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.Resource;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.HostMonitoringData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.VirtualMachineMonitoringData;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.configurator.monitoring.HostMonitorSettings;
import org.inria.myriads.snoozenode.exception.HostMonitoringException;
import org.inria.myriads.snoozenode.localcontroller.monitoring.MonitoringFactory;
import org.inria.myriads.snoozenode.localcontroller.monitoring.api.HostMonitor;
import org.inria.myriads.snoozenode.localcontroller.monitoring.api.VirtualMachineMonitor;
import org.inria.myriads.snoozenode.localcontroller.monitoring.api.impl.GangliaHostMonitor;
import org.inria.myriads.snoozenode.localcontroller.monitoring.information.VirtualMachineInformation;
import org.inria.myriads.snoozenode.localcontroller.monitoring.service.HostMonitoringService;
import org.inria.myriads.snoozenode.localcontroller.monitoring.service.InfrastructureMonitoring;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.AggregatedHostMonitoringData;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.AggregatedVirtualMachineData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Produce metrics from a given monitor (ganglia, libvirt...)
 * 
 * @author msimonin
 *
 */
public class HostMonitorDataProducer extends Thread
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(HostMonitorDataProducer.class);
    
    /** Producer Id*/
    private String id_;
    
    /** Host Monitor*/
    private HostMonitor monitor_;

    /** IsTerminated.*/
    private boolean isTerminated_;

    /** Is suspended*/
    private boolean isSuspended_;
    
    /** Aggregates current datas.*/
    private ArrayList<HostMonitoringData> monitoringData_;
    
    /** history size.*/
    private int historySize_;

    private BlockingQueue<AggregatedHostMonitoringData> dataQueue_;

    private int interval_;

    private Object lockObject_;

    private String localControllerId_;

    /** host monitoring service.*/
    private HostMonitoringService hostMonitoringService_;
    
    public HostMonitorDataProducer(
            String id,
            BlockingQueue<AggregatedHostMonitoringData> dataQueue, 
            HostMonitorSettings hostMonitorSettings,
            LocalControllerDescription localController,
            HostMonitoringService hostMonitoringService
            ) throws HostMonitoringException
    {
        id_ = id;
        localControllerId_ = localController.getId();
        dataQueue_ = dataQueue;
        hostMonitoringService_ = hostMonitoringService; 
        // TODO what to do with this historySize ?
        historySize_ = 1;
        interval_ = hostMonitorSettings.getInterval();
        isSuspended_ = false;
        isTerminated_ = false;
        lockObject_ = new Object();
        monitoringData_ = new ArrayList<HostMonitoringData>();
        
        monitor_ = MonitoringFactory.newHostMonitor(localController, hostMonitorSettings);
    }


    
    /**
     * The run routine.
     */
    public void run() 
    {

        log_.debug(String.format("Starting host monitoring data producer for %s", localControllerId_));
        try
        {
            while (true)
            {            
   
                doSleep(interval_);       
                
                if (isTerminated_)
                {
                    break;
                }
                // Get the resource managed by this monitor.
                log_.debug("Get resource data");
                HostMonitoringData resourceData = monitor_.getResourceData();
                if (resourceData == null)
                {
                    //skipping null resource data.
                    continue;
                }
                log_.debug("got resource");
                if (!isSuspended_)
                {
                    
                    log_.debug(String.format("Size of aggregated host %s monitoring data is %d / %d",
                                             localControllerId_, monitoringData_.size(), historySize_));
                    
                    if (monitoringData_.size() == historySize_)
                    {
                        log_.debug(String.format("Adding aggregated host %s monitoring data to the " +
                                                 "monitoring service queue", 
                                                 localControllerId_));
                        
                        AggregatedHostMonitoringData data = createAggregatedHostData(monitoringData_);
                        dataQueue_.put(data);
                        monitoringData_.clear();
                    } 

                    log_.debug("Aggregating resource");
                    monitoringData_.add(resourceData);
                
                }
                
                
            }
        }
        catch (Exception exception) 
        {
            exception.printStackTrace();
            log_.debug(String.format("Failed to generate host monitoring data: %s", 
                                     exception.getMessage()));
            hostMonitoringService_.onMonitoringFailure(id_);
        }
          
    }

    
    @SuppressWarnings("unchecked")
    private AggregatedHostMonitoringData createAggregatedHostData(ArrayList<HostMonitoringData> monitoringData)
    {
        log_.debug("Creating aggregated host data object");
        ArrayList<HostMonitoringData> clonedData = (ArrayList<HostMonitoringData>) monitoringData.clone();
        
        AggregatedHostMonitoringData aggregatedData = new AggregatedHostMonitoringData(localControllerId_, clonedData);
        return aggregatedData;
    }



    /**
     * Puts the thread to sleep.
     *  
     * @param milliseconds   The number of milliseconds
     * @throws InterruptedException 
     */
    private void doSleep(long milliseconds) 
        throws InterruptedException
    {
        synchronized (lockObject_)
        {
            lockObject_.wait(milliseconds);
        }
    }



    public void setSuspend()
    {
        // TODO Auto-generated method stub
        
    }

    public void wakeup()
    {
        // TODO Auto-generated method stub
        
    }

    public synchronized void terminate()
    {
        log_.debug("Terminating the consumer");
        synchronized (lockObject_)
        {
            isTerminated_ = true;
            lockObject_.notify();
        }
    }
    

}
