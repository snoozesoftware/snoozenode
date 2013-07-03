/**
 * Copyright (C) 2010-2013 Eugen Feller, INRIA <eugen.feller@inria.fr>
 *
 * This file is part of Snooze, a scalable, autonomic, and
 * energy-aware virtual machine (VM) management framework.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package org.inria.myriads.snoozenode.configurator.api.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Properties;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.NodeRole;
import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.HypervisorDriver;
import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.HypervisorSettings;
import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.HypervisorTransport;
import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.MigrationMethod;
import org.inria.myriads.snoozecommon.communication.localcontroller.wakeup.WakeupDriver;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.util.NetworkUtils;
import org.inria.myriads.snoozecommon.util.StringUtils;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.configurator.api.NodeConfigurator;
import org.inria.myriads.snoozenode.configurator.database.DatabaseSettings;
import org.inria.myriads.snoozenode.configurator.energymanagement.EnergyManagementSettings;
import org.inria.myriads.snoozenode.configurator.energymanagement.enums.PowerSavingAction;
import org.inria.myriads.snoozenode.configurator.energymanagement.enums.ShutdownDriver;
import org.inria.myriads.snoozenode.configurator.energymanagement.enums.SuspendDriver;
import org.inria.myriads.snoozenode.configurator.estimator.EstimatorSettings;
import org.inria.myriads.snoozenode.configurator.faulttolerance.FaultToleranceSettings;
import org.inria.myriads.snoozenode.configurator.httpd.HTTPdSettings;
import org.inria.myriads.snoozenode.configurator.localcontrollermetrics.LocalControllerMetricsSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.MonitoringSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.MonitoringThresholds;
import org.inria.myriads.snoozenode.configurator.networking.NetworkingSettings;
import org.inria.myriads.snoozenode.configurator.node.NodeSettings;
import org.inria.myriads.snoozenode.configurator.scheduler.GroupLeaderSchedulerSettings;
import org.inria.myriads.snoozenode.configurator.scheduler.GroupManagerSchedulerSettings;
import org.inria.myriads.snoozenode.configurator.submission.SubmissionSettings;
import org.inria.myriads.snoozenode.database.enums.DatabaseType;
import org.inria.myriads.snoozenode.exception.NodeConfiguratorException;
import org.inria.myriads.snoozenode.groupmanager.estimator.enums.Estimator;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.enums.Assignment;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.enums.Dispatching;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.enums.Placement;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.enums.Reconfiguration;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.enums.Relocation;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.sort.SortNorm;
import org.inria.myriads.snoozenode.localcontroller.monitoring.host.MetricsType;

/**
 * Node configurator.
 * 
 * @author Eugen Feller
 */
