package org.inria.myriads.snoozenode.groupmanager.anomaly;

import java.lang.reflect.InvocationTargetException;

import org.inria.myriads.snoozenode.configurator.anomaly.AnomalyResolverSettings;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.anomaly.resolver.api.AnomalyResolver;
import org.inria.myriads.snoozenode.groupmanager.anomaly.resolver.api.impl.UnderOverloadAnomalyResolver;
import org.inria.myriads.snoozenode.groupmanager.statemachine.api.impl.GroupManagerStateMachine;
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
        String classURI = anomalyResolverSettings.getName();
        ClassLoader classLoader = AnomalyResolverFactory.class.getClassLoader();
        
        AnomalyResolver anomalyResolver = null;
        try
        {
            Class<?> anomalyClass = classLoader.loadClass(classURI);
            Object anomalyResolverObject;
            anomalyResolverObject = anomalyClass.getConstructor().newInstance();
            anomalyResolver = (AnomalyResolver) anomalyResolverObject;
            log_.debug("Sucessfully created anomaly resolver" + classURI);
            
        }
        catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException e)
        {
            e.printStackTrace();
            anomalyResolver = new UnderOverloadAnomalyResolver();
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
