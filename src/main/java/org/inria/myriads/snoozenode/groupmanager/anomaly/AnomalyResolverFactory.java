package org.inria.myriads.snoozenode.groupmanager.anomaly;

import java.lang.reflect.InvocationTargetException;

import org.inria.myriads.snoozenode.configurator.anomaly.AnomalyResolverSettings;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.anomaly.resolver.api.AnomalyResolver;
import org.inria.myriads.snoozenode.groupmanager.anomaly.resolver.api.impl.UnderOverloadAnomalyResolver;
import org.inria.myriads.snoozenode.groupmanager.statemachine.api.impl.GroupManagerStateMachine;
import org.inria.myriads.snoozenode.util.PluginUtils;
import org.inria.snoozenode.external.notifier.ExternalNotifier;
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
            ExternalNotifier externalNotifier,
            AnomalyResolverSettings anomalyResolverSettings,
            ResourceDemandEstimator estimator,
            GroupManagerRepository repository, 
            GroupManagerStateMachine groupManagerStateMachine) 
    {        
        String anomalyResolverName = anomalyResolverSettings.getName();
        
        AnomalyResolver anomalyResolver = null;
        if (anomalyResolverName.equals("underoverload"))
        {
            anomalyResolver = new UnderOverloadAnomalyResolver();
        }
        else
        {
            try
            {
                log_.debug("Loading custom anomaly resolver");
                Object anomalyResolverObject = PluginUtils.createFromFQN(anomalyResolverName);
                anomalyResolver = (AnomalyResolver) anomalyResolverObject;
            }
            catch(Exception e)
            {
                log_.error("Unable to load the custom anomaly resolver, falling back to default");
                anomalyResolver = new UnderOverloadAnomalyResolver();
            }
            
        }
        anomalyResolver.setAnomalyResolverSettings(anomalyResolverSettings);
        anomalyResolver.setStateMachine(groupManagerStateMachine);
        anomalyResolver.setRepository(repository);
        anomalyResolver.setEstimator(estimator);
        anomalyResolver.setOptions(anomalyResolverSettings.getOptions());
        anomalyResolver.initialize();
        anomalyResolver.setExternalNotifier(externalNotifier);
        return anomalyResolver;
    }
    
  
    
    
}
