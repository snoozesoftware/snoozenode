package org.inria.myriads.snoozenode.estimator.api.impl;

import java.util.List;

import org.inria.myriads.snoozenode.estimator.api.Estimator;



public class AverageEstimator implements Estimator
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
