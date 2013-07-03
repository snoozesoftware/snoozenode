package org.inria.myriads.snoozenode.localcontroller.monitoring.host.api;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.inria.myriads.snoozecommon.metric.Metric;
import org.inria.myriads.snoozenode.configurator.localcontrollermetrics.LocalControllerMetricsSettings;
import org.inria.myriads.snoozenode.localcontroller.monitoring.host.MetricProducerFactory;
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
    //private List<MetricProducer> producers_;
    
    /** Metric producer. */ 
    private MetricProducer producer_ ;
    
     /** Blocking queue*/
    private BlockingQueue<Metric> metricQueue_ ;

    /** Indicates whether the thread sould terminate.*/
    private boolean isTerminated_;

    /** Monitoring intervale. */
    private long monitoringInterval_ ;

    /** Settings. */
    private LocalControllerMetricsSettings localControllerMetricsSettings_;

   
    public MetricsProducer(LocalControllerMetricsSettings localControllerMetricsSettings, BlockingQueue<Metric> metricQueue)
    {
        metricQueue_ = metricQueue;
        localControllerMetricsSettings_ = localControllerMetricsSettings;
        monitoringInterval_ = localControllerMetricsSettings.getInterval();
        initializeProducer();
        log_.debug("Metrics producer initialized");
    }

    private void initializeProducer()
    {
        try{
            producer_ = MetricProducerFactory.createMetricProducer(localControllerMetricsSettings_);
            this.start();
        }
        catch(Exception exception)
        {
            log_.warn("Unable to create the metrics producer ... check your config file");
            exception.printStackTrace();
        }
        
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
                
                List<Metric> metrics = producer_.getMetric();
                for ( Metric metric : metrics )
                {
                    metricQueue_.put(metric);
                }
                
                
                log_.debug("metrics collection finished");
                Thread.sleep(monitoringInterval_);
            }
        }
        catch(Exception exception)
        {
            log_.warn("Failed to produce metrics " + exception.getMessage() );
            exception.printStackTrace();
            
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
