package org.inria.myriads.snoozenode.configurator.estimator;


/**
 * 
 * Host Estimator Settings.
 * 
 * @author msimonin
 *
 */
public final class HostEstimatorSettings
{
    
    
    /** Estimator name.*/ 
    private String estimatorName_;
   
    
    /**
     * @param estimatorName     The estimator name.
     */
    public HostEstimatorSettings(String estimatorName)
    {
        estimatorName_ = estimatorName;
    }
    
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


  
    
    
}
