package org.inria.myriads.snoozenode.localcontroller.metrics.producer;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.inria.myriads.snoozecommon.metric.AggregatedMetricData;
import org.inria.myriads.snoozecommon.metric.Metric;
import org.inria.myriads.snoozenode.configurator.localcontrollermetrics.LocalControllerMetricsSettings;
import org.inria.myriads.snoozenode.localcontroller.metrics.MetricProducerFactory;
import org.inria.myriads.snoozenode.localcontroller.metrics.api.MetricProducer;
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
public class LocalControllerMetricProducer extends Thread
{
    
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LocalControllerMetricProducer.class);

    /** Producer list. */
    //private List<MetricProducer> producers_;
    
    /** Metric producer. */ 
    private MetricProducer producer_ ;
    
     /** Blocking queue*/
    private BlockingQueue<AggregatedMetricData> metricQueue_ ;

    /** Indicates whether the thread sould terminate.*/
    private boolean isTerminated_;

    /** Monitoring intervale. */
    private long monitoringInterval_ ;

    /** Settings. */
    private LocalControllerMetricsSettings localControllerMetricsSettings_;

   
    public LocalControllerMetricProducer(LocalControllerMetricsSettings localControllerMetricsSettings, BlockingQueue<AggregatedMetricData> metricQueue)
    {
        metricQueue_ = metricQueue;
        localControllerMetricsSettings_ = localControllerMetricsSettings;
        monitoringInterval_ = localControllerMetricsSettings.getInterval();
        producer_ = MetricProducerFactory.createMetricProducer(localControllerMetricsSettings);
        log_.debug("Metrics producer initialized ");
    }
    
    public void run()
    {
        AggregatedMetricData aggregatedMetricData = new AggregatedMetricData();
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
                    aggregatedMetricData.add(metric);
                    
                }
                
                metricQueue_.put((AggregatedMetricData) aggregatedMetricData.clone());
                aggregatedMetricData.clear();
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
