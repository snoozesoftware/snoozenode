package org.inria.myriads.snoozenode.groupmanager.managerpolicies.comparators.api;

import java.util.Comparator;

import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;

/**
 * 
 * Snooze comparator.
 * 
 * @author msimonin
 *
 * @param <T>
 */
public abstract class SnoozeComparator<T> implements Comparator<T> 
{
    /** estimator.*/
    protected ResourceDemandEstimator estimator_;
    
    /** order.*/
    protected boolean decreasing_;
    
    
    /**
     * Initializes the comparator.
     */
    public abstract void initialize();

    
    @Override
    public int compare(T o1, T o2)
    {   
        if (decreasing_)
        {
            return -internalCompare(o1, o2);
        }
        return internalCompare(o1, o2);
    }
    
    /**
     * 
     * Internal compare (compare logic is here).
     * 
     * @param o1       object 1
     * @param o2       object 2
     * @return  comparison
     */
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
