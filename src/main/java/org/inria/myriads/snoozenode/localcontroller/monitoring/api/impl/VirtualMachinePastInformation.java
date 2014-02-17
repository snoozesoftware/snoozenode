package org.inria.myriads.snoozenode.localcontroller.monitoring.api.impl;

import java.util.List;

import org.inria.myriads.snoozenode.localcontroller.monitoring.information.NetworkTrafficInformation;
import org.inria.myriads.snoozenode.localcontroller.monitoring.information.VirtualMachineInformation;

public class VirtualMachinePastInformation 
{
  
    private long previousCpuTime_; 
    
    private long previousSystemTime_;
    
    private long previousRxBytes_;
    
    private long previousTxBytes_;
    
    /**
     * @return the previousCpuTime
     */
    public long getPreviousCpuTime()
    {
        return previousCpuTime_;
    }

    /**
     * @param previousCpuTime the previousCpuTime to set
     */
    public void setPreviousCpuTime(long previousCpuTime)
    {
        previousCpuTime_ = previousCpuTime;
    }

    /**
     * @return the previousSystemTime
     */
    public long getPreviousSystemTime()
    {
        return previousSystemTime_;
    }

    /**
     * @param previousSystemTime the previousSystemTime to set
     */
    public void setPreviousSystemTime(long previousSystemTime)
    {
        previousSystemTime_ = previousSystemTime;
    }

    /**
     * @return the previousRxBytes
     */
    public long getPreviousRxBytes()
    {
        return previousRxBytes_;
    }

    /**
     * @param previousRxBytes the previousRxBytes to set
     */
    public void setPreviousRxBytes(long previousRxBytes)
    {
        previousRxBytes_ = previousRxBytes;
    }

    /**
     * @return the previousTxBytes
     */
    public long getPreviousTxBytes()
    {
        return previousTxBytes_;
    }

    /**
     * @param previousTxBytes the previousTxBytes to set
     */
    public void setPreviousTxBytes(long previousTxBytes)
    {
        previousTxBytes_ = previousTxBytes;
    }

   
    
    

}
