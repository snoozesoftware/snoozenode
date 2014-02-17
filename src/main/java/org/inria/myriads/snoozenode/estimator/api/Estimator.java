package org.inria.myriads.snoozenode.estimator.api;

import java.util.List;

public interface Estimator
{
    double estimate(List<Double> values);
}
