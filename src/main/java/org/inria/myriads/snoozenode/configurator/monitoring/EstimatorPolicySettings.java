package org.inria.myriads.snoozenode.configurator.monitoring;

/**
 * @author msimonin
 *
 */
public class EstimatorPolicySettings
{
    /** CPU Estimator name.*/
    private String cpuEstimatorName_; 
    
    /** Memory Estimator name.*/
    private String memoryEstimatorName_;
    
    /** Network Estimator name.*/
    private String networkEstimatorName_;

    /**
     * Constructor.
     */
    public EstimatorPolicySettings()
    {
        super();
    }

    /**
     * @return the cpuEstimatorName
     */
    public String getCpuEstimatorName()
    {
        return cpuEstimatorName_;
    }

    /**
     * @param cpuEstimatorName the cpuEstimatorName to set
     */
    public void setCpuEstimatorName(String cpuEstimatorName)
    {
        cpuEstimatorName_ = cpuEstimatorName;
    }

    /**
     * @return the memoryEstimatorName
     */
    public String getMemoryEstimatorName()
    {
        return memoryEstimatorName_;
    }

    /**
     * @param memoryEstimatorName the memoryEstimatorName to set
     */
    public void setMemoryEstimatorName(String memoryEstimatorName)
    {
        memoryEstimatorName_ = memoryEstimatorName;
    }

    /**
     * @return the networkEstimatorName
     */
    public String getNetworkEstimatorName()
    {
        return networkEstimatorName_;
    }

    /**
     * @param networkEstimatorName the networkEstimatorName to set
     */
    public void setNetworkEstimatorName(String networkEstimatorName)
    {
        networkEstimatorName_ = networkEstimatorName;
    }
    
    
    
}
