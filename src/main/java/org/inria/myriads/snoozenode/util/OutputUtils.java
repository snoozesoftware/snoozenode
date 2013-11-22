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
package org.inria.myriads.snoozenode.util;

import java.util.List;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.VirtualMachineMonitoringData;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.util.MonitoringUtils;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Output utility.
 * 
 * @author Eugen Feller
 */
public final class OutputUtils 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(OutputUtils.class);
    
    /**
     * Hide the consturctor.
     */
    private OutputUtils() 
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Prints the local controllers.
     * 
     * @param localControllers       The local controller descriptions
     */
    public static void printLocalControllers(List<LocalControllerDescription> localControllers)
    {
        Guard.check(localControllers);
        log_.debug("Printing local controllers");
        
        for (LocalControllerDescription description : localControllers)
        {
            log_.debug(String.format("Local controller %s is %s with total capacity: %s", 
                                     description.getId(), 
                                     description.getStatus(),
                                     description.getTotalCapacity()));
        }
    }
    
    /**
     * Prints the virtual machines.
     * 
     * @param virtualMachines       The virtual machines
     */
    public static void printVirtualMachines(List<VirtualMachineMetaData> virtualMachines)
    {
        Guard.check(virtualMachines);
        log_.debug("Printing virtual machines");
        
        for (VirtualMachineMetaData virtualMachine : virtualMachines)
        {
            String virtualMachineId = virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
            Map<Long, VirtualMachineMonitoringData> usedCapacity = virtualMachine.getUsedCapacity();
            if (usedCapacity.size() == 0)
            {
                log_.debug(String.format("No monitoring data available on virtual machine: %s", virtualMachineId));
                continue;
            }
            
            VirtualMachineMonitoringData monitoringData = 
                MonitoringUtils.getLatestVirtualMachineMonitoringData(usedCapacity);
            log_.debug(String.format("Virtual machine id: %s, requested capacity: %s, latest used capacity: %s", 
                                     virtualMachineId,
                                     virtualMachine.getRequestedCapacity(),
                                     monitoringData.getUsedCapacity()));
        }
    }
    
    /**
     * Display node parameterss.
     * 
     * @param configuration     The node configuration
     */
    public static void printNodeConfiguration(NodeConfiguration configuration)
    {
        Guard.check(configuration);
        
        log_.debug("------------------ System configuration -------------");
        log_.debug("Node settings:");
        log_.debug("-----------------");
        log_.debug(String.format("node.role: %s", 
                                 configuration.getNode().getRole()));
        log_.debug(String.format("node.networkCapacity.Rx: %s",     
                                 configuration.getNode().getNetworkCapacity().getRxBytes()));
        log_.debug(String.format("node.networkCapacity.Tx: %s",     
                                 configuration.getNode().getNetworkCapacity().getTxBytes()));
        log_.debug("--------------------");
        log_.debug("Networking settings:");
        log_.debug("--------------------");
        log_.debug(String.format("network.listen.address: %s",
                                 configuration.getNetworking().getListen().getControlDataAddress().getAddress()));
        log_.debug(String.format("network.listen.controlDataPort: %s",
                                 configuration.getNetworking().getListen().getControlDataAddress().getPort()));
        log_.debug(String.format("network.listen.monitoringDataPort: %s",
                                 configuration.getNetworking().getListen().getMonitoringDataAddress().getPort()));
        log_.debug(String.format("network.multicast.address: %s",
                   configuration.getNetworking().getMulticast().getGroupLeaderHeartbeatAddress().getAddress()));
        log_.debug(String.format("network.multicast.groupLeaderHeartbeatPort: %s",
                   configuration.getNetworking().getMulticast().getGroupLeaderHeartbeatAddress().getPort()));
        log_.debug(String.format("network.multicast.groupManagerHeartbeatPort: %s",
                   configuration.getNetworking().getMulticast().getGroupManagerHeartbeatAddress().getPort()));
        log_.debug("--------------------");
        log_.debug("HTTPd settings:");
        log_.debug("--------------------");
        log_.debug(String.format("httpd.maxNumberOfThread: %s",
                                 configuration.getHTTPd().getMaximumNumberOfThreads()));
        log_.debug(String.format("httpd.maxNumberOfConnections: %s",
                                 configuration.getHTTPd().getMaximumNumberOfConnections()));
        log_.debug("-------------------------");
        log_.debug("Fault tolerance settings:");
        log_.debug("-------------------------");
        log_.debug(String.format("faultTolerance.zookeeper.hosts: %s",     
                                 configuration.getFaultTolerance().getZooKeeper().getHosts()));
        log_.debug(String.format("faultTolerance.zookeeper.sessionTimeout: %d", 
                                 configuration.getFaultTolerance().getZooKeeper().getSessionTimeout()));
        log_.debug(String.format("faultTolerance.heartbeat.interval: %d",
                                 configuration.getFaultTolerance().getHeartbeat().getInterval()));
        log_.debug(String.format("faultTolerance.heartbeat.timeout: %d",
                                 configuration.getFaultTolerance().getHeartbeat().getTimeout()));
        log_.debug("--------------------");
        log_.debug("Hypervisor settings:");
        log_.debug("--------------------");
        log_.debug(String.format("hypervisor.driver: %s",
                                 configuration.getHypervisor().getDriver()));
        log_.debug(String.format("hypervisor.transport: %s",
                                 configuration.getHypervisor().getTransport()));
        log_.debug(String.format("hypervisor.port: %d",
                                 configuration.getHypervisor().getPort()));
        log_.debug(String.format("hypervisor.migration.method: %s", 
                                 configuration.getHypervisor().getMigration().getMethod()));
        log_.debug(String.format("hypervisor.migration.timeout: %s", 
                                 configuration.getHypervisor().getMigration().getTimeout()));
        log_.debug("--------------------");
        log_.debug("Database settings:");
        log_.debug("--------------------");
        log_.debug(String.format("database.type: %s", configuration.getDatabase().getType()));
        log_.debug(String.format("database.numberOfEntriesPerGroupManager: %s", 
                                 configuration.getDatabase().getNumberOfEntriesPerGroupManager()));
        log_.debug(String.format("database.numberOfEntriesPerVirtualMachine: %s", 
                                 configuration.getDatabase().getNumberOfEntriesPerVirtualMachine()));
        log_.debug("-----------------------");
        log_.debug("Monitoring settings:");
        log_.debug("-----------------------");
        log_.debug(String.format("monitoring.interval: %d",
                                 configuration.getMonitoring().getInterval()));
        log_.debug(String.format("monitoring.timeout: %d",
                                 configuration.getMonitoring().getTimeout()));
        log_.debug(String.format("monitoring.numberOfMonitoringEntries: %d",
                                 configuration.getMonitoring().getNumberOfMonitoringEntries()));
        log_.debug(String.format("monitoring.thresholds.cpu: %s",
                                 configuration.getMonitoring().getThresholds().getCPU()));
        log_.debug(String.format("monitoring.thresholds.memory: %s",
                                 configuration.getMonitoring().getThresholds().getMemory()));
        log_.debug(String.format("monitoring.thresholds.network: %s",
                                 configuration.getMonitoring().getThresholds().getNetwork())); 
        log_.debug("-------------------");
        log_.debug("Estimation settings:");
        log_.debug("-------------------");
        log_.debug(String.format("estimator.static: %s", 
                                 configuration.getEstimator().isStatic())); 
        log_.debug(String.format("estimator.sortNorm: %s",
                                 configuration.getEstimator().getSortNorm()));
        log_.debug(String.format("estimator.numberOfMonitoringEntries: %s",
                                 configuration.getEstimator().getNumberOfMonitoringEntries()));
        log_.debug(String.format("estimator.policy.cpu: %s",
                                 configuration.getEstimator().getPolicy().getCPU()));
        log_.debug(String.format("estimator.policy.cpu: %s",
                                 configuration.getEstimator().getPolicy().getMemory()));
        log_.debug(String.format("estimator.policy.memory: %s",
                                 configuration.getEstimator().getPolicy().getNetwork()));
        log_.debug("--------------------------------");
        log_.debug("Group leader scheduler settings:");
        log_.debug("--------------------------------");
        log_.debug(String.format("groupLeaderScheduler.assignmentPolicy: %s",
                                 configuration.getGroupLeaderScheduler().getAssignmentPolicy()));
        log_.debug(String.format("groupLeaderScheduler.dispatchingPolicy: %s",
                                 configuration.getGroupLeaderScheduler().getDispatchingPolicy()));        
        log_.debug("---------------------------------");
        log_.debug("Group manager scheduler settings:");
        log_.debug("---------------------------------");
        log_.debug(String.format("groupManagerScheduler.placementPolicy: %s",
                                 configuration.getGroupManagerScheduler().getPlacementPolicy()));
        log_.debug(String.format("groupManagerScheduler.relocation.overloadRelocationPolicy: %s",
                   configuration.getGroupManagerScheduler().getRelocationSettings().getOverloadPolicy()));
        log_.debug(String.format("groupManagerScheduler.relocation.underloadRelocationPolicy: %s",
                   configuration.getGroupManagerScheduler().getRelocationSettings().getUnderloadPolicy()));
        log_.debug(String.format("groupManagerScheduler.reconfiguration.enabled: %s", 
                                 configuration.getGroupManagerScheduler().getReconfigurationSettings().isEnabled()));
        log_.debug(String.format("groupManagerScheduler.reconfiguration.policy: %s",
                                 configuration.getGroupManagerScheduler().getReconfigurationSettings().getPolicy()));
        log_.debug(String.format("groupManagerScheduler.reconfiguration.interval: %s", 
                                configuration.getGroupManagerScheduler().getReconfigurationSettings().getInterval()));
        log_.debug("---------------------------");
        log_.debug("Energy management settings:");
        log_.debug("---------------------------");
        log_.debug(String.format("energyManagement.enableds: %s",
                                 configuration.getEnergyManagement().isEnabled()));
        log_.debug(String.format("energyManagement.numberOfReservedNodes: %d",
                                 configuration.getEnergyManagement().getNumberOfReservedNodes())); 
        log_.debug(String.format("energyManagement.powerSavingAction: %s",
                                 configuration.getEnergyManagement().getPowerSavingAction()));     
        log_.debug(String.format("energyManagement.drivers.shutdown: %s",
                                 configuration.getEnergyManagement().getDrivers().getShutdown()));     
        log_.debug(String.format("energyManagement.drivers.suspend: %s",
                                 configuration.getEnergyManagement().getDrivers().getSuspend()));     
        log_.debug(String.format("energyManagement.drivers.wakeup: %s",
                                 configuration.getEnergyManagement().getDrivers().getWakeup().getDriver())); 
        log_.debug(String.format("energyManagement.drivers.wakeup.options: %s",
                                 configuration.getEnergyManagement().getDrivers().getWakeup().getOptions()));
        log_.debug(String.format("energyManagement.thresholds.idleTime: %d",
                                 configuration.getEnergyManagement().getThresholds().getIdleTime()));
        log_.debug(String.format("energyManagement.thresholds.wakeupTime: %d",
                                 configuration.getEnergyManagement().getThresholds().getWakeupTime()));
        log_.debug(String.format("energyManagement.commandExecutionTimeout: %d",
                                 configuration.getEnergyManagement().getCommandExecutionTimeout())); 
        log_.debug("-----------------------");
        log_.debug("Provisioner Settings");
        log_.debug("-----------------------");
        log_.debug(String.format("provisioner.contextDisk.bus: %s",
                configuration.getProvisionerSettings().getFirstCdSettings().getDiskBusType()));
        log_.debug(String.format("provisioner.contextDisk.dev: %s",
                configuration.getProvisionerSettings().getFirstCdSettings().getDiskDevice()));
        log_.debug(String.format("provisioner.disk.bus: %s",
                configuration.getProvisionerSettings().getFirstHdSettings().getDiskBusType()));
        log_.debug(String.format("provisioner.disk.dev: %s",
                configuration.getProvisionerSettings().getFirstHdSettings().getDiskDevice()));
        log_.debug(String.format("provisioner.serial.enable: %s",
                configuration.getProvisionerSettings().isEnableSerial()));
        log_.debug(String.format("provisioner.vnc.enable: %s",
                configuration.getProvisionerSettings().getVncSettings().isEnableVnc()));
        log_.debug(String.format("provisioner.vnc.listenAdddress: %s",
                configuration.getProvisionerSettings().getVncSettings().getListenAddress()));
        log_.debug(String.format("provisioner.vnc.startPort: %s",
                configuration.getProvisionerSettings().getVncSettings().getStartPort()));
        log_.debug(String.format("provisioner.vnc.portRange: %s",
                configuration.getProvisionerSettings().getVncSettings().getVncPortRange()));
        log_.debug(String.format("provisioner.vnc.keymap: %s",
                configuration.getProvisionerSettings().getVncSettings().getKeymap()));
    }   
    

}
