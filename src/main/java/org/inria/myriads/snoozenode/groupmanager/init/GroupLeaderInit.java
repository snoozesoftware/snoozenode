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
package org.inria.myriads.snoozenode.groupmanager.init;

import java.io.IOException;
import java.util.List;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.AssignedGroupManager;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.configurator.database.DatabaseSettings;
import org.inria.myriads.snoozenode.database.DatabaseFactory;
import org.inria.myriads.snoozenode.database.api.GroupLeaderRepository;
import org.inria.myriads.snoozenode.estimator.ResourceEstimatorFactory;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.estimator.api.impl.StaticDynamicResourceDemandEstimator;
import org.inria.myriads.snoozenode.exception.GroupLeaderInitException;
import org.inria.myriads.snoozenode.exception.ResourceDemandEstimatorException;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.GroupLeaderPolicyFactory;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.assignment.api.AssignmentPolicy;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.enums.Assignment;
import org.inria.myriads.snoozenode.groupmanager.monitoring.MonitoringFactory;
import org.inria.myriads.snoozenode.groupmanager.virtualclustermanager.VirtualClusterManager;
import org.inria.myriads.snoozenode.groupmanager.virtualmachinediscovery.VirtualMachineDiscovery;
import org.inria.myriads.snoozenode.heartbeat.HeartbeatFactory;
import org.inria.myriads.snoozenode.heartbeat.message.HeartbeatMessage;
import org.inria.myriads.snoozenode.util.ManagementUtils;
import org.inria.snoozenode.external.notifier.ExternalNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Group leader initialization.
 * 
 * @author Eugen Feller
 */
public final class GroupLeaderInit
{
    /** Logging instance. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupLeaderInit.class);

    /** Node configuration parameters.. */
    private NodeConfiguration nodeConfiguration_;

    /** Group leader repository. */
    private GroupLeaderRepository groupLeaderRepository_;

    /** Virtual cluster manager. */
    private VirtualClusterManager virtualClusterManager_;

    /** Virtual machine discovery. */
    private VirtualMachineDiscovery virtualMachineDiscovery_;

    /** Local controller assignment policy. */
    private AssignmentPolicy assignmentPolicy_;

    /** Resource demand estimator. */
    private ResourceDemandEstimator estimator_;

    /** External notifier.*/
    private ExternalNotifier externalNotifier_;

    /**
     * Constructor.
     * 
     * @param nodeConfiguration         The node configuration
     * @param groupLeaderDescription    The group leader description
     * @param externalNotifier          external notifier.
     * @throws Exception 
     */
    public GroupLeaderInit(NodeConfiguration nodeConfiguration, 
            GroupManagerDescription groupLeaderDescription,
            ExternalNotifier externalNotifier
            ) 
        throws Exception 
    {
        Guard.check(nodeConfiguration);
        log_.debug("Initializing the group leader logic");
        nodeConfiguration_ = nodeConfiguration;    
        externalNotifier_ = externalNotifier;
        startInitialization(groupLeaderDescription);
    }

    
    
    /**
     * Starts the initialization.
     * 
     * @param groupLeaderDescription                The group leader description
     * @throws Exception 
     */
    private void startInitialization(GroupManagerDescription groupLeaderDescription)
        throws Exception
    {
        Guard.check(groupLeaderDescription);
        log_.debug("Starting the group leader components initialization!");
        
    
        initializeLocalControllerAssignmentPolicy();
        initializeRepository(groupLeaderDescription);
        initializeResourceDemandEstimator();
        initializeVirtualClusterManager();
        initializeVirtualMachineDiscovery();
        startGroupManagerMonitoringDataReceiver(); 
        //everyting is setup we can send heartbeat.
        startHeartbeatSender(groupLeaderDescription);       
    }
        
    /**
     * Returns the node configuration.
     * 
     * @return      The node configuration
     */
    public NodeConfiguration getNodeConfiguration()
    {
        return nodeConfiguration_;
    }
    
    /**
     * Initializes the resource demand estimator.
     * @throws ResourceDemandEstimatorException 
     */
    private void initializeResourceDemandEstimator() throws ResourceDemandEstimatorException 
    {
//        estimator_ = new StaticDynamicResourceDemandEstimator(nodeConfiguration_.getEstimator(),
//                                                 nodeConfiguration_.getMonitoring().getThresholds(),
//                                                 nodeConfiguration_.getSubmission().getPackingDensity());      
        estimator_ = ResourceEstimatorFactory.newResourceDemandEstimator(
                nodeConfiguration_.getEstimator(),
                nodeConfiguration_.getMonitoring(),
                nodeConfiguration_.getHostMonitoringSettings()
                );
    }
    
    /**
     * Initializes the repository.
     * 
     * @param groupLeaderDescription    The group leader description
     */
    private void initializeRepository(GroupManagerDescription groupLeaderDescription) 
    {
        String[] virtualMachineSubnets = nodeConfiguration_.getNetworking().getVirtualMachineSubnets();
        DatabaseSettings settings = nodeConfiguration_.getDatabase();
        groupLeaderRepository_ = 
                DatabaseFactory.newGroupLeaderRepository(
                                groupLeaderDescription,
                                virtualMachineSubnets, 
                                settings,
                                externalNotifier_);
    }
    
    /**
     * Starts the virtual machine discovery.
     */
    private void initializeVirtualMachineDiscovery() 
    {
        virtualMachineDiscovery_ = new VirtualMachineDiscovery(groupLeaderRepository_);
    }

