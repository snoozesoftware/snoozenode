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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.NodeRole;
import org.inria.myriads.snoozecommon.communication.localcontroller.MonitoringThresholds;
import org.inria.myriads.snoozecommon.communication.localcontroller.Resource;
import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.HypervisorDriver;
import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.HypervisorSettings;
import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.HypervisorTransport;
import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.MigrationMethod;
import org.inria.myriads.snoozecommon.communication.localcontroller.wakeup.WakeupDriver;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.util.NetworkUtils;
import org.inria.myriads.snoozecommon.util.StringUtils;
import org.inria.myriads.snoozenode.configurator.anomaly.AnomalyDetectorSettings;
import org.inria.myriads.snoozenode.configurator.anomaly.AnomalyResolverSettings;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.configurator.api.NodeConfigurator;
import org.inria.myriads.snoozenode.configurator.database.DatabaseSettings;
import org.inria.myriads.snoozenode.configurator.energymanagement.EnergyManagementSettings;
import org.inria.myriads.snoozenode.configurator.energymanagement.enums.PowerSavingAction;
import org.inria.myriads.snoozenode.configurator.energymanagement.enums.ShutdownDriver;
import org.inria.myriads.snoozenode.configurator.energymanagement.enums.SuspendDriver;
import org.inria.myriads.snoozenode.configurator.estimator.EstimatorSettings;
import org.inria.myriads.snoozenode.configurator.estimator.HostEstimatorSettings;
import org.inria.myriads.snoozenode.configurator.faulttolerance.FaultToleranceSettings;
import org.inria.myriads.snoozenode.configurator.httpd.HTTPdSettings;
import org.inria.myriads.snoozenode.configurator.imagerepository.DiskHostingType;
import org.inria.myriads.snoozenode.configurator.imagerepository.ImageRepositorySettings;
import org.inria.myriads.snoozenode.configurator.monitoring.EstimatorPolicySettings;
import org.inria.myriads.snoozenode.configurator.monitoring.HostMonitorSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.HostMonitorType;
import org.inria.myriads.snoozenode.configurator.monitoring.HostMonitoringSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.MonitoringSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.external.ExternalNotifierSettings;
import org.inria.myriads.snoozenode.configurator.networking.NetworkingSettings;
import org.inria.myriads.snoozenode.configurator.node.NodeSettings;
import org.inria.myriads.snoozenode.configurator.provisioner.ImageDiskSettings;
import org.inria.myriads.snoozenode.configurator.provisioner.ProvisionerSettings;
import org.inria.myriads.snoozenode.configurator.provisioner.VncSettings;
import org.inria.myriads.snoozenode.configurator.scheduler.GroupLeaderSchedulerSettings;
import org.inria.myriads.snoozenode.configurator.scheduler.GroupManagerSchedulerSettings;
import org.inria.myriads.snoozenode.configurator.submission.SubmissionSettings;
import org.inria.myriads.snoozenode.database.enums.DatabaseType;
import org.inria.myriads.snoozenode.exception.NodeConfiguratorException;
import org.inria.myriads.snoozenode.groupmanager.estimator.enums.Estimator;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.enums.Assignment;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.enums.Dispatching;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.enums.Reconfiguration;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.enums.Relocation;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.sort.SortNorm;
import org.inria.myriads.snoozenode.idgenerator.enums.IdGeneration;
import org.inria.myriads.snoozenode.monitoring.TransportProtocol;

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
        setMonitoringExternalSettings();
        setEstimatorSettings();
        setGroupLeaderSchedulerSettings();
        setGroupManagerSchedulerSettings();
        setSubmissionSettings();
        setEnergyManagementSettings();
        setImageRepositorySettings();
        setProvisionerSettings();
        setHostMonitoringSettings();
        setAnomalyDetectorSettings();
        setAnomalyResolverSettings();
        
        fileInput.close();
    }
    


    private void setAnomalyDetectorSettings() throws NodeConfiguratorException
    {
        AnomalyDetectorSettings anomalyDetectorSettings = nodeConfiguration_.getAnomalyDetectorSettings();
        
        boolean  isEnabled = Boolean.valueOf(getProperty("localController.anomaly.detector.enable", "false"));
        anomalyDetectorSettings.setEnabled(isEnabled);
        
        String name = getProperty("localController.anomaly.detector");
        anomalyDetectorSettings.setName(name);
        
        String numberOfEntries = getProperty("localController.anomaly.detector.numberOfMonitoringEntries");
        anomalyDetectorSettings.setNumberOfMonitoringEntries(Integer.valueOf(numberOfEntries));
        
        String interval = getProperty("localController.anomaly.detector.interval");
        anomalyDetectorSettings.setInterval(Integer.valueOf(interval));
        
        String options = getProperty("localController.anomaly.detector.options");
        Map<String, String> map = umarshal(options);
        anomalyDetectorSettings.setOptions(map);
        
    }
    
    private void setAnomalyResolverSettings() throws NodeConfiguratorException
    {
        AnomalyResolverSettings anomalyResolverSettings = nodeConfiguration_.getAnomalyResolverSettings();
        String name = getProperty("groupManager.anomaly.resolver");
        anomalyResolverSettings.setName(name);
        
        String numberOfEntries = getProperty("groupManager.anomaly.resolver.numberOfMonitoringEntries");
        anomalyResolverSettings.setNumberOfMonitoringEntries(Integer.valueOf(numberOfEntries));
        
        String options = getProperty("groupManager.anomaly.resolver.options");
        Map<String, String> map = umarshal(options);
        anomalyResolverSettings.setOptions(map);
        
    }



    private Map<String, String> umarshal(String options) throws NodeConfiguratorException
    {
        Map<String,String> map = new HashMap<String,String>();
        ObjectMapper mapper = new ObjectMapper();
        try
        {
            map = mapper.readValue(options, 
                    new TypeReference<HashMap<String,String>>(){});
        }
        catch (IOException e)
        {
            throw new NodeConfiguratorException(e.getMessage());
        }
        return map;
    }



    private void setHostMonitoringSettings() throws NodeConfiguratorException
    {
        String separator = ",";
        HostMonitoringSettings hostMonitoringSettings = nodeConfiguration_.getHostMonitoringSettings();
        String stringHostMonitorTypes = getProperty("localController.hostmonitor.type");
        String hostMonitors[] = stringHostMonitorTypes.split(separator);
        for (String hostMonitor : hostMonitors)
        {
            HostMonitorSettings hostMonitorSettings = new HostMonitorSettings();
            // type
            HostMonitorType type = HostMonitorType.valueOf(hostMonitor);
            hostMonitorSettings.setType(type);
            
            String options = getProperty(buildHostMonitorProperty("localController.hostmonitor", hostMonitor.toLowerCase(), "options"));
            Map<String, String> map = umarshal(options);
            hostMonitorSettings.setOptions(map);
            
            // default numberOfMonitoringEntries
            String defaultNumberOfMonitoringEntries = getProperty(buildHostMonitorProperty("localController.hostmonitor", hostMonitor.toLowerCase(), "numberOfMonitoringEntries"), "10");
            //default interval 
            String defaultInterval = getProperty(buildHostMonitorProperty("localController.hostmonitor", hostMonitor.toLowerCase(), "interval"), "3000");
            hostMonitorSettings.setInterval(Integer.valueOf(defaultInterval));
            //default threshold
            String stringDefaultThresholds = getProperty(buildHostMonitorProperty("localController.hostmonitor", hostMonitor.toLowerCase(), "thresholds"), "0,1,1"); 
            // default estimator
            String stringDefaultEstimator =  getProperty(buildHostMonitorProperty("localController.hostmonitor", hostMonitor.toLowerCase(), "estimator"), "average"); 

            // published metrics
            String stringPublished = getProperty(buildHostMonitorProperty("localController.hostmonitor", hostMonitor.toLowerCase(), "published"));
            String[] published = stringPublished.split(separator);
            for (String resourceName : published)
            {
                 //number of monitoring entries.
                int localNumberOfMonitoringEntries = 0;
                String stringLocalNumberOfMonitoringEntries = 
                         getProperty(buildHostMonitorProperty("localController.hostmonitor", hostMonitor.toLowerCase(), "numberOfMonitoringEntries", resourceName), defaultNumberOfMonitoringEntries);
                localNumberOfMonitoringEntries = Integer.valueOf(stringLocalNumberOfMonitoringEntries);
                 
                 
                String stringLocalThresholds = 
                         getProperty(buildHostMonitorProperty("localController.hostmonitor", hostMonitor.toLowerCase(), "thresholds", resourceName), stringDefaultThresholds);
                 
                String stringLocalEstimator = 
                        getProperty(buildHostMonitorProperty("localController.hostmonitor", hostMonitor.toLowerCase(), "estimator", resourceName), stringDefaultThresholds);
                
                List<Double> thresholds = StringUtils.convertStringToDoubleArray(stringLocalThresholds, separator);
                 
                Resource resource = new Resource(localNumberOfMonitoringEntries);
                resource.setName(resourceName);
                resource.setThresholds(thresholds);
                //register the resource settings.
                hostMonitorSettings.add(resource);
                //register the estimator
                hostMonitorSettings.add(resourceName, new HostEstimatorSettings(stringDefaultEstimator));
            }
            //register the host monitor setting (and all its resource)
            hostMonitoringSettings.add(type, hostMonitorSettings);
        }
    }

    private String buildHostMonitorProperty(String ... strings)
    {
        String result = "";
        int i = 0; 
        for (String string : strings)
        {   
            if (i == 0)
            {
                result += string;
            }
            else
            {
                result += "." + string;
            }
            i++;
        }
        return result;
        
    }


    /**
     * 
     * Sets the provisioner settings.
     * 
     * @throws NodeConfiguratorException    Node configuration exception.
     */
    private void setProvisionerSettings() throws NodeConfiguratorException
    {
        ProvisionerSettings provisionerSettings = nodeConfiguration_.getProvisionerSettings();
        
        /** Serial */
        String enableSerial = getProperty("provisioner.serial.enable");
        provisionerSettings.setEnableSerial(Boolean.valueOf(enableSerial));
        
        /** VNC */
        VncSettings vncSettings = new VncSettings();
        String enableVnc = getProperty("provisioner.vnc.enable");
        vncSettings.setEnableVnc(Boolean.valueOf(enableVnc));
        String listenAddress = getProperty("provisioner.vnc.listenAddress");
        vncSettings.setListenAddress(listenAddress);
        String vncStartPort  = getProperty("provisioner.vnc.startPort");
        vncSettings.setStartPort(Integer.valueOf(vncStartPort));
        String vncPortRange = getProperty("provisioner.vnc.portRange");
        vncSettings.setVncPortRange(Integer.valueOf(vncPortRange));
        String keymap = getProperty("provisioner.vnc.keymap");
        vncSettings.setKeymap(keymap);
        provisionerSettings.setVncSettings(vncSettings);

        
        /** First hd*/
        ImageDiskSettings imageDiskSettings = new ImageDiskSettings();
        String diskBusType = getProperty("provisioner.disk.bus");
        imageDiskSettings.setDiskBusType(diskBusType);
        String diskDevice = getProperty("provisioner.disk.dev");
        imageDiskSettings.setDiskDevice(diskDevice);
        provisionerSettings.setFirstHdSettings(imageDiskSettings);
        
        /** Context cd*/
        ImageDiskSettings contextDiskSettings = new ImageDiskSettings();
        diskBusType = getProperty("provisioner.contextDisk.bus");
        contextDiskSettings.setDiskBusType(diskBusType);
        diskDevice = getProperty("provisioner.contextDisk.dev");
        contextDiskSettings.setDiskDevice(diskDevice);
        provisionerSettings.setFirstCdSettings(contextDiskSettings);
        
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
        
        String idGenerator = getProperty("node.idgenerator");
        nodeSettings.setIdGenerator(IdGeneration.valueOf(idGenerator));
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
        String separator = ",";
        DatabaseSettings databaseSettings = nodeConfiguration_.getDatabase();
        
        String databaseType = getProperty("database.type");
        databaseSettings.setType(DatabaseType.valueOf(databaseType));
        
        String numberOfGroupManagerEntries = getProperty("database.numberOfEntriesPerGroupManager");
        databaseSettings.setNumberOfEntriesPerGroupManager(Integer.valueOf(numberOfGroupManagerEntries));
        
        String numberOfVirtualMachineEntries = getProperty("database.numberOfEntriesPerVirtualMachine");
        databaseSettings.setNumberOfEntriesPerVirtualMachine(Integer.valueOf(numberOfVirtualMachineEntries));
        
        String cassandraHosts = getProperty("database.cassandra.hosts");        
        databaseSettings.getCassandraSettings().setHosts(cassandraHosts);
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
     * 
     * Set the monitoring settings.
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
            List<Double> networkUtilizationThresholds = 
                    StringUtils.convertStringToDoubleArray(tmpUtilizationThresholds, 
                                                                                               separator);        
            MonitoringThresholds monitoringThresholds = new MonitoringThresholds(cpuThresholds,
                                                                                 memoryUtilizationThresholds,
                                                                                 networkUtilizationThresholds);
            
            monitoringSettings.setThresholds(monitoringThresholds);
            
            
            // estimators.
            String cpuDemandEstimator = getProperty("monitoring.estimator.cpu"); 
            String memoryDemandEstimator = getProperty("monitoring.estimator.memory");
            String networkDemandEstimator = getProperty("monitoring.estimator.network");
            
            EstimatorPolicySettings policySettings = new EstimatorPolicySettings();
            policySettings.setCpuEstimatorName(cpuDemandEstimator);
            policySettings.setMemoryEstimatorName(memoryDemandEstimator);
            policySettings.setNetworkEstimatorName(networkDemandEstimator);
            
            monitoringSettings.setEstimatorPolicy(policySettings);
        }    
    /**
     * Sets the utilization settings.
     * 
     * @throws NodeConfiguratorException    The configuration exception
     */
    private void setMonitoringExternalSettings() 
        throws NodeConfiguratorException 
    {     
        ExternalNotifierSettings monitoringExternalSettings = nodeConfiguration_.getExternalNotifier();
        String transport = getProperty("external.notifier.transport");
        monitoringExternalSettings.setTransportProtocol(TransportProtocol.valueOf(transport));
        
        String address = getProperty("external.notifier.address");
        int port = Integer.valueOf(getProperty("external.notifier.port"));
        NetworkAddress sendDataAddress = NetworkUtils.createNetworkAddress(address, port);
        monitoringExternalSettings.setAddress(sendDataAddress);
        
        String username = getProperty("external.notifier.username");
        String password = getProperty("external.notifier.password");
        String vhost = getProperty("external.notifier.vhost");
        int numberOfRetries = Integer.valueOf(getProperty("external.notifier.faultTolerance.numberOfRetries"));
        int retryInterval = Integer.valueOf(getProperty("external.notifier.faultTolerance.retryInterval"));
        monitoringExternalSettings.setUsername(username);
        monitoringExternalSettings.setPassword(password);
        monitoringExternalSettings.setVhost(vhost);
        monitoringExternalSettings.setNumberOfRetries(numberOfRetries);
        monitoringExternalSettings.setRetryInterval(retryInterval);
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
        String name = getProperty("estimator");
        estimatorSettings.setName(name);
        
        String optionsString = getProperty("estimator.options", "{}");
        Map<String, String> options = umarshal(optionsString);
        estimatorSettings.setOptions(options);
        
//        String isStatic = getProperty("estimator.static");
//        estimatorSettings.setStatic(Boolean.valueOf(isStatic)); 
//                
//        String sortNorm = getProperty("estimator.sortNorm");
//        estimatorSettings.setSortNorm(SortNorm.valueOf(sortNorm)); 
        
        String numberOfMonitoringEntries = getProperty("estimator.numberOfMonitoringEntries");
        estimatorSettings.setNumberOfMonitoringEntries(Integer.valueOf(numberOfMonitoringEntries));

        // TODO move this to monitoring.estimator
//        String cpuDemandEstimator = getProperty("estimator.policy.cpu");
//        estimatorSettings.getPolicy().setCPU(Estimator.valueOf(cpuDemandEstimator)); 
//        
//        String memoryDemandEstimator = getProperty("estimator.policy.memory");
//        estimatorSettings.getPolicy().setMemory(Estimator.valueOf(memoryDemandEstimator));
//        
//        String networkDemandEstimator = getProperty("estimator.policy.network");
//        estimatorSettings.getPolicy().setNetwork(Estimator.valueOf(networkDemandEstimator));
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
        groupManager.setPlacementPolicy(String.valueOf(placementPolicy));
        
        String pluginsDirectory = getProperty("groupManagerScheduler.pluginsDirectory");
        groupManager.setPluginsDirectory(pluginsDirectory);
        
//        String overloadPolicy = getProperty("groupManagerScheduler.relocation.overloadPolicy");
//        groupManager.getRelocationSettings().setOverloadPolicy(Relocation.valueOf(overloadPolicy));
//                
//        String underloadPolicy = getProperty("groupManagerScheduler.relocation.underloadPolicy");   
//        groupManager.getRelocationSettings().setUnderloadPolicy(Relocation.valueOf(underloadPolicy));
        
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
     * 
     * Sets the image repository settings.
     * 
     * @throws NodeConfiguratorException    Node configuration exception.
     */
    private void setImageRepositorySettings() throws NodeConfiguratorException
    {
        ImageRepositorySettings imageRepositorySettings = 
                nodeConfiguration_.getImageRepositorySettings();
        
        String address = getProperty("imageRepository.address");
        String port = getProperty("imageRepository.port");
        NetworkAddress imageRepositoryAddress = new NetworkAddress();
        imageRepositoryAddress.setAddress(address);
        imageRepositoryAddress.setPort(Integer.valueOf(port));
        imageRepositorySettings.setImageRepositoryAddress(imageRepositoryAddress);
        
        String diskHostingType = getProperty("imageRepository.manager.disks");
        imageRepositorySettings.setDiskType(DiskHostingType.valueOf(diskHostingType));
        
        String source = getProperty("imageRepository.manager.source");
        imageRepositorySettings.setSource(source);
        String destination = getProperty("imageRepository.manager.destination");
        imageRepositorySettings.setDestination(destination);
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
    
    /**
     * Returns the content of a properties.
     * 
     * @param tag                           The tag
     * @return                              The content string
     * @throws NodeConfiguratorException    The configuration exception
     */
    private String getProperty(String tag, String defaultValue) 
        throws NodeConfiguratorException
    {
        String content = properties_.getProperty(tag);
        if (content == null) 
        {
           content = defaultValue;
        }
        
        content = content.trim();
        return content;             
    }
    
}
