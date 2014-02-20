package org.inria.myriads.snoozenode.groupmanager.managerpolicies.comparators.api;

import java.util.Comparator;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;

public abstract class SnoozeComparator<T> implements Comparator<T> 
{
    protected ResourceDemandEstimator estimator_;
    
    protected boolean decreasing_;
    
    public abstract void initialize();

    public int compare(T o1, T o2)
    {   
        if (decreasing_)
        {
            return -internalCompare(o1, o2);
        }
        return internalCompare(o1, o2);
    }
    
    protected abstract int internalCompare(T o1, T o2);
    

    /**
     * @return the estimator
     */
    public ResourceDemandEstimator getEstimator()
    {
        return estimator_;
    }

    /**
     * @param estimator the estimator to set
     */
    public void setEstimator(ResourceDemandEstimator estimator)
    {
        estimator_ = estimator;
    }

    /**
     * @return the decreasing
     */
    public boolean isDecreasing()
    {
        return decreasing_;
    }

    /**
     * @param decreasing the decreasing to set
     */
    public void setDecreasing(boolean decreasing)
    {
        decreasing_ = decreasing;
    }
    
    
}
