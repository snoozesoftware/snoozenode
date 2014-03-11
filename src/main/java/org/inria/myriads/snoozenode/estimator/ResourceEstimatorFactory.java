package org.inria.myriads.snoozenode.estimator;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;

import org.inria.myriads.snoozecommon.communication.localcontroller.MonitoringThresholds;
import org.inria.myriads.snoozenode.configurator.estimator.EstimatorSettings;
import org.inria.myriads.snoozenode.configurator.estimator.HostEstimatorSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.HostMonitoringSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.MonitoringSettings;
import org.inria.myriads.snoozenode.configurator.submission.PackingDensity;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.estimator.api.impl.StaticDynamicResourceDemandEstimator;
import org.inria.myriads.snoozenode.exception.ResourceDemandEstimatorException;
import org.inria.myriads.snoozenode.groupmanager.anomaly.resolver.api.AnomalyResolver;
import org.inria.myriads.snoozenode.groupmanager.estimator.api.HostMonitoringEstimator;
import org.inria.myriads.snoozenode.groupmanager.estimator.api.VirtualMachineMonitoringEstimator;
import org.inria.myriads.snoozenode.groupmanager.estimator.api.impl.AverageCPUDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.estimator.api.impl.AverageHostMonitoringEstimator;
import org.inria.myriads.snoozenode.groupmanager.estimator.api.impl.AverageMemoryDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.estimator.api.impl.AverageRxDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.estimator.api.impl.AverageTxDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.estimator.enums.Estimator;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPolicy;
import org.inria.myriads.snoozenode.util.PluginUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ResourceEstimatorFactory
{

    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(ResourceEstimatorFactory.class);
    
    public static ResourceDemandEstimator newResourceDemandEstimator(
            EstimatorSettings estimatorSettings, 
            MonitoringSettings monitoringSettings, 
            HostMonitoringSettings hostMonitoringsettings) throws ResourceDemandEstimatorException
    {
        String estimatorName = estimatorSettings.getName();
        ResourceDemandEstimator resourceDemandEstimator = null;
        
        if (estimatorName.equals("StaticDynamic"))
        {
            log_.debug("Loading StaticDynamic resource demand estimator");
            resourceDemandEstimator = new StaticDynamicResourceDemandEstimator();    
        }
        else
        {
            log_.debug(String.format(
                    "Loading custom resource demand estimator : %s, trying to load it form plugins directory ",
                    estimatorName));           
            try
            {
                Object estimatorObject  = PluginUtils.createFromFQN(estimatorName);
                resourceDemandEstimator = (ResourceDemandEstimator) estimatorObject;
            }
            catch(Exception e)
            {
                log_.error("Unable to create the custom estimator, falling back to default");
                resourceDemandEstimator = new StaticDynamicResourceDemandEstimator();
            }
        }
        resourceDemandEstimator.setEstimatorSettings(estimatorSettings);
        resourceDemandEstimator.setMonitoringSettings(monitoringSettings);
        resourceDemandEstimator.setHostMonitoringSettings(hostMonitoringsettings);
        resourceDemandEstimator.initialize();
        return resourceDemandEstimator;
    }

    public static VirtualMachineMonitoringEstimator newVirtualMachineDemandCpuEstimator(String estimatorName)
    {
        log_.debug("Creating a new CPU demand estimator");
        VirtualMachineMonitoringEstimator cpuDemandEstimator = null;
        if (estimatorName.equals("average"))
        {
            log_.debug("Selecting average CPU demand estimator");
            cpuDemandEstimator = new AverageCPUDemandEstimator();
        }
        else
        {
            log_.debug(String.format("Selecting a custom CPU estimator : %s; trying to load it from plugin directory", estimatorName));
            log_.debug(String.format(
                    "Loading custom resource demand estimator : %s, trying to load it form plugins directory ",
                    estimatorName));           
            try
            {
                Object estimatorObject  = PluginUtils.createFromFQN(estimatorName);
                cpuDemandEstimator = (VirtualMachineMonitoringEstimator) estimatorObject;
            }
            catch(Exception e)
            {
                log_.error("Unable to create the custom estimator, falling back to default");
                cpuDemandEstimator = new AverageCPUDemandEstimator();
            }
            
        }        
        return cpuDemandEstimator;
    }
    
    public static VirtualMachineMonitoringEstimator newVirtualMachineDemandMemEstimator(String estimatorName)
    {
        log_.debug("Creating a new mem demand estimator");
        VirtualMachineMonitoringEstimator memDemandEstimator = null;
        if (estimatorName.equals("average"))
        {
            log_.debug("Selecting average CPU demand estimator");
            memDemandEstimator = new AverageMemoryDemandEstimator();
        }
        else
        {
            log_.debug(String.format("Selecting a custom CPU estimator : %s; trying to load it from plugin directory", estimatorName));
            log_.debug(String.format(
                    "Loading custom resource demand estimator : %s, trying to load it form plugins directory ",
                    estimatorName));           
            try
            {
                Object estimatorObject  = PluginUtils.createFromFQN(estimatorName);
                memDemandEstimator = (VirtualMachineMonitoringEstimator) estimatorObject;
            }
            catch(Exception e)
            {
                log_.error("Unable to create the custom estimator, falling back to default");
                memDemandEstimator = new AverageMemoryDemandEstimator();
            }
            
        }
        
        return memDemandEstimator;
    }

    public static VirtualMachineMonitoringEstimator newVirtualMachineDemandRxEstimator(String estimatorName)
    {
        log_.debug("Creating a new rx demand estimator");
        
        VirtualMachineMonitoringEstimator rxDemandEstimator = null;
        if (estimatorName.equals("average"))
        {
            log_.debug("Selecting average CPU demand estimator");
            rxDemandEstimator = new AverageRxDemandEstimator();
        }
        else
        {
            log_.debug(String.format("Selecting a custom CPU estimator : %s; trying to load it from plugin directory", estimatorName));
            log_.debug(String.format(
                    "Loading custom resource demand estimator : %s, trying to load it form plugins directory ",
                    estimatorName));           
            try
            {
                Object estimatorObject  = PluginUtils.createFromFQN(estimatorName);
                rxDemandEstimator = (VirtualMachineMonitoringEstimator) estimatorObject;
            }
            catch(Exception e)
            {
                log_.error("Unable to create the custom estimator, falling back to default");
                rxDemandEstimator = new AverageRxDemandEstimator();
            }
        }
        return rxDemandEstimator;
    }
    
    public static VirtualMachineMonitoringEstimator newVirtualMachineDemandTxEstimator(String estimatorName)
    {
        log_.debug("Creating a new Tx demand estimator");
        VirtualMachineMonitoringEstimator txDemandEstimator = null;
        if (estimatorName.equals("average"))
        {
            log_.debug("Selecting average CPU demand estimator");
            txDemandEstimator = new AverageRxDemandEstimator();
        }
        else
        {
            log_.debug(String.format("Selecting a custom CPU estimator : %s; trying to load it from plugin directory", estimatorName));
            log_.debug(String.format(
                    "Loading custom resource demand estimator : %s, trying to load it form plugins directory ",
                    estimatorName));           
            try
            {
                Object estimatorObject  = PluginUtils.createFromFQN(estimatorName);
                txDemandEstimator = (VirtualMachineMonitoringEstimator) estimatorObject;
            }
            catch(Exception e)
            {
                log_.error("Unable to create the custom estimator, falling back to default");
                txDemandEstimator = new AverageRxDemandEstimator();
            }
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
