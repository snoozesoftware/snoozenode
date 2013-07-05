package org.inria.myriads.snoozenode.localcontroller.metrics;

import java.io.IOException;
import java.net.UnknownHostException;

import org.inria.myriads.snoozenode.configurator.localcontrollermetrics.LocalControllerMetricsSettings;
import org.inria.myriads.snoozenode.localcontroller.metrics.api.MetricProducer;
import org.inria.myriads.snoozenode.localcontroller.metrics.api.impl.GmondProducer;

public class MetricProducerFactory
{
    
    public static MetricProducer createMetricProducer(LocalControllerMetricsSettings localControllerMetricsSettings)
    {
        String hostname = localControllerMetricsSettings.getHostname();
        int port = localControllerMetricsSettings.getPort();
        int interval =  localControllerMetricsSettings.getInterval();
        String[] metrics = localControllerMetricsSettings.getMetrics();
        return new GmondProducer(hostname, port,  metrics, interval);
    }
}
