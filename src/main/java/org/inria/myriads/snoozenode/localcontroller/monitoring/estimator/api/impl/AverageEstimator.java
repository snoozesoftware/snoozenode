package org.inria.myriads.snoozenode.localcontroller.monitoring.estimator.api.impl;

import java.util.List;

import org.inria.myriads.snoozenode.localcontroller.monitoring.estimator.MonitoringEstimator;

public class AverageEstimator extends MonitoringEstimator
{

    @Override
    public double estimate(List<Double> values)
    {
        if (values.size() == 0)
        {
            return 0d;
        }
        
        Double average = 0d;
        for (Double value : values)
        {
            average += value;
        }
        return average/values.size();
    }

}
