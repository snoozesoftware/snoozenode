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

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.groupmanager.ListenSettings;
import org.inria.myriads.snoozecommon.communication.rest.CommunicatorFactory;
import org.inria.myriads.snoozecommon.communication.rest.api.GroupManagerAPI;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.configurator.database.DatabaseSettings;
import org.inria.myriads.snoozenode.configurator.scheduler.ReconfigurationSettings;
import org.inria.myriads.snoozenode.database.DatabaseFactory;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.estimator.ResourceEstimatorFactory;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.estimator.api.impl.StaticDynamicResourceDemandEstimator;
import org.inria.myriads.snoozenode.exception.ResourceDemandEstimatorException;
import org.inria.myriads.snoozenode.groupmanager.energysaver.EnergySaverFactory;
import org.inria.myriads.snoozenode.groupmanager.energysaver.saver.EnergySaver;
import org.inria.myriads.snoozenode.groupmanager.monitoring.MonitoringFactory;
import org.inria.myriads.snoozenode.groupmanager.monitoring.service.GroupManagerMonitoringService;
import org.inria.myriads.snoozenode.groupmanager.monitoring.service.LocalControllerMonitoringService;
import org.inria.myriads.snoozenode.groupmanager.reconfiguration.ReconfigurationScheduler;
import org.inria.myriads.snoozenode.groupmanager.statemachine.api.StateMachine;
import org.inria.myriads.snoozenode.groupmanager.statemachine.api.impl.GroupManagerStateMachine;
import org.inria.myriads.snoozenode.heartbeat.HeartbeatFactory;
import org.inria.myriads.snoozenode.heartbeat.message.HeartbeatMessage;
import org.inria.myriads.snoozenode.heartbeat.sender.HeartbeatMulticastSender;
import org.inria.myriads.snoozenode.util.ManagementUtils;
import org.inria.snoozenode.external.notifier.ExternalNotifier;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Group manager initialization.
 * 
 * @author Eugen Feller
 */
