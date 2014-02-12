package org.inria.myriads.snoozenode.localcontroller.anomaly.runner;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.Resource;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.database.api.LocalControllerRepository;
import org.inria.myriads.snoozenode.localcontroller.anomaly.detector.api.AnomalyDetector;
import org.inria.myriads.snoozenode.localcontroller.anomaly.detector.api.AnomalyDetectorEstimator;
import org.inria.myriads.snoozenode.localcontroller.anomaly.listener.AnomalyDetectorListener;
import org.inria.myriads.snoozenode.localcontroller.anomaly.runner.AnomalyDetectorRunner;
import org.inria.myriads.snoozenode.localcontroller.monitoring.enums.LocalControllerState;
import org.inria.myriads.snoozenode.localcontroller.monitoring.producer.VirtualMachineHeartbeatDataProducer;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.AggregatedVirtualMachineData;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.LocalControllerDataTransporter;
import org.inria.myriads.snoozenode.util.OutputUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author msimonin
 *
 */
public class AnomalyDetectorRunner implements Runnable
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(AnomalyDetectorRunner.class);
    
    /** LocalController Repository.*/
    private LocalControllerRepository repository_;
    
    /** Lock object. */
    private Object lockObject_;

    /** Terminated. */
    private boolean isTerminated_;
    
    /** interval.*/
    private int interval_;
    
    /** anomalyDetectorListener */
    private AnomalyDetectorListener listener_;
    
    /** anomalyDetectorEstimator*/
    private AnomalyDetector anomalyDetector_;
    

    /**
     * Anomaly detector
     * @param anomalyDetector 
     * 
     * @param 
     */
    public AnomalyDetectorRunner(
            LocalControllerRepository repository, 
            AnomalyDetector anomalyDetector, 
            AnomalyDetectorListener listener)
    {
        Guard.check(repository);
        log_.debug("Initializing the anomaly detector");
        repository_ = repository;
        lockObject_ = new Object();
        interval_ = 10000; //TODO remove hard coded.
        anomalyDetector_ = anomalyDetector;
        listener_ = listener;
    }
    
    /** The run() method. */
    public void run() 
    {
        long pastTimestamp = 0 ; 
        try
        {
            boolean isDetected = false;
            while (!isTerminated_)
            {    
                log_.debug("Anomaly detector waked up");
                if (pastTimestamp > 0)
                {
                    Map<String, Resource> hostResources = repository_.getLastHostMonitoringValues(pastTimestamp);
                    List<VirtualMachineMetaData> virtualMachines = repository_.getLastVirtualMachineMetaData(pastTimestamp);
                    
                    //logic to extract in this class
                    LocalControllerState state = anomalyDetector_.detectAnomaly(hostResources, virtualMachines);
                    if (!state.equals(LocalControllerState.STABLE))
                    {
                        listener_.onAnomalyDetected(state);
                    }
                }
                
                pastTimestamp = new Timestamp(System.currentTimeMillis()).getTime();
                synchronized (lockObject_)
                {
                    lockObject_.wait(interval_);
                }
            }            
        }
        catch (InterruptedException exception) 
        {
            log_.error(String.format("Anomaly detector was interruped: %s", 
                                      exception.getMessage()));
        }
        log_.debug("Anomaly detector terminated");
    }
            
    /** 
     * Terminates the thread.
     */
    public synchronized void terminate() 
    {
        log_.debug("Terminating the heartbeat producer");
        synchronized (lockObject_)
        {
            isTerminated_ = true;
            lockObject_.notify();
        }
    }

    

    
}
