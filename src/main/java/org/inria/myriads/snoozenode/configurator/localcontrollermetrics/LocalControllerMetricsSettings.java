package org.inria.myriads.snoozenode.configurator.localcontrollermetrics;

import org.inria.myriads.snoozenode.localcontroller.monitoring.host.MetricsType;

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
    
    /**
     * @return the metricType
     */
    public MetricsType getMetricType()
    {
        return metricType_;
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
    
    
    

}
