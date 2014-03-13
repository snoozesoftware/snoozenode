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
 
    /** Estimator settings.*/
    protected EstimatorSettings estimatorSettings_;
    
    /** monitoring settings.*/
    protected MonitoringSettings monitoringSettings_;
    
    /** host monitoring settings.*/
    protected HostMonitoringSettings hostMonitoringSettings_;
    
    
    /**
     * 
     * Initializes the resource demand estimator.
     * 
     * @throws ResourceDemandEstimatorException The exception.
     */
    public abstract void initialize() throws ResourceDemandEstimatorException;
    
    
    /** 
     * Checks whether a local controller has enough active capacity to host the VM.
     * 
     * @param virtualMachine        The virtual machine meta data        
     * @param localController       The local controller description
     * @return                      true if enough capacity vailable, false otherwise
     */
    public abstract boolean hasEnoughLocalControllerCapacity(VirtualMachineMetaData virtualMachine, 
            LocalControllerDescription localController);
   
    /**
     * Verifies weather a virtual machine fits into the group manager based
     * on its summary information.
     * 
     * @param virtualMachine     The virtual machine meta data
     * @param groupManager       The group manager data
     * @return                   true if everything ok, false otherwise
     */
    public abstract boolean hasEnoughGroupManagerCapacity(VirtualMachineMetaData virtualMachine,
            GroupManagerDescription groupManager);
    
    /**
     * Estimates the local controller utilization.
     * 
     * @param localController    The local controller description
     * @return                   The estimated local controller utilization
     */
    public abstract ArrayList<Double> computeLocalControllerCapacity(LocalControllerDescription localController);
    
    /**
     * Returns the current group manager summary data.
     * 
     * @param localControllers      The list of local controllers
     * @return                      The group manager summary information
     */
    public abstract GroupManagerSummaryInformation generateGroupManagerSummaryInformation(
            ArrayList<LocalControllerDescription> localControllers);
    
    
    /**
     * Estimates virtual machine resource demands.
     * 
     * @param virtualMachine     The virtual machine meta data
     * @return                   The estimated virtual machine monitoring data
     */
    public abstract ArrayList<Double> estimateVirtualMachineResourceDemand(VirtualMachineMetaData virtualMachine);
    
    
    /**
     * Estimates virtual machine resource demands.
     * 
     * @param hostUtilizationHistory     host utilization.
     * @return                           The estimated host monitoring data
     */
    public abstract Map<String, Double> estimateHostResourceUtilization(Map<String, Resource> hostUtilizationHistory);
    
    /**
     * Computes the allowed utilization.
     * 
     * @param sourceLocalController    The local controller description
     * @return                         The allowed capacity
     */
    public abstract List<Double> computeMaxAllowedCapacity(LocalControllerDescription sourceLocalController);
    
    /** 
     * Number of monitoring entries.
     * TODO not abstract?
     * @return      The number of monitoring entries
     */
    public abstract int getNumberOfMonitoringEntries();
    
    
    /**
     * 
     * Sorts the virtual machines.
     * 
     * @param virtualMachines   Virtual machines.
     * @param decreasing        True iff decreasing sort.
     */
    public void sortVirtualMachines(List<VirtualMachineMetaData> virtualMachines, boolean decreasing)
    {
        Collections.sort(
                virtualMachines, 
                ComparatorFactory.newVirtualMachinesComparator(estimatorSettings_, this, decreasing)
                );
    }
    
    /**
     * 
     * Sorts the local controllers.
     * 
     * @param localControllers  The localcontrollers to sort.
     * @param decreasing        True iff decreasing sort.
     */
    public void sortLocalControllers(List<LocalControllerDescription> localControllers, boolean decreasing)
    {
        Collections.sort(
                localControllers,
                ComparatorFactory.newLocalControllersComparator(estimatorSettings_, this, decreasing)
                );
    }
    
    /**
     * 
     * Sorts the group managers.
     * 
     * @param groupManagers The group managers to sort.
     * @param decreasing    True iff decreasing sort.
     */
    public void sortGroupManagers(List<GroupManagerDescription> groupManagers, boolean decreasing)
    {
        Collections.sort(
                groupManagers,
                ComparatorFactory.newGroupManagersComparator(estimatorSettings_, this, decreasing));
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