public final class GroupManagerInit
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupManagerInit.class);
    
    /** Holds the node params reference. */
    private NodeConfiguration nodeConfiguration_;
    
    /** Group mananager repository. */
    private GroupManagerRepository repository_;
    
    /** Energy saver. */
    private EnergySaver energySaver_;

    /** Heartbeat multicast sender. */
    private HeartbeatMulticastSender heartbeatSender_;

    /** Local controller monitoring service. */
    private LocalControllerMonitoringService localControllerMonitoring_;
    
    /** Monitoring service. */
    private GroupManagerMonitoringService monitoringService_;

    /** Control loop scheduler. */
    private ReconfigurationScheduler reconfigurationScheduler_;

    /** Group manager description. */
    private GroupManagerDescription description_;

    /** Resource demand measure. */
    private ResourceDemandEstimator estimator_;
     
    /** State machine. */
    private StateMachine stateMachine_;
    
    /** External Notifier. */
    private ExternalNotifier externalNotifier_;
    
    
    
    /**
     * Group manager logic constructor.
     * 
     * @param nodeConfiguration         The node configuration
     * @param groupManagerDescription   The group manager description
     * @param externalNotifier          The external notifier   
     * @throws Exception                The exception
     */
    public GroupManagerInit(NodeConfiguration nodeConfiguration,
                            GroupManagerDescription groupManagerDescription,
                            ExternalNotifier externalNotifier
            ) 
        throws Exception 
    {
        Guard.check(nodeConfiguration, groupManagerDescription);
        log_.debug("Initializing the group manager logic");
        
        nodeConfiguration_ = nodeConfiguration; 
        description_ = groupManagerDescription;
        externalNotifier_ = externalNotifier;
        initializeRepository();
        initializeResourceDemandEstimator();
        initializeStateMachine();
        checkAndEnableFeatures();
        startLocalControllerMonitoringService();
        startHeartbeatSender();
        

    }
        
   

    /**
     * Stops the group manager services.
     * 
     * @throws IOException 
     * @throws SchedulerException 
     */
    public void stopServices()  
        throws SchedulerException, IOException
    {
        log_.debug("Stopping the group manager logic");
        
        if (repository_ != null)
        {
            repository_.clean();
        }
        
        if (energySaver_ != null)
        {
            energySaver_.terminate();
        }
        
        if (heartbeatSender_ != null)
        {
            heartbeatSender_.terminate();
        }
                
        if (localControllerMonitoring_ != null)
        {
            localControllerMonitoring_.terminate();
        }
        
        if (monitoringService_ != null)
        {
            monitoringService_.terminate();
        }
        
        if (reconfigurationScheduler_ != null)
        {
            reconfigurationScheduler_.shutdown();
        }
    }
            
    /**
     * Initializes the repository.
     */
    private void initializeRepository()
    {        
        int maxCapacity = nodeConfiguration_.getDatabase().getNumberOfEntriesPerVirtualMachine();
        DatabaseSettings settings = nodeConfiguration_.getDatabase();
        int interval = nodeConfiguration_.getMonitoring().getInterval();
        repository_ = DatabaseFactory.newGroupManagerRepository(
                new GroupManagerDescription(description_, 0),
                maxCapacity,
                interval,
                settings,
                nodeConfiguration_.getExternalNotifier(),
                externalNotifier_
                );
    }
    
    /**
     * Initializes the resource demand estimator.
     * @throws ResourceDemandEstimatorException 
     */
    private void initializeResourceDemandEstimator() throws ResourceDemandEstimatorException 
    {     
        estimator_ = ResourceEstimatorFactory.newResourceDemandEstimator(
                nodeConfiguration_.getEstimator(),
                nodeConfiguration_.getMonitoring(),
                nodeConfiguration_.getHostMonitoringSettings()
                );
    }
    
    /**
     * Initializes the state machine.
     */
    private void initializeStateMachine() 
    {
        stateMachine_ = new GroupManagerStateMachine(nodeConfiguration_, estimator_, repository_, externalNotifier_);
    }
    
    /**
     * Starts the local controller monitoring service.
     * 
     * @throws Exception 
     */
    private void startLocalControllerMonitoringService() 
        throws Exception 
    {
        localControllerMonitoring_ = new LocalControllerMonitoringService(nodeConfiguration_, 
                                                                          stateMachine_,
                                                                          repository_);
        localControllerMonitoring_.startMonitoring();
    }
    
    /**
     * Enable possible features.
     * 
     * @throws Exception    The exception
     */
    private void checkAndEnableFeatures() 
        throws Exception
    {
        ReconfigurationSettings reconfigurationSettings = 
            nodeConfiguration_.getGroupManagerScheduler().getReconfigurationSettings();
        if (reconfigurationSettings.isEnabled())
        {
            log_.debug("Starting the reconfiguration loop");
            String interval = reconfigurationSettings.getInterval();
            reconfigurationScheduler_ = new ReconfigurationScheduler(stateMachine_, interval);
            reconfigurationScheduler_.run();
        }
        
        if (nodeConfiguration_.getEnergyManagement().isEnabled())
        {
            log_.debug("Starting the energy saver");
            energySaver_ = EnergySaverFactory.newEnergySaver(nodeConfiguration_.getEnergyManagement(), 
                                                             repository_,
                                                             stateMachine_);
            new Thread(energySaver_, "EnergySaver").start();
        }  
    }
    
    /** 
     * Multicast group manager presence.
     * @throws IOException 
     */
    private void startHeartbeatSender() 
        throws IOException 
    {
        log_.debug("Starting the group manager heartbeat sender");   
        
        NetworkAddress heartbeatAddress = nodeConfiguration_.getNetworking()
                                                                  .getMulticast()
                                                                  .getGroupManagerHeartbeatAddress();
        int heartbeatInterval = nodeConfiguration_.getFaultTolerance().getHeartbeat().getInterval();
        ListenSettings listenSettings = nodeConfiguration_.getNetworking().getListen();
        HeartbeatMessage heartbeatMessage = ManagementUtils.createHeartbeatMessage(listenSettings,
                                                                                   description_.getId());
        heartbeatSender_ = HeartbeatFactory.newHeartbeatMulticastSender(heartbeatAddress, 
                                                                        heartbeatInterval,
                                                                        heartbeatMessage);        
        new Thread(heartbeatSender_, "HeartBeatSender").start();
    }
    
    /**
     * Starts the data sender.
     * 
     * @param groupLeader   The group leader description
     * @throws Exception    The exception
     */
    private void startGroupManagerMonitoringService(GroupManagerDescription groupLeader)
        throws Exception
    {
        Guard.check(groupLeader);              
        if (monitoringService_ == null)
        {
            monitoringService_ = MonitoringFactory.newGroupManagerMonitoringService(
                    description_.getId(), 
                    repository_,
                    estimator_,
                    nodeConfiguration_.getDatabase(),
                    nodeConfiguration_.getMonitoring(), 
                    nodeConfiguration_.getExternalNotifier()
                    );
        }
        
        monitoringService_.startServices(groupLeader.getListenSettings().getMonitoringDataAddress());
    }
        
    /**
     * Returns the energy saver reference.
     * 
     * @return The energy saver
     */
    public EnergySaver getEnergySaver()
    {
        return energySaver_;
    }
    
    /**
     * Routine to join the group leader.
     * 
     * @param groupLeader      The group leader description
     * @return                 true if everything ok, false otherwise
     * @throws Exception       The exception
     */    
    public boolean onGroupLeaderJoin(GroupManagerDescription groupLeader)
        throws Exception 
    {
        Guard.check(groupLeader);
        
        NetworkAddress address = groupLeader.getListenSettings().getControlDataAddress();
        log_.debug(String.format("Joining group leader %s with control data port: %s",
                                 address.getAddress(), address.getPort()));
        
        repository_.fillGroupManagerDescription(description_);
        
        GroupManagerAPI communicator = CommunicatorFactory.newGroupManagerCommunicator(address);
        boolean hasJoined = communicator.joinGroupLeader(description_);
        if (hasJoined)
        {
            startGroupManagerMonitoringService(groupLeader);
        }
        
        return hasJoined;
    }
 
    /**.
     * Returns the state machine.
     * 
     * @return  The state machine
     */
    public StateMachine getStateMachine() 
    {
        return stateMachine_;
    }
    
    /**
     * Returns the group manager repository.
     * 
     * @return  The group manager repository
     */
    public GroupManagerRepository getRepository()
    {
        return repository_;
    }

    /**
     * 
     * Gets the external notifier.
     * 
     * @return ExternalNotifier     
     */
    public ExternalNotifier getExternalNotifier()
    {
        return externalNotifier_;
    }
}