public final class JavaPropertyNodeConfigurator 
    implements NodeConfigurator
{
    /** NodeParams. */
    private NodeConfiguration nodeConfiguration_;
        
    /** Properties. */
    private Properties properties_;
    
    /**
     * Initialize parameters.
     *  
     * @param configurationFile             The configuration file
     * @throws NodeConfiguratorException 
     * @throws IOException 
     */
    public JavaPropertyNodeConfigurator(String configurationFile) 
        throws NodeConfiguratorException, IOException
    {
        Guard.check(configurationFile);
        nodeConfiguration_ = new NodeConfiguration();
          
        properties_ = new Properties();    
        FileInputStream fileInput = new FileInputStream(configurationFile);
        properties_.load(fileInput); 
        
        setNodeSettings();
        setNetworkingSettings();
        setHTTPdSettings();
        setHypervisorSettings();
        setDatabaseSettings();
        setFaultToleranceSettings();
        setMonitoringSettings();
        setEstimatorSettings();
        setGroupLeaderSchedulerSettings();
        setGroupManagerSchedulerSettings();
        setSubmissionSettings();
        setEnergyManagementSettings();
        setLocalControllerMetricsSettings();
        
        fileInput.close();
    }
    
    private void setLocalControllerMetricsSettings() throws NodeConfiguratorException
    {
        LocalControllerMetricsSettings localControllerMetricSettings = nodeConfiguration_.getLocalControllerMetricsSettings();

        String metricsType = getProperty("localController.metrics.type");
        localControllerMetricSettings.setMetricType(MetricsType.valueOf(metricsType));
        
        String hostname = getProperty("localController.metrics.hostname");
        localControllerMetricSettings.setHostname(hostname);
        
        String portString = getProperty("localController.metrics.port");
        int port = Integer.valueOf(portString);
        localControllerMetricSettings.setPort(port);
        
        int interval = Integer.valueOf(getProperty("localController.metrics.interval"));
        localControllerMetricSettings.setInterval(interval);
               
        String metricsString = getProperty("localController.metrics.published");
        metricsString = metricsString.replace(" ", "");
        String[] metrics = metricsString.split(",");
        localControllerMetricSettings.setMetrics(metrics);

    }

    /**
     * Sets the general settings.
     * 
     * @throws NodeConfiguratorException    The configuration exception
     */
    private void setNodeSettings() 
        throws NodeConfiguratorException
    {
        NodeSettings nodeSettings = nodeConfiguration_.getNode();
        String nodeRole = getProperty("node.role");
        nodeSettings.setRole(NodeRole.valueOf(nodeRole));    
        
        int networkRxCapacity = Integer.valueOf(getProperty("node.networkCapacity.Rx"));
        int networkTxCapacity = Integer.valueOf(getProperty("node.networkCapacity.Tx"));
        nodeSettings.getNetworkCapacity().setRxBytes(networkRxCapacity);
        nodeSettings.getNetworkCapacity().setTxBytes(networkTxCapacity);
    }
    

    /**
     * Set the network settings.
     * 
     * @throws NodeConfiguratorException    The configuration exception
     * @throws UnknownHostException         The unknown host exception
     */
    private void setNetworkingSettings() 
        throws NodeConfiguratorException, UnknownHostException 
    {
        NetworkingSettings networkingSettings = nodeConfiguration_.getNetworking();
        String listenAddress = properties_.getProperty("network.listen.address");
        if (listenAddress != null)
        {
            listenAddress = listenAddress.trim();
        } else
        {
            listenAddress = InetAddress.getLocalHost().getHostAddress();
        }
          
        int controlDataPort = Integer.valueOf(getProperty("network.listen.controlDataPort"));  
        NetworkAddress controlDataAddress = NetworkUtils.createNetworkAddress(listenAddress, 
                                                                                          controlDataPort);
        networkingSettings.getListen().setControlDataAddress(controlDataAddress);
        
        int monitoringDataPort = Integer.valueOf(getProperty("network.listen.monitoringDataPort"));
        NetworkAddress monitoringDataAddress = NetworkUtils.createNetworkAddress(listenAddress, 
                                                                                             monitoringDataPort);
        networkingSettings.getListen().setMonitoringDataAddress(monitoringDataAddress);
        
        String multicastAddress = getProperty("network.multicast.address");       
        int groupLeaderHeartbeatPort = Integer.valueOf(getProperty("network.multicast.groupLeaderHeartbeatPort"));
        NetworkAddress groupLeaderMulticast = NetworkUtils.createNetworkAddress(multicastAddress, 
                                                                                            groupLeaderHeartbeatPort);
        networkingSettings.getMulticast().setGroupLeaderHeartbeatAddress(groupLeaderMulticast);
        
        int groupManagerHeartbeatPort = Integer.valueOf(getProperty("network.multicast.groupManagerHeartbeatPort"));
        NetworkAddress groupManagerMulticast = NetworkUtils.createNetworkAddress(multicastAddress, 
                                                                                             groupManagerHeartbeatPort);
        networkingSettings.getMulticast().setGroupManagerHeartbeatAddress(groupManagerMulticast);
        
        // list strings separated by ,
        String virtualMachineSubnets = getProperty("network.virtualMachineSubnet");
        virtualMachineSubnets = virtualMachineSubnets.replace(" ", "");
        String[] subnets = virtualMachineSubnets.split(",");
        
        networkingSettings.setVirtualMachineSubnets(subnets);
    }
    
    /**
     * Submission settings.
     * 
     * @throws NodeConfiguratorException 
     */
    private void setSubmissionSettings() 
        throws NodeConfiguratorException
    {
        SubmissionSettings submissionSettings = nodeConfiguration_.getSubmission();    
        String dispatchingRetries = getProperty("submission.dispatching.numberOfRetries");
        submissionSettings.getDispatching().setNumberOfRetries(Integer.valueOf(dispatchingRetries));

        String dispatchingInterval = getProperty("submission.dispatching.retryInterval");
        submissionSettings.getDispatching().setRetryInterval(Integer.valueOf(dispatchingInterval));
        
        String collectionRetries = getProperty("submission.collection.numberOfRetries");
        submissionSettings.getCollection().setNumberOfRetries(Integer.valueOf(collectionRetries));
        
        String collectionInterval = getProperty("submission.collection.retryInterval");
        submissionSettings.getCollection().setRetryInterval(Integer.valueOf(collectionInterval));
        
        String cpuPackingDensity = getProperty("submission.packingDensity.cpu");
        double cpuDenity = Double.valueOf(cpuPackingDensity);
        submissionSettings.getPackingDensity().setCPU(cpuDenity);
        
        String memoryPackingDensity = getProperty("submission.packingDensity.memory");
        double memoryDensity = Double.valueOf(memoryPackingDensity);
        submissionSettings.getPackingDensity().setMemory(memoryDensity);
        
        String networkPackingDensity = getProperty("submission.packingDensity.network");
        double networkDensity = Double.valueOf(networkPackingDensity);    
        submissionSettings.getPackingDensity().setNetwork(networkDensity);
    }

    /**
     * Set HTTPd settings.
     * 
     * @throws NodeConfiguratorException     The configuration exception
     */
    private void setHTTPdSettings() 
        throws NodeConfiguratorException 
    {
        HTTPdSettings httpdSettings = nodeConfiguration_.getHTTPd();
        String maximumNumberOfThreads = getProperty("httpd.maxNumberOfThreads");
        httpdSettings.setMaximumNumberOfThreads(maximumNumberOfThreads);
        
        String maximumNumberOfConnections = getProperty("httpd.maxNumberOfConnections");
        httpdSettings.setMaximumNumberOfConnections(maximumNumberOfConnections);
        
        String maxThreads = getProperty("httpd.maxThreads");
        httpdSettings.setMaxThreads(maxThreads);
        
        String minThreads = getProperty("httpd.minThreads");
        httpdSettings.setMinThreads(minThreads);
        
        String lowThreads = getProperty("httpd.lowThreads");
        httpdSettings.setLowThreads(lowThreads);
        
        String maxQueued = getProperty("httpd.maxQueued");
        httpdSettings.setMaxQueued(maxQueued);
        
        String maxIoIdleTimeMs = getProperty("httpd.maxIoIdleTimeMs");
        httpdSettings.setMaxIoIdleTimeMs(maxIoIdleTimeMs);

    }
    
    /**
     * Sets the hypervisor settings.
     * 
     * @throws NodeConfiguratorException    The configuration exception  
     */
    private void setHypervisorSettings() 
        throws NodeConfiguratorException
    {
        HypervisorSettings hypervisorSettings = nodeConfiguration_.getHypervisor();
        String hypervisorDriver = getProperty("hypervisor.driver");   
        hypervisorSettings.setDriver(HypervisorDriver.valueOf(hypervisorDriver));
        
        String hypervisorTransport = getProperty("hypervisor.transport");
        hypervisorSettings.setTransport(HypervisorTransport.valueOf(hypervisorTransport));
        
        String hypervisorPort = getProperty("hypervisor.port");        
        hypervisorSettings.setPort(Integer.valueOf(hypervisorPort));
        
        String migrationMethod = getProperty("hypervisor.migration.method");
        hypervisorSettings.getMigration().setMethod(MigrationMethod.valueOf(migrationMethod));
        
        String convergenceTimeout = getProperty("hypervisor.migration.timeout");
        hypervisorSettings.getMigration().setTimeout(Integer.valueOf(convergenceTimeout));
    }
    
    /**
     * Sets the database settings.
     * 
     * @throws NodeConfiguratorException 
     */
    private void setDatabaseSettings()
        throws NodeConfiguratorException 
    {
        DatabaseSettings databaseSettings = nodeConfiguration_.getDatabase();
        
        String databaseType = getProperty("database.type");
        databaseSettings.setType(DatabaseType.valueOf(databaseType));
        
        String numberOfGroupManagerEntries = getProperty("database.numberOfEntriesPerGroupManager");
        databaseSettings.setNumberOfEntriesPerGroupManager(Integer.valueOf(numberOfGroupManagerEntries));
        
        String numberOfVirtualMachineEntries = getProperty("database.numberOfEntriesPerVirtualMachine");
        databaseSettings.setNumberOfEntriesPerVirtualMachine(Integer.valueOf(numberOfVirtualMachineEntries));        
    }

    /**
     * Set leader election settings.
     * 
     * @throws NodeConfiguratorException    The configuration exception
     */
    private void setFaultToleranceSettings() 
        throws NodeConfiguratorException 
    {
        FaultToleranceSettings faultToleranceSettings = nodeConfiguration_.getFaultTolerance();
        String zooKeeperHosts = getProperty("faultTolerance.zookeeper.hosts");
        faultToleranceSettings.getZooKeeper().setHosts(zooKeeperHosts);
        
        String zooKeeperSessionTimeout = getProperty("faultTolerance.zookeeper.sessionTimeout");
        faultToleranceSettings.getZooKeeper().setSessionTimeout(Integer.valueOf(zooKeeperSessionTimeout));
        
        String heartbeatInterval = getProperty("faultTolerance.heartbeat.interval");   
        faultToleranceSettings.getHeartbeat().setInterval(Integer.valueOf(heartbeatInterval));
        
        String heartbeatTimeout = getProperty("faultTolerance.heartbeat.timeout"); 
        faultToleranceSettings.getHeartbeat().setTimeout(Integer.valueOf(heartbeatTimeout));
    }
            
    /**
     * Sets the utilization settings.
     * 
     * @throws NodeConfiguratorException    The configuration exception
     */
    private void setMonitoringSettings() 
        throws NodeConfiguratorException 
    {     
        String separator = ",";
        
        MonitoringSettings monitoringSettings = nodeConfiguration_.getMonitoring();
        String monitoringInterval = getProperty("monitoring.interval");   
        monitoringSettings.setInterval(Integer.valueOf(monitoringInterval));
        
        String monitoringTimeout = getProperty("monitoring.timeout");   
        monitoringSettings.setTimeout(Integer.valueOf(monitoringTimeout));
        
        String numberOfMonitoringEntries = getProperty("monitoring.numberOfMonitoringEntries"); 
        monitoringSettings.setNumberOfMonitoringEntries(Integer.valueOf(numberOfMonitoringEntries));
        
        String tmpUtilizationThresholds = getProperty("monitoring.thresholds.cpu"); 
   
        List<Double> cpuThresholds = StringUtils.convertStringToDoubleArray(tmpUtilizationThresholds, separator);
        tmpUtilizationThresholds = getProperty("monitoring.thresholds.memory"); 
        List<Double> memoryUtilizationThresholds = StringUtils.convertStringToDoubleArray(tmpUtilizationThresholds,
                                                                                          separator);        
        tmpUtilizationThresholds = getProperty("monitoring.thresholds.network");
        List<Double> networkUtilizationThresholds = StringUtils.convertStringToDoubleArray(tmpUtilizationThresholds, 
                                                                                           separator);        
        MonitoringThresholds monitoringThresholds = new MonitoringThresholds(cpuThresholds,
                                                                             memoryUtilizationThresholds,
                                                                             networkUtilizationThresholds);
        monitoringSettings.setThresholds(monitoringThresholds);
    }
    
    /**
     * Sets estimator settings.
     * 
     * @throws NodeConfiguratorException    The configuration exception
     */
    private void setEstimatorSettings()
        throws NodeConfiguratorException
    {
        EstimatorSettings estimatorSettings = nodeConfiguration_.getEstimator();       
        String isStatic = getProperty("estimator.static");
        estimatorSettings.setStatic(Boolean.valueOf(isStatic)); 
                
        String sortNorm = getProperty("estimator.sortNorm");
        estimatorSettings.setSortNorm(SortNorm.valueOf(sortNorm)); 
        
        String numberOfMonitoringEntries = getProperty("estimator.numberOfMonitoringEntries");
        estimatorSettings.setNumberOfMonitoringEntries(Integer.valueOf(numberOfMonitoringEntries));
                
        String cpuDemandEstimator = getProperty("estimator.policy.cpu");
        estimatorSettings.getPolicy().setCPU(Estimator.valueOf(cpuDemandEstimator)); 
        
        String memoryDemandEstimator = getProperty("estimator.policy.memory");
        estimatorSettings.getPolicy().setMemory(Estimator.valueOf(memoryDemandEstimator));
        
        String networkDemandEstimator = getProperty("estimator.policy.network");
        estimatorSettings.getPolicy().setNetwork(Estimator.valueOf(networkDemandEstimator));
    }
    
    /**
     * Set the group leader scheduling settings.
     * 
     * @throws NodeConfiguratorException    The configuration exception
     */
    private void setGroupLeaderSchedulerSettings()
        throws NodeConfiguratorException
    {
        GroupLeaderSchedulerSettings groupLeader = nodeConfiguration_.getGroupLeaderScheduler();
        String assignmentPolicy = getProperty("groupLeaderScheduler.assignmentPolicy"); 
        groupLeader.setAssignmentPolicy(Assignment.valueOf(assignmentPolicy));
        
        String dispatchingPolicy = getProperty("groupLeaderScheduler.dispatchingPolicy");
        groupLeader.setDispatchingPolicy(Dispatching.valueOf(dispatchingPolicy));
    }

    /**
     * Set the group manager scheduling settings.
     * 
     * @throws NodeConfiguratorException    The configuration exception
     */
    private void setGroupManagerSchedulerSettings()
        throws NodeConfiguratorException
    {
        GroupManagerSchedulerSettings groupManager = nodeConfiguration_.getGroupManagerScheduler();
        String placementPolicy = getProperty("groupManagerScheduler.placementPolicy");   
        groupManager.setPlacementPolicy(Placement.valueOf(placementPolicy));
        
        String overloadPolicy = getProperty("groupManagerScheduler.relocation.overloadPolicy");
        groupManager.getRelocationSettings().setOverloadPolicy(Relocation.valueOf(overloadPolicy));
                
        String underloadPolicy = getProperty("groupManagerScheduler.relocation.underloadPolicy");   
        groupManager.getRelocationSettings().setUnderloadPolicy(Relocation.valueOf(underloadPolicy));
        
        String isEnabled = getProperty("groupManagerScheduler.reconfiguration.enabled"); 
        groupManager.getReconfigurationSettings().setEnabled(Boolean.valueOf(isEnabled));
        
        String policy = getProperty("groupManagerScheduler.reconfiguration.policy"); 
        groupManager.getReconfigurationSettings().setPolicy(Reconfiguration.valueOf(policy));
        
        String interval = getProperty("groupManagerScheduler.reconfiguration.interval");
        groupManager.getReconfigurationSettings().setInterval(interval);
    }
    
    /**
     * Sets the energy management settings.
     * 
     * @throws NodeConfiguratorException    The configuration exception
     */
    private void setEnergyManagementSettings() 
        throws NodeConfiguratorException 
    {       
        EnergyManagementSettings energyManagement = nodeConfiguration_.getEnergyManagement();
        String isEnabled = getProperty("energyManagement.enabled");  
        energyManagement.setEnabled(Boolean.valueOf(isEnabled));     
        
        String commandExecutionTimeout = getProperty("energyManagement.commandExecutionTimeout");  
        energyManagement.setCommandExecutionTimeout(Integer.valueOf(commandExecutionTimeout));
        
        String numberOfReservedNodes = getProperty("energyManagement.numberOfReservedNodes");
        energyManagement.setNumberOfReservedNodes(Integer.valueOf(numberOfReservedNodes));
                
        String idleTimeThreshold = getProperty("energyManagement.thresholds.idleTime");  
        energyManagement.getThresholds().setIdleTime(Integer.valueOf(idleTimeThreshold));  
        
        String wakeupTimeThreshold = getProperty("energyManagement.thresholds.wakeupTime");  
        energyManagement.getThresholds().setWakeupTime(Integer.valueOf(wakeupTimeThreshold));    
        
        String powerSavingAction = getProperty("energyManagement.powerSavingAction"); 
        energyManagement.setPowerSavingAction(PowerSavingAction.valueOf(powerSavingAction));
                
        String shutdownDriver = getProperty("energyManagement.drivers.shutdown"); 
        energyManagement.getDrivers().setShutdown(ShutdownDriver.valueOf(shutdownDriver));
        
        String suspendDriver = getProperty("energyManagement.drivers.suspend"); 
        energyManagement.getDrivers().setSuspend(SuspendDriver.valueOf(suspendDriver));
        
        String wakeupDriver = getProperty("energyManagement.drivers.wakeup"); 
        energyManagement.getDrivers().getWakeup().setDriver(WakeupDriver.valueOf(wakeupDriver));
        
        String wakeupOptions = getProperty("energyManagement.drivers.wakeup.options");
        energyManagement.getDrivers().getWakeup().setOptions(wakeupOptions);
    }

    /** 
     * Returns the node configuration.
     *  
     * @return     Node params instance
     */
    public NodeConfiguration getNodeConfiguration() 
    {
        return nodeConfiguration_;
    }
    
    /**
     * Returns the content of a properties.
     * 
     * @param tag                           The tag
     * @return                              The content string
     * @throws NodeConfiguratorException    The configuration exception
     */
    private String getProperty(String tag) 
        throws NodeConfiguratorException
    {
        String content = properties_.getProperty(tag);
        if (content == null) 
        {
            throw new NodeConfiguratorException(String.format("%s entry is missing", tag));
        }
        
        content = content.trim();
        return content;             
    }
}
