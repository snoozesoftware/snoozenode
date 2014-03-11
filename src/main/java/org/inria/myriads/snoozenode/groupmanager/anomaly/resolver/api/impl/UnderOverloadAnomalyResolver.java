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
package org.inria.myriads.snoozenode.groupmanager.anomaly.resolver.api.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.scheduler.RelocationSettings;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.exception.AnomalyResolverException;
import org.inria.myriads.snoozenode.exception.NodeConfiguratorException;
import org.inria.myriads.snoozenode.groupmanager.anomaly.resolver.api.AnomalyResolver;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.GroupManagerPolicyFactory;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.ReconfigurationPlan;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.relocation.api.VirtualMachineRelocation;
import org.inria.myriads.snoozenode.groupmanager.migration.MigrationPlanEnforcer;
import org.inria.myriads.snoozenode.groupmanager.migration.listener.MigrationPlanListener;
import org.inria.myriads.snoozenode.groupmanager.statemachine.api.StateMachine;
import org.inria.myriads.snoozenode.localcontroller.monitoring.enums.LocalControllerState;
import org.inria.snoozenode.external.notifier.ExternalNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Anomaly resolver.
 * 
 * @author Eugen Feller, Matthieu simonin
 */
public final class UnderOverloadAnomalyResolver extends AnomalyResolver implements MigrationPlanListener
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(UnderOverloadAnomalyResolver.class);
    
    /** Overload relocation policy. */
    private VirtualMachineRelocation overloadRelocationPolicy_;

    /** Underload relocation policy. */
    private VirtualMachineRelocation underloadRelocationPolicy_;
   
    /** Anomaly local controller. */
    private LocalControllerDescription anomalyLocalController_;

    /** 
     * Number of monitoring entries to consider in estimation 
     * (overrides resourceDemandEstimator)
     **/
    private int numberOfMonitoringEntries_;

    private int count_;

    /**
     * Constructor.
     */
    public UnderOverloadAnomalyResolver()
    {
        
    }
    
    
    @Override
    public void initialize() 
    {
        String overloadPolicy = options_.get("overloadpolicy");
        if (overloadPolicy == null)
        {
//            throw new NodeConfiguratorException("Unable to find mandatory option : overloadpolicy");
        }
        overloadRelocationPolicy_ = 
                GroupManagerPolicyFactory.newVirtualMachineRelocation(overloadPolicy,
                                                                      estimator_);
        String underloadPolicy = options_.get("underloadpolicy");
        if (underloadPolicy == null)
        {
//            throw new NodeConfiguratorException("Unable to find mandatory option : overloadpolicy");
        }
        
        underloadRelocationPolicy_ = 
                GroupManagerPolicyFactory.newVirtualMachineRelocation(overloadPolicy,
                                                                      estimator_);
        
        // TODO get it from the options.
        numberOfMonitoringEntries_ = estimator_.getNumberOfMonitoringEntries();
    }
    
    /**
     * Computes the relocation plan.
     * 
     * @param localControllerState          The local controller state
     * @param anomalyLocalController        The anomaly local controller description
     * @param destinationLocalControllers   The destination local controller descriptions
     * @return                              The migration plan
     */
    private ReconfigurationPlan computeRelocationPlan(LocalControllerState localControllerState, 
                                                LocalControllerDescription anomalyLocalController, 
                                                List<LocalControllerDescription> destinationLocalControllers)
    {
        Guard.check(localControllerState, anomalyLocalController, destinationLocalControllers);
        log_.debug(String.format("Computing migration plan for %s local controller: %s", 
                                 localControllerState, anomalyLocalController.getId()));
        
        ReconfigurationPlan relocationPlan = null;
        switch (localControllerState)
        {                
            case OVERLOADED:
                relocationPlan = overloadRelocationPolicy_.relocateVirtualMachines(anomalyLocalController,
                                                                                   destinationLocalControllers);
                break;
                
            case UNDERLOADED:
                relocationPlan = underloadRelocationPolicy_.relocateVirtualMachines(anomalyLocalController,
                                                                                    destinationLocalControllers);
                break;
            default:
                log_.error(String.format("Unsupported state: %s", localControllerState));
                break;     
        }
        
        return relocationPlan;
    }
    
    /**
     * Returns the destination local controller descriptions.
     * 
     * @param state     The lcoal controller state
     * @return          The local controller descriptions
     */
    private List<LocalControllerDescription> getDestinationLocalControllers(LocalControllerState state)
    {
        log_.debug(String.format("Returning %s local controllers", state));
        
        List<LocalControllerDescription> destination;
        if (state.equals(LocalControllerState.OVERLOADED))
        {
            log_.debug("Getting all local controllers (including PASSIVE)");
            destination = repository_.getLocalControllerDescriptions(numberOfMonitoringEntries_, 
                                                                                 false,
                                                                                 true);
            return destination;
        }
        
        log_.debug("Getting all local controllers (excluding PASSIVE)");
        destination = repository_.getLocalControllerDescriptions(numberOfMonitoringEntries_,
                                                                             true,
                                                                             true);
        return destination;
    }

    
    /**
     * Called to resolve anomaly.
     * 
     * @param localControllerId     The anomaly local controller identifier
     * @param anomaly                 The local controller state
     * @throws Exception            The exception
     */
    public synchronized void resolveAnomaly(LocalControllerDescription anomalyLocalController, Object anomalyObject)
        throws Exception
    {
        Guard.check(anomalyLocalController, anomalyObject);
        log_.debug("Starting anomaly resolution");
              
        // Cast here
        LocalControllerState anomaly = (LocalControllerState) anomalyObject;
        log_.debug("The anomaly is " + String.valueOf(anomaly));
       
        if (anomalyLocalController == null)
        {
            throw new AnomalyResolverException("Local controller description is not available!");
        }
               
        List<LocalControllerDescription> destinationControllers = getDestinationLocalControllers(anomaly);
        if (destinationControllers == null)
        {
            throw new AnomalyResolverException("Destination local controller descriptions are not available!");
        }
        
        ReconfigurationPlan migrationPlan = 
                computeRelocationPlan(anomaly, anomalyLocalController, destinationControllers);  
        if (migrationPlan == null)
        {
            throw new AnomalyResolverException("Migration plan is not available!");
        }
                
        if (migrationPlan.getMapping().size() > 0)
        {
            log_.debug(String.format("%s relocation started!", anomaly));
        }
        
        List<LocalControllerDescription> passiveLocalControllers = getPassiveLocalControllers(migrationPlan);
        if (passiveLocalControllers.size() > 0)
        {
            log_.debug("Migration plan has PASSIVE local controllers!");
            boolean isWokenUp = stateMachine_.onWakeupLocalControllers(passiveLocalControllers);
            if (!isWokenUp)
            {
                throw new AnomalyResolverException("Failed to wakeup local controllers!");
            }
        }
        
        if (anomaly.equals(LocalControllerState.UNDERLOADED))
        {
            anomalyLocalController_ = anomalyLocalController;
        }
        
        MigrationPlanEnforcer migrationPlanExecutor = 
                new MigrationPlanEnforcer(externalNotifier_, repository_, this);
        migrationPlanExecutor.enforceMigrationPlan(migrationPlan);
    }
    
    /**
     * Called when migration plan was enforced.
     */
    public void onMigrationPlanEnforced()
    {
        log_.debug("Entering on migration plan enforced!");
        stateMachine_.onAnomalyResolved(anomalyLocalController_);
    }
    
    /**
     * Checking migration plan for passive local controllers.
     * 
     * @param migrationPlan     The migration plan
     * @return                  The list of passive local controllers
     */
    private List<LocalControllerDescription> getPassiveLocalControllers(ReconfigurationPlan migrationPlan) 
    {
        log_.debug("Checking for passive local controllers in the migration plan");
        
        Map<VirtualMachineMetaData, LocalControllerDescription> mapping = migrationPlan.getMapping();
        Map<String, LocalControllerDescription> passiveControllers = 
            new HashMap<String, LocalControllerDescription>();
        for (LocalControllerDescription localController : mapping.values())
        {
            String localControllerId = localController.getId();
            LocalControllerStatus status = localController.getStatus();
            if (status.equals(LocalControllerStatus.PASSIVE))
            {
                passiveControllers.put(localControllerId, localController);
            }
        }
        
        List<LocalControllerDescription> localControllers = 
            new ArrayList<LocalControllerDescription>(passiveControllers.values());
        return localControllers;
    }


    @Override
    public synchronized boolean readyToResolve(String anomalyLocalControllerId, Object anomalyObject)
    {
        return true;
    }
   

}
