package org.inria.myriads.snoozenode.groupmanager.anomaly;

import org.inria.myriads.snoozenode.configurator.scheduler.RelocationSettings;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.statemachine.api.impl.GroupManagerStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnomalyResolverFactory
{
    
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(AnomalyResolverFactory.class);
    
    /**
     * Hide Constructor
     */
    private AnomalyResolverFactory()
    {
        
    }

    public static AnomalyResolver newAnomalyresolver(
            RelocationSettings relocation,
            ResourceDemandEstimator estimator,
            GroupManagerRepository repository, 
            GroupManagerStateMachine groupManagerStateMachine)
    {
        AnomalyResolver anomalyResolver = new UnderOverloadAnomalyResolver();
        anomalyResolver.setRelocationSettings(relocation);
        anomalyResolver.setEstimator(estimator);
        anomalyResolver.setRepository(repository);
        anomalyResolver.setStateMachine(groupManagerStateMachine);
        anomalyResolver.initialize();
        return anomalyResolver;
    }
    
  
    
    
}
