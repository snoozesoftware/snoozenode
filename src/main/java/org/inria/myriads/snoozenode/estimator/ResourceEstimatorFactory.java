package org.inria.myriads.snoozenode.estimator;

import org.inria.myriads.snoozecommon.communication.localcontroller.MonitoringThresholds;
import org.inria.myriads.snoozenode.configurator.estimator.EstimatorSettings;
import org.inria.myriads.snoozenode.configurator.estimator.HostEstimatorSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.HostMonitoringSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.MonitoringSettings;
import org.inria.myriads.snoozenode.configurator.submission.PackingDensity;
import org.inria.myriads.snoozenode.estimator.api.impl.StaticDynamicResourceDemandEstimator;
import org.inria.myriads.snoozenode.exception.ResourceDemandEstimatorException;
import org.inria.myriads.snoozenode.groupmanager.estimator.api.HostMonitoringEstimator;
import org.inria.myriads.snoozenode.groupmanager.estimator.api.VirtualMachineMonitoringEstimator;
import org.inria.myriads.snoozenode.groupmanager.estimator.api.impl.AverageCPUDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.estimator.api.impl.AverageHostMonitoringEstimator;
import org.inria.myriads.snoozenode.groupmanager.estimator.api.impl.AverageMemoryDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.estimator.api.impl.AverageRxDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.estimator.api.impl.AverageTxDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.estimator.enums.Estimator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ResourceEstimatorFactory
{

    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(ResourceEstimatorFactory.class);
    
    public static StaticDynamicResourceDemandEstimator newResourceDemandEstimator(
            EstimatorSettings estimatorSettings, 
            MonitoringSettings monitoringSettings, 
            HostMonitoringSettings hostMonitoringsettings) throws ResourceDemandEstimatorException
    {
        StaticDynamicResourceDemandEstimator resourceDemandEstimator = new StaticDynamicResourceDemandEstimator();
        resourceDemandEstimator.setEstimatorSettings(estimatorSettings);
        resourceDemandEstimator.setMonitoringSettings(monitoringSettings);
        resourceDemandEstimator.setHostMonitoringSettings(hostMonitoringsettings);
        resourceDemandEstimator.initialize();
        return resourceDemandEstimator;
    }

    public static VirtualMachineMonitoringEstimator newVirtualMachineDemandCpuEstimator(String estimatorName)
    {
        log_.debug("Creating a new CPU demand estimator");
        Estimator estimator = Estimator.valueOf(estimatorName);
        VirtualMachineMonitoringEstimator cpuDemandEstimator = null;
        switch (estimator)
        {        
            case average :
                log_.debug("Selecting average CPU demand estimator");
                cpuDemandEstimator = new AverageCPUDemandEstimator();
            default : 
                log_.equals(String.format("Unknown CPU demand estimator selected: %s", estimatorName));
                break;
        }
        
        return cpuDemandEstimator;
    }
    
    public static VirtualMachineMonitoringEstimator newVirtualMachineDemandMemEstimator(String estimatorName)
    {
        log_.debug("Creating a new mem demand estimator");
        Estimator estimator = Estimator.valueOf(estimatorName);
        VirtualMachineMonitoringEstimator memDemandEstimator = null;
        switch (estimator)
        {        
            case average :
                log_.debug("Selecting average mem demand estimator");
                memDemandEstimator = new AverageMemoryDemandEstimator();
            default : 
                log_.equals(String.format("Unknown mem demand estimator selected: %s", estimatorName));
                memDemandEstimator = new AverageMemoryDemandEstimator();
                break;
        }
        
        return memDemandEstimator;
    }

    public static VirtualMachineMonitoringEstimator newVirtualMachineDemandRxEstimator(String estimatorName)
    {
        log_.debug("Creating a new rx demand estimator");
        Estimator estimator = Estimator.valueOf(estimatorName);
        VirtualMachineMonitoringEstimator rxDemandEstimator = null;
        switch (estimator)
        {        
            case average :
                log_.debug("Selecting average rx demand estimator");
                rxDemandEstimator = new AverageRxDemandEstimator();
                break;
            default : 
                log_.equals(String.format("Unknown rx demand estimator selected: %s", estimatorName));
                rxDemandEstimator = new AverageRxDemandEstimator();
                break;
        }
        
        return rxDemandEstimator;
    }
    
    public static VirtualMachineMonitoringEstimator newVirtualMachineDemandTxEstimator(String estimatorName)
    {
        log_.debug("Creating a new Tx demand estimator");
        Estimator estimator = Estimator.valueOf(estimatorName);
        VirtualMachineMonitoringEstimator txDemandEstimator = null;
        switch (estimator)
        {        
        case average :
            log_.debug("Selecting average Tx demand estimator");
            txDemandEstimator = new AverageTxDemandEstimator();
            break;
        default : 
            log_.equals(String.format("Unknown Tx demand estimator selected: %s", estimatorName));
            txDemandEstimator = new AverageTxDemandEstimator();
            break;
        }
        
        return txDemandEstimator;
    }

    public static HostMonitoringEstimator newHostMonitoringEstimator(HostEstimatorSettings estimatorSetting)
    {   
        log_.debug("Creating a new Host monitoring estimator");
        String estimatorName = estimatorSetting.getEstimatorName();
        Estimator estimator = Estimator.valueOf(estimatorName);
        HostMonitoringEstimator hostEstimator = null;
        switch(estimator)
        {
        case average :
               log_.debug("Selecting average host monitoting estimator");
               hostEstimator = new AverageHostMonitoringEstimator();
               break;
        default : 
            log_.equals(String.format("Unknown host monitoring estimator selected: %s", estimatorName));
            hostEstimator = new AverageHostMonitoringEstimator();
            break;
        }
        return hostEstimator;
    }

}
