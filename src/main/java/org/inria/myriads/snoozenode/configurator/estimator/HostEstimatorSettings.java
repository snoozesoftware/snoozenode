package org.inria.myriads.snoozenode.configurator.estimator;

public final class HostEstimatorSettings
{
    private String estimatorName_;
    

    /**
     * @return the estimatorName
     */
    public String getEstimatorName()
    {
        return estimatorName_;
    }

    /**
     * @param estimatorName the estimatorName to set
     */
    public void setEstimatorName(String estimatorName)
    {
        estimatorName_ = estimatorName;
    }

  

    /**
     * @param estimatorName
     */
    public HostEstimatorSettings(String estimatorName)
    {
        estimatorName_ = estimatorName;
    }
    
    
}
