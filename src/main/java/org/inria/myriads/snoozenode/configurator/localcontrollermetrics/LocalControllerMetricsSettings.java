package org.inria.myriads.snoozenode.configurator.localcontrollermetrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.inria.myriads.snoozenode.localcontroller.metrics.MetricsType;

/**
 * @author msimonin
 *
 */
public class LocalControllerMetricsSettings
{
    /** Metric type.*/
    private MetricsType metricType_; 
    
    /** Hostname. */
    private String hostname_;
    
    /** port*/
    private int port_;
    
    /** Metrics to get. */
    private String[] metrics_;

    /** Probing interval .*/
    private int interval;
    
    /** Thresholds.*/
    private Map<String, List<Double> > thresholds_;
    
    /** Number of metrics entries to collect. */
    private int numberOfMetricsEntries_;
    
    
    
    
    public LocalControllerMetricsSettings()
    {
        thresholds_ = new HashMap<String, List<Double>>();
    }

    public int getNumberOfMetricsEntries()
    {
        return numberOfMetricsEntries_;
    }

    public void setNumberOfMetricsEntries(int numberOfMetricsEntries)
    {
        numberOfMetricsEntries_ = numberOfMetricsEntries;
    }

    /**
     * @return the metricType
     */
    public MetricsType getMetricType()
    {
        return metricType_;
    }

    public Map<String, List<Double>> getThresholds()
    {
        return thresholds_;
    }

    public void setThresholds(Map<String, List<Double>> thresholds)
    {
        thresholds_ = thresholds;
    }

    /**
     * @param metricType the metricType to set
     */
    public void setMetricType(MetricsType metricType)
    {
        metricType_ = metricType;
    }

    /**
     * @return the hostname
     */
    public String getHostname()
    {
        return hostname_;
    }

    /**
     * @param hostname the hostname to set
     */
    public void setHostname(String hostname)
    {
        hostname_ = hostname;
    }

    /**
     * @return the port
     */
    public int getPort()
    {
        return port_;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port)
    {
        port_ = port;
    }

    /**
     * @return the metrics
     */
    public String[] getMetrics()
    {
        return metrics_;
    }

    /**
     * @param metrics the metrics to set
     */
    public void setMetrics(String[] metrics)
    {
        metrics_ = metrics;
    }

    /**
     * @return the interval
     */
    public int getInterval()
    {
        return interval;
    }

    /**
     * @param interval the interval to set
     */
    public void setInterval(int interval)
    {
        this.interval = interval;
    }

    public void setThreshold(String metric, List<Double> currentThresholds)
    {
        this.thresholds_.put(metric, currentThresholds);
        
    }
    
}