    /**
     * Starts the virtual cluster manager.
     */
    private void initializeVirtualClusterManager() 
    {
        virtualClusterManager_ = new VirtualClusterManager(nodeConfiguration_, groupLeaderRepository_, estimator_); 
    }

    /** 
     * Return the group manager repository.
     *  
     * @return   The group leader repository
     */
    public GroupLeaderRepository getRepository() 
    {
        return groupLeaderRepository_;
    }
    
    /**
     * Starts the group manager monitoring data receiver.
     * 
     * @throws Exception 
     */
    private void startGroupManagerMonitoringDataReceiver() 
        throws Exception 
    {
        log_.debug("Starting the group manager monitoring data receiver");  
        NetworkAddress monitoringAddress = 
            nodeConfiguration_.getNetworking().getListen().getMonitoringDataAddress();
        int monitoringTimeout = nodeConfiguration_.getMonitoring().getTimeout(); 
        MonitoringFactory.newGroupManagerSummaryReceiver(monitoringAddress,
                                                         monitoringTimeout,
                                                         groupLeaderRepository_);
    }
    
    /**
     * Initializes the local controller assignment policy.
     * 
     * @throws GroupLeaderInitException 
     */
    private void initializeLocalControllerAssignmentPolicy() 
        throws GroupLeaderInitException
    {
        String policy = nodeConfiguration_.getGroupLeaderScheduler().getAssignmentPolicy();
        assignmentPolicy_ = GroupLeaderPolicyFactory.newLocalControllerAssignment(policy);
        if (assignmentPolicy_ == null)
        {
            throw new GroupLeaderInitException("Local controller assignment policy is NULL");
        }
    }
    
    /** 
     * Assigns local controller to group manager.
     *  
     * @param localController    The local controller description
     * @return                   The group manager description                              
     */
    public AssignedGroupManager assignLocalController(LocalControllerDescription localController) 
    {
        Guard.check(localController);
        log_.debug(String.format("Assigning local controller: %s, %s: %d", 
                                 localController.getId(), 
                                 localController.getControlDataAddress().getAddress(),
                                 localController.getControlDataAddress().getPort()));
        
        int numberOfMonitoringEntries = nodeConfiguration_.getEstimator().getNumberOfMonitoringEntries();
        List<GroupManagerDescription> descriptions = 
            groupLeaderRepository_.getGroupManagerDescriptions(numberOfMonitoringEntries);
        if (descriptions.size() == 0)
        {
            log_.debug("No group manager descriptions available!");
            return null;
        }
        
        AssignedGroupManager lookUp = lookupLocalControllerLocation(localController, descriptions);
        if (lookUp == null)
        {
            GroupManagerDescription groupManager = assignmentPolicy_.assign(localController, descriptions);
            log_.debug(String.format("Unable to find previous group manager! Assigned to new group manager: %s",
                                      groupManager.getId()));
            lookUp = new AssignedGroupManager();
            lookUp.setGroupManager(groupManager);
            return lookUp;
        }
        
        log_.debug(String.format("Previous identifier: %s and group manager found: %s",
                                 lookUp.getLocalControllerId(), 
                                 lookUp.getGroupManager().getId()));
        return lookUp;
    }
    
    /**
     * Performs local controller lookup.
     * 
     * @param localController   The local controller description
     * @param groupManagers     The group manager descriptions
     * @return                  The lookup reply, null otherwise
     */
    private AssignedGroupManager lookupLocalControllerLocation(LocalControllerDescription localController,
                                                               List<GroupManagerDescription> groupManagers)
    {
        log_.debug("Performing local controller lookup on group managers");
        NetworkAddress contactInformation = localController.getControlDataAddress();
        

        AssignedGroupManager lookup = groupLeaderRepository_.getAssignedGroupManager(contactInformation);
        
        return lookup;
    }
    
    /** 
     * Multicast the group leader presence.
     *  
     * @param  groupLeader   The group leader description
     * @throws IOException 
     */
    private void startHeartbeatSender(GroupManagerDescription groupLeader) 
        throws IOException 
    {
        Guard.check(groupLeader);
        log_.debug("Starting the group leader heartbeat sender");  
                
        NetworkAddress heartbeatAddress = nodeConfiguration_.getNetworking()
                                                                  .getMulticast()
                                                                  .getGroupLeaderHeartbeatAddress();
        int heartbeatInterval = nodeConfiguration_.getFaultTolerance().getHeartbeat().getInterval();
        HeartbeatMessage heartbeatMessage = ManagementUtils.createHeartbeatMessage(groupLeader.getListenSettings(), 
                                                                                   groupLeader.getId());
        new Thread(HeartbeatFactory.newHeartbeatMulticastSender(heartbeatAddress, 
                                                                heartbeatInterval,
                                                                heartbeatMessage),
                  "HeartbeatSender"
                ).start();
    }
    
    /** 
     * Returns the virtual cluster manager.
     * 
     * @return  The virtual cluster manager
     */
    public VirtualClusterManager getVirtualClusterManager() 
    {
        return virtualClusterManager_;
    }

    /**
     * Returns the virtual machine discovery.
     * 
     * @return      The virtual machine discovery service
     */
    public VirtualMachineDiscovery getVirtualMachineDiscovery() 
    {
        return virtualMachineDiscovery_;
    }

    /**
     * Returns the resource demand estimator.
     * 
     * @return  The resource demand estimator
     */
    public ResourceDemandEstimator getResourceDemandEstimator() 
    {
        return estimator_;
    }
}
