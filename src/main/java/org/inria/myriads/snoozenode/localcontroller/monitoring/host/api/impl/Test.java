package org.inria.myriads.snoozenode.localcontroller.monitoring.host.api.impl;

import org.inria.myriads.snoozecommon.metric.Metric;
import org.inria.myriads.snoozenode.localcontroller.monitoring.host.api.MetricProducer;

public class Test implements MetricProducer
{

    private String name_ = "testMetric";
    
    @Override
    public Metric getMetric()
    {
        return new Metric(name_, 1d);
    }

    @Override
    public String getName()
    {
        return name_;
    }

}
