package org.inria.myriads.snoozenode.localcontroller.monitoring.host.api;

import org.inria.myriads.snoozecommon.metric.Metric;

public interface MetricProducer
{

    public Metric getMetric();

    public String getName();
    
}
