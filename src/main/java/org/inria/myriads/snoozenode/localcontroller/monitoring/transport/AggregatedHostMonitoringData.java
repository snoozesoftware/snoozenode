package org.inria.myriads.snoozenode.localcontroller.monitoring.transport;

import java.io.Serializable;
import java.util.List;

import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.HostMonitoringData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.VirtualMachineMonitoringData;

/**
 * @author msimonin
 *
 */
public class AggregatedHostMonitoringData implements Serializable
{

    /** Serial id.*/
    private static final long serialVersionUID = 1L;
    
    /** Virtual machine identifier. */
    private String localControllerId_;
    
    /** Host monitoring data. */
    private List<HostMonitoringData> monitoringData_;

    /**
     * 
     */
    public AggregatedHostMonitoringData()
    {
        super();
    }

    /**
     * @param localControllerId
     * @param monitoringData
     */
    public AggregatedHostMonitoringData(String localControllerId, List<HostMonitoringData> monitoringData)
    {
        localControllerId_ = localControllerId;
        monitoringData_ = monitoringData;
    }


    /**
     * @return the localControllerId
     */
    public String getLocalControllerId()
    {
        return localControllerId_;
    }

    /**
     * @param localControllerId the localControllerId to set
     */
    public void setLocalControllerId(String localControllerId)
    {
        localControllerId_ = localControllerId;
    }

    /**
     * @return the monitoringData
     */
    public List<HostMonitoringData> getMonitoringData()
    {
        return monitoringData_;
    }

    /**
     * @param monitoringData the monitoringData to set
     */
    public void setMonitoringData(List<HostMonitoringData> monitoringData)
    {
        monitoringData_ = monitoringData;
    }
    
    
}
