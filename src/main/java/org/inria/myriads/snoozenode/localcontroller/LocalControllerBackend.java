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
package org.inria.myriads.snoozenode.localcontroller;

import java.util.ArrayList;
import java.util.HashMap;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.AssignedGroupManager;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerLocation;
import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.HypervisorSettings;
import org.inria.myriads.snoozecommon.communication.rest.CommunicatorFactory;
import org.inria.myriads.snoozecommon.communication.rest.api.GroupManagerAPI;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.NetworkDemand;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.configurator.energymanagement.enums.PowerSavingAction;
import org.inria.myriads.snoozenode.configurator.energymanagement.enums.ShutdownDriver;
import org.inria.myriads.snoozenode.configurator.energymanagement.enums.SuspendDriver;
import org.inria.myriads.snoozenode.configurator.monitoring.external.ExternalNotifierSettings;
import org.inria.myriads.snoozenode.database.DatabaseFactory;
import org.inria.myriads.snoozenode.database.api.LocalControllerRepository;
import org.inria.myriads.snoozenode.database.enums.DatabaseType;
import org.inria.myriads.snoozenode.exception.HostMonitoringException;
import org.inria.myriads.snoozenode.exception.VirtualMachineMonitoringException;
import org.inria.myriads.snoozenode.executor.ShellCommandExecuter;
import org.inria.myriads.snoozenode.heartbeat.HeartbeatFactory;
import org.inria.myriads.snoozenode.heartbeat.discovery.GroupLeaderDiscovery;
import org.inria.myriads.snoozenode.heartbeat.listener.GroupLeaderHeartbeatArrivalListener;
import org.inria.myriads.snoozenode.heartbeat.listener.GroupManagerHeartbeatFailureListener;
import org.inria.myriads.snoozenode.localcontroller.actuator.ActuatorFactory;
import org.inria.myriads.snoozenode.localcontroller.actuator.api.VirtualMachineActuator;
import org.inria.myriads.snoozenode.localcontroller.connector.Connector;
import org.inria.myriads.snoozenode.localcontroller.monitoring.MonitoringFactory;
import org.inria.myriads.snoozenode.localcontroller.monitoring.api.HostMonitor;
import org.inria.myriads.snoozenode.localcontroller.monitoring.api.VirtualMachineMonitor;
import org.inria.myriads.snoozenode.localcontroller.monitoring.service.InfrastructureMonitoring;
import org.inria.myriads.snoozenode.localcontroller.monitoring.service.VirtualMachineMonitoringService;
import org.inria.myriads.snoozenode.localcontroller.powermanagement.PowerManagementFactory;
import org.inria.myriads.snoozenode.localcontroller.powermanagement.shutdown.Shutdown;
import org.inria.myriads.snoozenode.localcontroller.powermanagement.suspend.Suspend;
import org.inria.myriads.snoozenode.message.SystemMessage;
import org.inria.myriads.snoozenode.message.SystemMessageType;
import org.inria.myriads.snoozenode.util.ExternalNotifierUtils;
import org.inria.myriads.snoozenode.util.ManagementUtils;
import org.inria.snoozenode.external.notifier.ExternalNotificationType;
import org.inria.snoozenode.external.notifier.ExternalNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local controller backend.
 * 
 * @author Eugen Feller
 */
