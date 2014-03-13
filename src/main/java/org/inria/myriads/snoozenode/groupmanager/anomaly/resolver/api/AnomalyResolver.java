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
package org.inria.myriads.snoozenode.groupmanager.anomaly.resolver.api;

import java.util.HashMap;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozenode.configurator.anomaly.AnomalyResolverSettings;
import org.inria.myriads.snoozenode.configurator.scheduler.RelocationSettings;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.exception.NodeConfiguratorException;
import org.inria.myriads.snoozenode.groupmanager.anomaly.listener.AnomalyResolverListener;
import org.inria.myriads.snoozenode.groupmanager.statemachine.api.StateMachine;
import org.inria.snoozenode.external.notifier.ExternalNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Anomaly resolver.
 * 
 * @author Eugen Feller
 */
public abstract class AnomalyResolver 
    implements AnomalyResolverListener
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(AnomalyResolver.class);
    
    /** State machine. */
    protected StateMachine stateMachine_;
    
    /** Anomaly Resolver Settings.*/
    protected AnomalyResolverSettings anomalyResolverSettings_;
    
    /** Estimator. */
    protected ResourceDemandEstimator estimator_;
    
    /** GroupManager Repository.*/
    protected GroupManagerRepository repository_;
    
    /** Options.*/
    protected Map<String, String> options_;
    
    /** TODO remove it (make it static)*/
    protected ExternalNotifier externalNotifier_;

    /**
     * Constructor.
     * 
     */
    public AnomalyResolver()
    {
        options_ = new HashMap<String, String>();
    }
    
    /**
     * Specific initialization here.
     * Called just after constructor in the factory.
     */
    public abstract void initialize();
    
    
    /**
     * 
     * Tells the state machine if the anomaly resolver is ready to resolve.
     * 
     * @param localControllerId         The local controller id
     * @param anomalyObject             The anomaly object.
     * @return  true iff the resolver is ready to resolve.
     */
    public abstract boolean readyToResolve(String localControllerId, Object anomalyObject);
    
    /**
     * Called to resolve anomaly.
     * 
     * @param localController       The anomaly local controller
     * @param anomaly               The local controller state
     * @throws Exception            The exception
     */
    public abstract void resolveAnomaly(LocalControllerDescription localController, Object anomaly) throws Exception;


    
    
    
    
    /**
     * 
     * Gets the number of monitoring entries to consider.
     * By default global settings are applied.
     * 
     * @return  the number of monitoring entries.
     */
    public int getNumberOfMonitoringEntries()
    {
        return estimator_.getNumberOfMonitoringEntries();
    }
    
    @Override
    public void onAnomalyResolved(LocalControllerDescription anomalyLocalController)
    {
        stateMachine_.onAnomalyResolved(anomalyLocalController);
    }

    /**
     * @return the stateMachine
     */
    public StateMachine getStateMachine()
    {
        return stateMachine_;
    }

    /**
     * @param stateMachine the stateMachine to set
     */
    public void setStateMachine(StateMachine stateMachine)
    {
        stateMachine_ = stateMachine;
    }

    /**
     * @return the estimator
     */
    public ResourceDemandEstimator getEstimator()
    {
        return estimator_;
    }

    /**
     * @param estimator the estimator to set
     */
    public void setEstimator(ResourceDemandEstimator estimator)
    {
        estimator_ = estimator;
    }

    /**
     * @return the repository
     */
    public GroupManagerRepository getRepository()
    {
        return repository_;
    }

    /**
     * @param repository the repository to set
     */
    public void setRepository(GroupManagerRepository repository)
    {
        repository_ = repository;
    }

    /**
     * @return the options
     */
    public Map<String, String> getOptions()
    {
        return options_;
    }

    /**
     * @param options the options to set
     */
    public void setOptions(Map<String, String> options)
    {
        options_ = options;
    }

    /**
     * @return the anomalyResolverSettings
     */
    public AnomalyResolverSettings getAnomalyResolverSettings()
    {
        return anomalyResolverSettings_;
    }

    /**
     * @param anomalyResolverSettings the anomalyResolverSettings to set
     */
    public void setAnomalyResolverSettings(AnomalyResolverSettings anomalyResolverSettings)
    {
        anomalyResolverSettings_ = anomalyResolverSettings;
    }

    /**
     * @return the externalNotifier
     */
    public ExternalNotifier getExternalNotifier()
    {
        return externalNotifier_;
    }

    /**
     * @param externalNotifier the externalNotifier to set
     */
    public void setExternalNotifier(ExternalNotifier externalNotifier)
    {
        externalNotifier_ = externalNotifier;
    }


    
}
