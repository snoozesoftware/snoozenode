package org.inria.myriads.snoozenode.localcontroller.metrics.api;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.inria.myriads.snoozecommon.metric.Metric;

public interface MetricProducer
{

    public List<Metric> getMetric() throws Exception;

    public String getType();
    
}
