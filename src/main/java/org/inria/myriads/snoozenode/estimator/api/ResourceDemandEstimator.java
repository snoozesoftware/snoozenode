package org.inria.myriads.snoozenode.estimator.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.groupmanager.summary.GroupManagerSummaryInformation;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.Resource;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozenode.configurator.estimator.EstimatorSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.HostMonitoringSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.MonitoringSettings;
import org.inria.myriads.snoozenode.estimator.comparator.ComparatorFactory;
import org.inria.myriads.snoozenode.exception.ResourceDemandEstimatorException;

/**
 * @author msimonin
 *
 */
public abstract class ResourceDemandEstimator
{
    
    protected EstimatorSettings estimatorSettings_;
    
    protected MonitoringSettings monitoringSettings_;
    
    protected HostMonitoringSettings hostMonitoringSettings_;
    

    
    public abstract void initialize() throws ResourceDemandEstimatorException ;
    
    public abstract boolean hasEnoughLocalControllerCapacity(VirtualMachineMetaData virtualMachine, 
            LocalControllerDescription localController);
    
    public abstract ArrayList<Double> computeLocalControllerCapacity(LocalControllerDescription localController);
    
    public abstract GroupManagerSummaryInformation generateGroupManagerSummaryInformation(
            ArrayList<LocalControllerDescription> localControllers);
    
    public abstract ArrayList<Double> estimateVirtualMachineResourceDemand(VirtualMachineMetaData firstVirtualMachine);
    
    public abstract Map<String, Double> estimateHostResourceUtilization(Map<String, Resource> hostUtilizationHistory);
    
    public abstract List<Double> computeMaxAllowedCapacity(LocalControllerDescription sourceLocalController);
    
    // TODO not abstract ?
    public abstract int getNumberOfMonitoringEntries();
    
    public void sortVirtualMachines(List<VirtualMachineMetaData> virtualMachines, boolean decreasing)
    {
        Collections.sort(virtualMachines, ComparatorFactory.newVirtualMachinesComparator(estimatorSettings_, this, decreasing));
    }
    
    public void sortLocalControllers(List<LocalControllerDescription> localControllers, boolean decreasing)
    {
        Collections.sort(localControllers, ComparatorFactory.newLocalControllersComparator(estimatorSettings_, this, decreasing));
    }
    
    public void sortGroupManagers(List<GroupManagerDescription> groupManagers, boolean decreasing)
    {
        Collections.sort(groupManagers, ComparatorFactory.newGroupManagersComparator(estimatorSettings_, this, decreasing));
    }
    

    
    /**
     * @return the estimatorSettings
     */
    public EstimatorSettings getEstimatorSettings()
    {
        return estimatorSettings_;
    }

    /**
     * @param estimatorSettings the estimatorSettings to set
     */
    public void setEstimatorSettings(EstimatorSettings estimatorSettings)
    {
        estimatorSettings_ = estimatorSettings;
    }

    /**
     * @return the hostMonitoringSettings
     */
    public HostMonitoringSettings getHostMonitoringSettings()
    {
        return hostMonitoringSettings_;
    }

    /**
     * @param hostMonitoringSettings the hostMonitoringSettings to set
     */
    public void setHostMonitoringSettings(HostMonitoringSettings hostMonitoringSettings)
    {
        hostMonitoringSettings_ = hostMonitoringSettings;
    }

    /**
     * @return the monitoringSettings
     */
    public MonitoringSettings getMonitoringSettings()
    {
        return monitoringSettings_;
    }

    /**
     * @param monitoringSettings the monitoringSettings to set
     */
    public void setMonitoringSettings(MonitoringSettings monitoringSettings)
    {
        monitoringSettings_ = monitoringSettings;
    }

    

    
    
       
    
    
    
    
}
