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
package org.inria.myriads.snoozenode.groupmanager.statemachine.api.impl;

import java.util.List;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.migration.MigrationRequest;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineSubmissionRequest;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineSubmissionResponse;
import org.inria.myriads.snoozecommon.communication.virtualmachine.ResizeRequest;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.anomaly.AnomalyResolverSettings;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.configurator.energymanagement.EnergyManagementSettings;
import org.inria.myriads.snoozenode.configurator.energymanagement.enums.PowerSavingAction;
import org.inria.myriads.snoozenode.configurator.estimator.EstimatorSettings;
import org.inria.myriads.snoozenode.configurator.scheduler.GroupManagerSchedulerSettings;
import org.inria.myriads.snoozenode.configurator.scheduler.RelocationSettings;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.estimator.api.impl.StaticDynamicResourceDemandEstimator;
import org.inria.myriads.snoozenode.exception.GroupManagerInitException;
import org.inria.myriads.snoozenode.exception.NodeConfiguratorException;
import org.inria.myriads.snoozenode.groupmanager.anomaly.AnomalyResolverFactory;
import org.inria.myriads.snoozenode.groupmanager.anomaly.resolver.api.AnomalyResolver;
import org.inria.myriads.snoozenode.groupmanager.anomaly.resolver.api.impl.UnderOverloadAnomalyResolver;
import org.inria.myriads.snoozenode.groupmanager.energysaver.EnergySaverFactory;
import org.inria.myriads.snoozenode.groupmanager.energysaver.util.EnergySaverUtils;
import org.inria.myriads.snoozenode.groupmanager.energysaver.wakeup.WakeupResources;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.GroupManagerPolicyFactory;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.enums.Reconfiguration;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.ReconfigurationPlan;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.ReconfigurationPolicy;
import org.inria.myriads.snoozenode.groupmanager.migration.MigrationPlanEnforcer;
import org.inria.myriads.snoozenode.groupmanager.migration.listener.MigrationPlanListener;
import org.inria.myriads.snoozenode.groupmanager.statemachine.SystemState;
import org.inria.myriads.snoozenode.groupmanager.statemachine.VirtualMachineCommand;
import org.inria.myriads.snoozenode.groupmanager.statemachine.api.StateMachine;
import org.inria.myriads.snoozenode.groupmanager.virtualmachinemanager.VirtualMachineManager;
import org.inria.myriads.snoozenode.localcontroller.monitoring.enums.LocalControllerState;
import org.inria.myriads.snoozenode.message.ManagementMessage;
import org.inria.myriads.snoozenode.message.ManagementMessageType;
import org.inria.myriads.snoozenode.message.SystemMessage;
import org.inria.myriads.snoozenode.message.SystemMessageType;
import org.inria.myriads.snoozenode.util.ExternalNotifierUtils;
import org.inria.snoozenode.external.notifier.ExternalNotificationType;
import org.inria.snoozenode.external.notifier.ExternalNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Group manager state machine.
 * 
 * @author Eugen Feller
 */
