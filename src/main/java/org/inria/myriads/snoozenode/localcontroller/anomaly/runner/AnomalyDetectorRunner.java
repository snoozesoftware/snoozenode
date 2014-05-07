package org.inria.myriads.snoozenode.localcontroller.anomaly.runner;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.localcontroller.Resource;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.database.api.LocalControllerRepository;
import org.inria.myriads.snoozenode.localcontroller.anomaly.detector.api.AnomalyDetector;
import org.inria.myriads.snoozenode.localcontroller.anomaly.listener.AnomalyDetectorListener;
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
    
    /** anomalyDetectorListener. */
    private AnomalyDetectorListener listener_;
    
    /** anomalyDetectorEstimator. */
    private AnomalyDetector anomalyDetector_;

    /** number of monitoring entries to take into account.*/
    private int numberOfMonitoringEntries_;
    

    /**
     * 
     * The anomaly detector runner.
     * 
     * @param repository        The local controller repository.
     * @param anomalyDetector   The anomaly detector.
     * @param listener           The listener.
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
        anomalyDetector_ = anomalyDetector;
        numberOfMonitoringEntries_ = anomalyDetector_.getSettings().getNumberOfMonitoringEntries();
        log_.debug("anomaly interval " + anomalyDetector_.getSettings().getInterval());
        interval_ = anomalyDetector_.getSettings().getInterval(); 
        listener_ = listener;
    }
    
    /** The run() method. */
    public void run() 
    {
        long pastTimestamp = 0;
        try
        {
            while (!isTerminated_)
            {    
                log_.debug("Anomaly detector waked up");
                if (pastTimestamp > 0)
                {
                    Map<String, Resource> hostResources =
                            repository_.getHostMonitoringValues(numberOfMonitoringEntries_);
                    List<VirtualMachineMetaData> virtualMachines =
                            repository_.getVirtualMachines(numberOfMonitoringEntries_);
                    
                    //logic to extract in this class
                    Object anomaly = anomalyDetector_.detectAnomaly(hostResources, virtualMachines);
                    if (anomaly != null)
                    {
                        listener_.onAnomalyDetected(anomaly);
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
