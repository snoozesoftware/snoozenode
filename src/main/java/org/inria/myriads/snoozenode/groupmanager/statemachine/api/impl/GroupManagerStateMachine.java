/**
 * Copyright (C) 2010-2012 Eugen Feller, INRIA <eugen.feller@inria.fr>
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
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineSubmission;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.configurator.energymanagement.EnergyManagementSettings;
import org.inria.myriads.snoozenode.configurator.energymanagement.enums.PowerSavingAction;
import org.inria.myriads.snoozenode.configurator.estimator.EstimatorSettings;
import org.inria.myriads.snoozenode.configurator.scheduler.GroupManagerSchedulerSettings;
import org.inria.myriads.snoozenode.configurator.scheduler.RelocationSettings;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.exception.GroupManagerInitException;
import org.inria.myriads.snoozenode.groupmanager.anomaly.AnomalyResolver;
import org.inria.myriads.snoozenode.groupmanager.energysaver.EnergySaverFactory;
import org.inria.myriads.snoozenode.groupmanager.energysaver.util.EnergySaverUtils;
import org.inria.myriads.snoozenode.groupmanager.energysaver.wakeup.WakeupResources;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.GroupManagerPolicyFactory;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.enums.Reconfiguration;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.ReconfigurationPolicy;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.plan.MigrationPlan;
import org.inria.myriads.snoozenode.groupmanager.migration.MigrationPlanEnforcer;
import org.inria.myriads.snoozenode.groupmanager.migration.listener.MigrationPlanListener;
import org.inria.myriads.snoozenode.groupmanager.statemachine.SystemState;
import org.inria.myriads.snoozenode.groupmanager.statemachine.VirtualMachineCommand;
import org.inria.myriads.snoozenode.groupmanager.statemachine.api.StateMachine;
import org.inria.myriads.snoozenode.groupmanager.virtualmachinemanager.VirtualMachineManager;
import org.inria.myriads.snoozenode.localcontroller.monitoring.enums.LocalControllerState;
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
    
    /** 
     * Constructor. 
     * 
     * @param nodeConfiguration         The node configuration
     * @param estimator                 The resource demand estimator
     * @param repository                The repository
     */
    public GroupManagerStateMachine(NodeConfiguration nodeConfiguration,
                                    ResourceDemandEstimator estimator,
                                    GroupManagerRepository repository)
    {
        log_.debug("Initializing the state machine");
        systemState_ = SystemState.IDLE; 
        energyManagementSettings_ = nodeConfiguration.getEnergyManagement();
        estimatorSettings_ = nodeConfiguration.getEstimator();
        repository_ = repository;
        // Migration plan enforcer
        migrationPlanEnforcer_ = new MigrationPlanEnforcer(repository, this);
        // Wakeup 
        wakeupResources_ = createWakeupResources(energyManagementSettings_, repository);
        // Virtual machine manager
        virtualMachineManager_ = createVirtualMachineManager(nodeConfiguration, estimator, repository);
        // Anomaly
        GroupManagerSchedulerSettings schedulerSettings = nodeConfiguration.getGroupManagerScheduler();
        anomalyResolver_ = createAnomalyResolver(schedulerSettings.getRelocationSettings(), estimator, repository);
        // Reconfiguration
        Reconfiguration reconfiguration = schedulerSettings.getReconfigurationSettings().getPolicy();
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
    private AnomalyResolver createAnomalyResolver(RelocationSettings relocation, 
                                                  ResourceDemandEstimator estimator, 
                                                  GroupManagerRepository repository)
    {
        AnomalyResolver anomalyResolver = new AnomalyResolver(relocation, 
                                                              estimator, 
                                                              repository, 
                                                              this);
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
        GroupManagerSchedulerSettings settings = nodeConfiguration.getGroupManagerScheduler();
        VirtualMachineManager virtualMachineManager = new VirtualMachineManager(settings, 
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
    public String startVirtualMachines(VirtualMachineSubmission submissionRequest)
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
                repository_.getLocalControllerDescriptions(numberOfMonitoringEntries, true);
            if (localControllers == null)
            {
                throw new GroupManagerInitException("Local controllers list is not available!");
            }
                 
            if (localControllers.size() == 0)
            {
                throw new GroupManagerInitException("Local controller list is empty!");
            }
        
            MigrationPlan migrationPlan = reconfiguration_.reconfigure(localControllers);
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
    public void resolveAnomaly(String localControllerId, LocalControllerState state) 
    {
        log_.debug(String.format("Starting to resolve ANOMALY (%s) situation!", state));
        
        if (!changeState(SystemState.RELOCATION))
        {
            return;
        }
        
        try 
        {
            anomalyResolver_.resolveAnomaly(localControllerId, state);
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
        log_.debug(String.format("Entering on anomaly resolved for local controller: %s", 
                                  localController.getId()));
        if (localController != null)
        {
            PowerSavingAction action = energyManagementSettings_.getPowerSavingAction();
            log_.debug(String.format("Power saving action to be executed: %s", action));
            EnergySaverUtils.powerCycleLocalController(localController, action, repository_);        
        }
       
        setIdle();
    }

    /**
     * Returns virtual machine submission response.
     * 
     * @param taskIdentifier    The task identifier
     * @return                  The response
     */
    @Override
    public VirtualMachineSubmission getVirtualMachineResponse(String taskIdentifier) 
    {
        return virtualMachineManager_.getVirtualMachineResponse(taskIdentifier);
    }
}