public class GroupManagerStateMachine 
    implements StateMachine, MigrationPlanListener
{
    /** Logging instance. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupManagerStateMachine.class);
    
    /** System state reference. */
    private SystemState systemState_;
    
    /** Reconfiguration policy. */
    private ReconfigurationPolicy reconfiguration_;
        
    /** Anomaly resolver. */ 
    private MigrationPlanEnforcer migrationPlanEnforcer_;

    /** Virtual machine manager. */
    private VirtualMachineManager virtualMachineManager_;

    /** Energy management settings. */
    private EnergyManagementSettings energyManagementSettings_;
    
    /** Energy management settings. */
    private EstimatorSettings estimatorSettings_;

    /** Repository. */
    private GroupManagerRepository repository_;
    
    /** Wakeup resource. */
    private WakeupResources wakeupResources_;

    /** Anomaly resolver. */
    private AnomalyResolver anomalyResolver_;
    
    /** External notifier. */
    private ExternalNotifier externalNotifier_;
    
    /** 
     * Constructor. 
     * 
     * @param nodeConfiguration         The node configuration
     * @param estimator                 The resource demand estimator
     * @param repository                The repository
     * @param externalNotifier          The external Notifier.
     */
    public GroupManagerStateMachine(NodeConfiguration nodeConfiguration,
                                    ResourceDemandEstimator estimator,
                                    GroupManagerRepository repository,
                                    ExternalNotifier externalNotifier
                                    )
    {
        log_.debug("Initializing the state machine");
        systemState_ = SystemState.IDLE; 
        energyManagementSettings_ = nodeConfiguration.getEnergyManagement();
        estimatorSettings_ = nodeConfiguration.getEstimator();
        repository_ = repository;
        externalNotifier_ = externalNotifier;
        // Migration plan enforcer
        migrationPlanEnforcer_ = new MigrationPlanEnforcer(externalNotifier_, repository, this);
        // Wakeup 
        wakeupResources_ = createWakeupResources(energyManagementSettings_, repository);
        // Virtual machine manager
        virtualMachineManager_ = createVirtualMachineManager(nodeConfiguration, estimator, repository);
        // Anomaly
        anomalyResolver_ = createAnomalyResolver(nodeConfiguration.getAnomalyResolverSettings(), estimator, repository);
        // Reconfiguration
        GroupManagerSchedulerSettings schedulerSettings = nodeConfiguration.getGroupManagerScheduler();
        String reconfiguration = schedulerSettings.getReconfigurationSettings().getPolicy();
        reconfiguration_ = GroupManagerPolicyFactory.newVirtualMachineReconfiguration(reconfiguration, estimator);  
    }
    
    /**
     * Creates anomaly resolver.
     *  
     * @param relocation             The relocation settings
     * @param estimator              The estimator
     * @param repository             The group manager repository
     * @return                       The anomaly resolver
     */
    private AnomalyResolver createAnomalyResolver(AnomalyResolverSettings anomalyResolverSettings, 
                                                  ResourceDemandEstimator estimator, 
                                                  GroupManagerRepository repository) 
    {
        // TODO Factory method
        AnomalyResolver anomalyResolver = AnomalyResolverFactory.newAnomalyresolver(
                externalNotifier_,
                anomalyResolverSettings,
                estimator,
                repository_,
                this
                );
        
        return anomalyResolver;
    }
    
    /**
     * Starts the virtual machine management.
     * 
     * @param nodeConfiguration            The node configuration
     * @param repository                   The group manager repository
     * @param estimator                    The estimator
     * @return                             The virtual machine manager
     */
    private VirtualMachineManager createVirtualMachineManager(NodeConfiguration nodeConfiguration,
                                                              ResourceDemandEstimator estimator,
                                                              GroupManagerRepository repository)
    {
        
        VirtualMachineManager virtualMachineManager = new VirtualMachineManager(nodeConfiguration, 
                                                                                estimator, 
                                                                                repository, 
                                                                                this);
        return virtualMachineManager;
    }
    
    /**
     * Initializes wakeup resources.
     * 
     * @param energyManagement          The energy management settings
     * @param repository                The group manager repository
     * @return                          The wakeup resources
     */
    private WakeupResources createWakeupResources(EnergyManagementSettings energyManagement,
                                                  GroupManagerRepository repository) 
    {
        int wakeupTime = energyManagement.getThresholds().getWakeupTime();
        int commandExecutionTimeout = energyManagement.getCommandExecutionTimeout();
        WakeupResources wakeup = EnergySaverFactory.newWakeupResource(wakeupTime, 
                                                                      commandExecutionTimeout,
                                                                      repository);   
        return wakeup;
    }
    
    /**
     * Starts the virtual machine.
     * 
     * @param submissionRequest     The virtual machine submission
     * @return                      The task identifier
     */
    @Override
    public String startVirtualMachines(VirtualMachineSubmissionRequest submissionRequest)
    {
        log_.debug("Starting virtual machines");        
        
        if (!changeState(SystemState.MANAGEMENT))
        {
            return null;
        }
        
        String taskIdentifier = virtualMachineManager_.start(submissionRequest);
        return taskIdentifier;
    }
    
    
    
    /**
     * Processes virtual machine command.
     * 
     * @param command      The virtual machine command
     * @param location     The virtual machine location
     * @return             true if everything ok, false otherwise
     */
    @Override
    public boolean controlVirtualMachine(VirtualMachineCommand command, VirtualMachineLocation location)
    {
        log_.debug(String.format("Starting virtual machine command: %s processing", command));
        
        if (!changeState(SystemState.MANAGEMENT))
        {
            return false;
        }   
        
        setIdle();
        boolean isProcessed = virtualMachineManager_.processControlCommand(command, location);
        
        ExternalNotifierUtils.send(
                externalNotifier_,
                ExternalNotificationType.MANAGEMENT,
                new ManagementMessage(
                        ManagementMessageType.PROCESSED, repository_.getVirtualMachineMetaData(location, 0)),
                location.getGroupManagerId() + "." +
                location.getLocalControllerId() + "." + 
                location.getVirtualMachineId() + "." +
                command
                );
        
        return isProcessed;
    }

    /** 
     * Changes the system state.
     * 
     * @param state    The new system state
     * @return          true if everything ok, false otherwise
     */
    private synchronized boolean changeState(SystemState state)
    {
        if (systemState_.equals(SystemState.IDLE))
        {
            log_.debug(String.format("Changing system state to: %s", state));
            systemState_ = state;
            // log state change.
            ExternalNotifierUtils.send(
                    externalNotifier_,
                    ExternalNotificationType.SYSTEM,
                    new SystemMessage(SystemMessageType.GM_BUSY, repository_.getGroupManager()),
                    "groupmanager." + repository_.getGroupManagerId()
                    );
            return true;
        }
        
        log_.debug(String.format("Unable to change state! System is in state: %s", systemState_));
        return false;
    }
    
    /**
     * Power cycles the idle resources.
     * 
     * @param idleResources     The list of idle resources
     * @return                  true if energy savings enabled, false otherwise
     */ 
    @Override
    public boolean onEnergySavingsEnabled(List<LocalControllerDescription> idleResources)
    {
        Guard.check(idleResources);
        log_.debug("Entering on energy savings enabled!");
        
        if (!changeState(SystemState.ENERGYSAVER))
        {
            return false;
        }      

        log_.debug(String.format("Power cycling %d idle resources!", idleResources.size()));    
        PowerSavingAction action = energyManagementSettings_.getPowerSavingAction();
        
        ExternalNotifierUtils.send(
                externalNotifier_,
                ExternalNotificationType.SYSTEM,
                new SystemMessage(SystemMessageType.ENERGY, idleResources),
                "groupmanager." + repository_.getGroupManagerId()
                );
        
        EnergySaverUtils.powerCycleLocalControllers(idleResources, action, repository_);  
        setIdle();
        
        return true;
    }
    
    /**
     * Changes system state to idle.
     */
    private synchronized void setIdle()
    {
        log_.debug(String.format("Changing system state from: %s to IDLE", systemState_));
        ExternalNotifierUtils.send(
                externalNotifier_,
                ExternalNotificationType.SYSTEM,
                new SystemMessage(SystemMessageType.GM_IDLE, repository_.getGroupManager()),
                "groupmanager." + repository_.getGroupManagerId()
                );
        systemState_ = SystemState.IDLE;
    }
    
    /**
     * Starts the reconfiguration process.
     * 
     * @return     true if everything ok, false otherwise
     */
    @Override
    public boolean startReconfiguration()
    {
        log_.debug("Starting the reconfiguration procedure");
        
        if (!changeState(SystemState.RECONFIGURATION))
        {
            return false;
        }    
        
        try
        {
            int numberOfMonitoringEntries = estimatorSettings_.getNumberOfMonitoringEntries();
            List<LocalControllerDescription> localControllers = 
                repository_.getLocalControllerDescriptions(numberOfMonitoringEntries, true, true);
            if (localControllers == null)
            {
                throw new GroupManagerInitException("Local controllers list is not available!");
            }
                 
            if (localControllers.size() == 0)
            {
                throw new GroupManagerInitException("Local controller list is empty!");
            }
        
            ReconfigurationPlan migrationPlan = reconfiguration_.reconfigure(localControllers);
            if (migrationPlan == null)
            {
                throw new GroupManagerInitException("Migration plan is not available!");        
            }
            
            if (migrationPlan.getNumberOfReleasedNodes() != 0)
            {   
                log_.debug("Consolidation started!");
            }
            
            migrationPlanEnforcer_.enforceMigrationPlan(migrationPlan);
        }
        catch (Exception exception) 
        {
            setIdle();
            log_.debug(String.format("Unable to execute the migration plan: %s", exception.getMessage()));
            return false;
        }
        
        return true;
    }

    /**
     * Resolve anomaly.
     * 
     * @param localControllerId    The local controller identifier
     * @param state                The local controller state           
     */
    @Override
    public void resolveAnomaly(String localControllerId, Object anomaly) 
    {
        log_.debug(String.format("Starting to resolve ANOMALY"));
    
        // test if the anomaly resolver will handle this anomaly.
        boolean resolve = anomalyResolver_.readyToResolve(localControllerId, anomaly);
        if (!resolve)
        {
            log_.debug("Skipping anomaly for now");
            return;
        }
        
        if (!changeState(SystemState.RELOCATION))
        {
            return;
        }
        int numberOfMonitoringEntries = anomalyResolver_.getNumberOfMonitoringEntries();

        LocalControllerDescription anomalyLocalController = 
                repository_.getLocalControllerDescription(localControllerId, numberOfMonitoringEntries, true);        
        try 
        {
            anomalyResolver_.resolveAnomaly(anomalyLocalController, anomaly);
        } 
        catch (Exception exception) 
        {
            log_.debug(String.format("Exception during anomaly resolving: %s", exception.getMessage()));
            setIdle();
        }
    }
    
    /**
     * Checks if the system is busy.
     * 
     * @return   true if busy, false otherwise
     */
    @Override
    public boolean isBusy()
    {        
        if (!systemState_.equals(SystemState.IDLE))
        {
            log_.debug(String.format("System is in state: %s", systemState_));
            return true;
        }     
        
        return false;
    }

    /**
     * Called upon migration plan enforcement.
     */
    @Override
    public void onMigrationPlanEnforced() 
    {
        setIdle();
    }
    
    /**
     * Wakeup local controller.
     * 
     * @param localController   The local controller
     * @return                  true if everything ok, false otherwise
     */
    @Override
    public boolean onWakeupLocalController(LocalControllerDescription localController)
    {
        log_.debug(String.format("Entering on wakeup local controller: %s", localController.getId()));    
        boolean isWokenUp = wakeupResources_.wakeupLocalController(localController);
        if (!isWokenUp)
        {  
            return false;
        }
        
        wakeupResources_.sleep();
        return true;
    }
    
    /**
     * Wakeup local controllers.
     * 
     * @param localControllers   The local controllers
     * @return                   true if everything ok, false otherwise
     */
    @Override
    public boolean onWakeupLocalControllers(List<LocalControllerDescription> localControllers)
    {
        log_.debug("Entering on wakeup local controllers");  
        boolean isWokenUp = false;
        try 
        {
            isWokenUp = wakeupResources_.wakeupLocalControllers(localControllers);
        } 
        catch (InterruptedException exception) 
        {
            log_.error("Interrupted", exception);
        }
        finally
        {
            if (!isWokenUp)
            {  
                return false;
            }
        }
        
        wakeupResources_.sleep();
        return true;
    }
    
    /**
     * Called upon virtual machine submission finished.
     */
    @Override
    public void onVirtualMachineSubmissionFinished() 
    {
        setIdle();
    }
    
    /**
     * Called on anomaly resolved.
     * 
     * @param localController    The local controller
     */
    @Override
    public void onAnomalyResolved(LocalControllerDescription localController) 
    {   
        if (localController != null)
        {
            log_.debug(String.format("Entering on anomaly resolved for local controller: %s", 
                    localController.getId()));
            
            PowerSavingAction action = energyManagementSettings_.getPowerSavingAction();
            log_.debug(String.format("Power saving action to be executed: %s", action));
            EnergySaverUtils.powerCycleLocalController(localController, action, repository_);        
        }
        setIdle();
    }

    /**
     * Called on anomaly resolved.
     * 
     */
    @Override
    public void onAnomalyResolved() 
    {   
        setIdle();
        log_.debug("Anomaly resoved (no power saving)");
    }
    /**
     * Returns virtual machine submission response.
     * 
     * @param taskIdentifier    The task identifier
     * @return                  The submission response
     */
    @Override
    public VirtualMachineSubmissionResponse getVirtualMachineSubmissionResponse(String taskIdentifier) 
    {
        return virtualMachineManager_.getVirtualMachineSubmissionResponse(taskIdentifier);
    }


    
    
    /**
     * Starts the migration of the vm.
     * @param migrationRequest migrationRequest
     * 
     * 
     * @return     true if everything ok, false otherwise
     */
    @Override
    public boolean startMigration(MigrationRequest migrationRequest)
    {
        log_.debug("Starting the migration procedure");
        
        if (!changeState(SystemState.RECONFIGURATION))
        {
            return false;
        }
        
        try
        {
            migrationPlanEnforcer_.startManualMigration(migrationRequest);
        }
        catch (Exception exception) 
        {
            setIdle();
            log_.debug(String.format("Unable to execute the migration plan: %s", exception.getMessage()));
            return false;
        }
        
        return true;
    }

    @Override
    public VirtualMachineMetaData resizeVirtualMachine(ResizeRequest resizeRequest)
    {
        log_.debug("Starting a resize request");
        
        if (!changeState(SystemState.MANAGEMENT))
        {
            return null;
        }   
        
        setIdle();
        VirtualMachineMetaData newVirtualMachineMetaData = 
                virtualMachineManager_.resizeVirtualMachine(resizeRequest);
        
        VirtualMachineMetaData virtualMachine = 
                repository_.getVirtualMachineMetaData(resizeRequest.getVirtualMachineLocation(), 0);
        virtualMachine.setRequestedCapacity(newVirtualMachineMetaData.getRequestedCapacity());
        return newVirtualMachineMetaData;
        
    }
}