public final class LocalControllerBackend 
    implements GroupLeaderHeartbeatArrivalListener, GroupManagerHeartbeatFailureListener
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LocalControllerBackend.class);
    
    /** Local actuator. */
    private VirtualMachineActuator virtualMachineActuator_;
          
    /** Monitoring management of virtual machines. */
    private VirtualMachineMonitoringService virtualMachineMonitoringService_;
    
    /** Node parameters. */
    private NodeConfiguration nodeConfiguration_;
        
    /** Group leader repository. */
    private LocalControllerRepository localControllerRepository_;
    
    /** Controller description. */
    private LocalControllerDescription localControllerDescription_;
    
    /** Resource monitoring .*/
    private InfrastructureMonitoring resourceMonitoring_;
    
    /** Shutdown logic. */
    private Shutdown shutdownLogic_;
    
    /** Suspend logic. */
    private Suspend suspendLogic_;
    
    /**  External Notifier. */
    private ExternalNotifier externalNotifier_;
    
    /**
     * Constructor.
     * 
     * @param configuration     The node configuration
     * @throws Exception        The exception
     */
    public LocalControllerBackend(NodeConfiguration configuration) 
        throws Exception
    {
        Guard.check(configuration);
        log_.debug("Initializing the local controller backend");
        
        nodeConfiguration_ = configuration;
        initializeExternalNotifier();
        initializeDatabase();
        initializePowerManagement();
        startHypervisorServices();  
        createLocalControllerDescription();
        onGroupManagerHeartbeatFailure();
    }


    /**
     * Initializes the external notifier.
     * (should be static...)
     */
    private void initializeExternalNotifier()
    {
        externalNotifier_ = new ExternalNotifier(nodeConfiguration_);
    }


    /**
     * Initializes the database.
     */
    private void initializeDatabase()
    {
        DatabaseType type = nodeConfiguration_.getDatabase().getType();
        ExternalNotifierSettings externalNotifierSettings = nodeConfiguration_.getExternalNotifier();
        localControllerRepository_ = DatabaseFactory.newLocalControllerRepository(type, externalNotifier_);
    }
    
    /**
     * Initializes the power management.
     */
    private void initializePowerManagement()
    {
        int commandExecutionTimeOut = nodeConfiguration_.getEnergyManagement().getCommandExecutionTimeout();
        ShellCommandExecuter executer = new ShellCommandExecuter(commandExecutionTimeOut);
        
        SuspendDriver suspendDriver = nodeConfiguration_.getEnergyManagement().getDrivers().getSuspend();
        suspendLogic_ = PowerManagementFactory.newSuspendLogic(suspendDriver, executer);
        
        ShutdownDriver shutdownDriver = nodeConfiguration_.getEnergyManagement().getDrivers().getShutdown();
        shutdownLogic_ = PowerManagementFactory.newShutdownLogic(shutdownDriver, executer); 
    }

    /**
     * Initializes the hypervisor related services.
     * 
     * @throws Exception   The exception
     */
    private void startHypervisorServices() 
        throws Exception
    {
        log_.debug("Initializing the hypervisor services");
        String address = nodeConfiguration_.getNetworking().getListen().getControlDataAddress().getAddress();
        HypervisorSettings settings = nodeConfiguration_.getHypervisor();
        Connector connector = ActuatorFactory.newHypervisorConnector(address, settings);  
        virtualMachineActuator_ = ActuatorFactory.newVirtualMachineActuator(connector);
        createInfrastructureMonitor(connector);
     }

    /**
     * Creates infrastructure monitor.
     * 
     * @param connector                            The connector
     * @throws HostMonitoringException             The host monitoring exception
     * @throws VirtualMachineMonitoringException   The virtual machine monitoring exception
     */
    private void createInfrastructureMonitor(Connector connector) 
        throws HostMonitoringException, VirtualMachineMonitoringException
    {        
        NetworkDemand networkCapacity = nodeConfiguration_.getNode().getNetworkCapacity();
        HostMonitor hostMonitor = MonitoringFactory.newHostMonitoring(connector, networkCapacity);
        VirtualMachineMonitor virtualMachineMonitor =  MonitoringFactory.newVirtualMachineMonitor(connector); 
        resourceMonitoring_ = new InfrastructureMonitoring(virtualMachineMonitor, 
                                                           hostMonitor,
                                                           nodeConfiguration_.getMonitoring(),
                                                           nodeConfiguration_.getExternalNotifier());
    }
    
    /**
     * Creates the local controller description.
     * 
     * @throws HostMonitoringException     The host monitoring exception
     */
    private void createLocalControllerDescription() 
        throws HostMonitoringException
    {
        ArrayList<Double> totalCapacity = resourceMonitoring_.getHostMonitor().getTotalCapacity();
        localControllerDescription_ =  ManagementUtils.createLocalController(nodeConfiguration_, totalCapacity);
    }

    /**
     * Starts the virtual machine monitoring service.
     * 
     * @param groupManager  The group manager description
     * @throws Exception    The exception
     */
    private void startVirtualMachineMonitoringService(GroupManagerDescription groupManager) 
        throws Exception
    {
        if (virtualMachineMonitoringService_ == null)
        {
            virtualMachineMonitoringService_ = new VirtualMachineMonitoringService(localControllerDescription_,  
                                                                                   localControllerRepository_, 
                                                                                   resourceMonitoring_,
                                                                                   nodeConfiguration_.getDatabase()
                                                                                    );
        }
        
        virtualMachineMonitoringService_.startService(groupManager.getListenSettings().getMonitoringDataAddress());
    }
    
    /**
     * Initializes the group leader discovery.
     * 
     * @throws Exception    The exception
     */
    @Override
    public void onGroupManagerHeartbeatFailure() 
        throws Exception 
    {
        log_.debug("Initializing the group leader discovery");
        
        if (virtualMachineMonitoringService_ != null)
        {
            virtualMachineMonitoringService_.stopService();
        }
        
        ExternalNotifierUtils.send(
                externalNotifier_,
                ExternalNotificationType.SYSTEM,
                new SystemMessage(SystemMessageType.GM_FAILED, localControllerDescription_),
                "localcontroller." + localControllerDescription_.getId());
        
        new GroupLeaderDiscovery(nodeConfiguration_.getNetworking().getMulticast().getGroupLeaderHeartbeatAddress(), 
                                 nodeConfiguration_.getFaultTolerance().getHeartbeat().getTimeout(),
                                 this);
        
    }

    /**
     * Called upon group leader heartbeat arrival.
     * 
     * @param groupLeaderDescription    The group leader description
     * @return                          true if everything ok, false otherwise
     * @throws Exception                The exception
     */
    @Override
    public synchronized boolean onGroupLeaderHeartbeatArrival(GroupManagerDescription groupLeaderDescription) 
        throws Exception
    {
        log_.debug("Starting the join procedure");
        
        AssignedGroupManager assignment = assignLocalController(groupLeaderDescription);
        if (assignment == null)
        {
            log_.debug("Unable to assign the local controller! No group manager available yet?");
            return false;
        }
        
        GroupManagerDescription groupManager = assignment.getGroupManager();
        if (groupManager == null)
        {
            log_.debug("No group manager description available in the assignment!");
            return false;
        }
       
        String localControllerId = assignment.getLocalControllerId();
        if (localControllerId != null)
        {
            log_.debug("Updating local controller identifier!");
            localControllerDescription_.setId(localControllerId);
        }
        
        updateRepositoryInformation(groupManager);        
        boolean hasJoined = joinGroupManager(groupManager);
        if (!hasJoined)
        {
            log_.error("Unable to join the assigned group manager!");           
            return false;
        }
        
        
        startSystemServices(groupManager);
        return true;  
    }
    
    /**
     * Updates the repository information.
     * 
     * @param groupManagerDescription   The group manager description
     */
    private void updateRepositoryInformation(GroupManagerDescription groupManagerDescription)
    {
        log_.debug("Updating the virtual machine meta data information with new group manager description");
        
        HashMap<String, VirtualMachineMetaData> metaData =
            localControllerRepository_.updateVirtualMachineMetaData(groupManagerDescription);
        localControllerDescription_.setVirtualMachineMetaData(metaData);
        
        log_.debug(String.format("Update local controller location with : \n" +
                                 "localControllerId  : %s \n" + 
                                 "groupManagerId : %s \n" + 
                                 "groupManagerAddress : %s \n",
                                 localControllerDescription_.getId(),
                                 groupManagerDescription.getId(),
                                 groupManagerDescription.getListenSettings().getControlDataAddress().toString()
                ));
        
        
        //update also localcontroller location.
        LocalControllerLocation location = new LocalControllerLocation();
        location.setLocalControllerId(localControllerDescription_.getId());
        location.setGroupManagerId(groupManagerDescription.getId());
        location.setGroupManagerControlDataAddress(groupManagerDescription.getListenSettings().getControlDataAddress());
        
        localControllerDescription_.setLocation(location);
    }
    
    /**
     * Starts the system services.
     * 
     * @param groupManager   The group manager description
     * @throws Exception     The exception
     */
    private void startSystemServices(GroupManagerDescription groupManager)
        throws Exception
    {
        log_.debug("Starting the system services");
        
        int heartbeatTimeout = nodeConfiguration_.getFaultTolerance().getHeartbeat().getTimeout();
        HeartbeatFactory.newGroupManagerHeartbeatHandler(groupManager.getHeartbeatAddress(),
                                                         groupManager.getId(),
                                                         heartbeatTimeout,
                                                         this);
        startVirtualMachineMonitoringService(groupManager);
    }
    
    /**
     * Try to get a group manager assigned.
     * 
     * @param groupLeader    The group leader description
     * @return               The local controller assignment
     */
    private AssignedGroupManager assignLocalController(GroupManagerDescription groupLeader)
    {
        log_.debug("Assigninng the local controller to a group manager");
        
        NetworkAddress monitoringAddress = groupLeader.getListenSettings().getControlDataAddress();
        GroupManagerAPI communicator = CommunicatorFactory.newGroupManagerCommunicator(monitoringAddress);
        AssignedGroupManager assignment = communicator.assignLocalController(localControllerDescription_);
        return assignment;
    }
    
    /**
     * Join the group manager.
     * 
     * @param groupManager  The group manager description
     * @return              true if everything ok, false otherwise
     */
    private boolean joinGroupManager(GroupManagerDescription groupManager)
    {
        log_.debug("Joining the assigned group manager");
        
        NetworkAddress monitoringAddress = groupManager.getListenSettings().getControlDataAddress();
        GroupManagerAPI communicator = CommunicatorFactory.newGroupManagerCommunicator(monitoringAddress);
        boolean hasJoined = communicator.joinGroupManager(localControllerDescription_);      
        return hasJoined;
    }
    
    /**
     * Returns the local actuator.
     * 
     * @return     The virtual machine actuator
     */
    public VirtualMachineActuator getVirtualMachineActuator()
    {
        return virtualMachineActuator_;
    }
    
    /**
     * Returns the local monitor.
     * 
     * @return      The virtual machine monitoring service
     */
    public VirtualMachineMonitoringService getVirtualMachineMonitoringService()
    {
        return virtualMachineMonitoringService_;
    }
    
    /**
     * Returns the node parameters.
     *  
     * @return      The node parameters
     */
    public NodeConfiguration getNodeParameters()
    {
        return nodeConfiguration_;
    }
    
    /**
     * Returns the local controller repository.
     * 
     * @return      The local controller repository
     */
    public LocalControllerRepository getRepository() 
    {
        return localControllerRepository_;
    }
    
    /**
     * Returns the local controller description.
     * 
     * @return      The local controller description
     */
    public LocalControllerDescription getLocalControllerDescription()
    {
        return localControllerDescription_;
    }
    
    /**
     * Power cycles the local controller.
     * 
     * @param powerSavingAction     The power saving action
     * @return                      true if everything ok, false otherwise
     */
    public boolean powerCycle(PowerSavingAction powerSavingAction)
    {
        boolean isPowerCycled = false;
        
        ExternalNotifierUtils.send(
                externalNotifier_,
                ExternalNotificationType.SYSTEM,
                new SystemMessage(SystemMessageType.ENERGY, localControllerDescription_),
                "localcontroller." + localControllerDescription_.getId());
        
        switch (powerSavingAction)
        {
            case suspendToRam:
                isPowerCycled = suspendLogic_.suspendToRam();
                break;
                
            case suspendToDisk:
                isPowerCycled = suspendLogic_.suspendToDisk();
                break;
                
            case suspendToBoth:
                isPowerCycled = suspendLogic_.suspendToBoth();
                break;
                
            case shutdown:
                isPowerCycled = shutdownLogic_.shutdown();
                break;
                
            default:
                log_.error(String.format("This power cycling action is not supported: %s", powerSavingAction));
        }
        
        
        
        return isPowerCycled;
    }
}
