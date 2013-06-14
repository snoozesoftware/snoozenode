package org.inria.snoozenode.external.notifier;

import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.configurator.monitoring.external.ExternalNotifierSettings;
import org.inria.myriads.snoozenode.eventmessage.EventMessage;
import org.inria.myriads.snoozenode.monitoring.datasender.DataSenderFactory;
import org.inria.myriads.snoozenode.monitoring.datasender.api.DataSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalNotifier
{
    
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(ExternalNotifier.class);
    
    /** management notifier. */
    private DataSender management_ ; 
    
    /** system notifier. */
    private DataSender system_; 
    
    /** monitoring notifier*/
    private DataSender monitoring_;

    /**
     * 
     */
    public ExternalNotifier(NodeConfiguration nodeConfiguration)
    {
        ExternalNotifierSettings externalNotifierSettings = nodeConfiguration.getExternalNotifier();
        management_ = DataSenderFactory.newExternalDataSender(
                ExternalNotificationType.MANAGEMENT.toString(),
                externalNotifierSettings);
        
        system_ = DataSenderFactory.newExternalDataSender(
                ExternalNotificationType.SYSTEM.toString(),
                externalNotifierSettings);
        
        monitoring_ = DataSenderFactory.newExternalDataSender(
                ExternalNotificationType.MONITORING.toString(),
                externalNotifierSettings);
    }
    
    public  void send(ExternalNotificationType notificationType, Object message, String routingKey)
    {
        try
        {
            switch(notificationType){
                case MANAGEMENT : 
                    management_.send(message, routingKey);
                    break;
                case SYSTEM :
                    system_.send(message, routingKey);
                    break;
                case MONITORING : 
                    monitoring_.send(message, routingKey);
            }
            
        }
        catch (Exception e)
        {
            log_.error("Failed to send external datas" + e.getMessage());
        } 
    }
    
    
    
    
    
}
