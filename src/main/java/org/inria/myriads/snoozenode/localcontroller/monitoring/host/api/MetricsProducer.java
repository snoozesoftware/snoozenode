package org.inria.myriads.snoozenode.localcontroller.monitoring.host.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.inria.myriads.snoozecommon.metric.Metric;
import org.inria.myriads.snoozenode.localcontroller.monitoring.host.api.impl.CpuTempProducer;
import org.inria.myriads.snoozenode.localcontroller.monitoring.host.api.impl.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Generates the metrics and put them in the blockingQueue.
 * Register and unregister metrics.
 * 
 * @author msimonin
 *
 */
public class MetricsProducer extends Thread
{
    
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(MetricsProducer.class);

    /** Producer list. */
    private List<MetricProducer> producers_;
     
     /** Blocking queue*/
    private BlockingQueue<Metric> metricQueue_ ;

    /** Indicates whether the thread sould terminate.*/
    private boolean isTerminated_;

    /** Monitoring intervale. */
    private long monitoringInterval_ ;

        /**
     * @param producers
     * @param metricQueue
     */
    public MetricsProducer(BlockingQueue<Metric> metricQueue)
    {
        metricQueue_ = metricQueue;
        producers_.add(new CpuTempProducer());
        monitoringInterval_ = 5000 ; //switch to default initialization;
        log_.debug("Metrics producer initialized");
    }


    public MetricsProducer(int interval, BlockingQueue<Metric> metricQueue)
    {
        metricQueue_ = metricQueue;
        producers_ = new ArrayList<MetricProducer>();
        producers_.add(new CpuTempProducer());
        producers_.add(new Test());
        monitoringInterval_ = interval ; //switch to default initialization;
        log_.debug("Metrics producer initialized");
    }


    public void run()
    {
        
        log_.debug(String.format("Starting metrics producer"));
        try
        {
            while (true)
            {            
                
                if (isTerminated_)
                {
                    break;
                }
                log_.debug("Starting metrics collection");
                for (MetricProducer producer : producers_)
                {
                    log_.debug(String.format("Getting %s metric", producer.getName()));
                    metricQueue_.put(producer.getMetric());
                    
                }
                log_.debug("metrics collection finished");
                Thread.sleep(monitoringInterval_);
            }
        }
        catch(Exception exception)
        {
            log_.warn("Failed to produce metrics");
            
        }
    } 
     

    
    /** 
     * Terminates the thread.
     */
    public synchronized void terminate() 
    {          
        isTerminated_ = true;
    }
    
}
