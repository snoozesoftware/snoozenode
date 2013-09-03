package org.inria.myriads.snoozenode.util;

import org.inria.snoozenode.external.notifier.ExternalNotificationType;
import org.inria.snoozenode.external.notifier.ExternalNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * External notifier utils.
 * 
 * @author msimonin
 *
 */
public final class ExternalNotifierUtils
{

     /** Define the logger. */
        private static final Logger log_ = LoggerFactory.getLogger(ExternalNotifierUtils.class);
     
        /**
         * Hide the consturctor.
         */
        private ExternalNotifierUtils() 
        {
            throw new UnsupportedOperationException();
        }
        
        /**
         * 
         * Sends.
         * 
         * @param externalNotifier  External notifier.
         * @param notificationType  Notification type.
         * @param message           Message.
         * @param routingKey        Routing key.
         */
        public static void send(
                ExternalNotifier externalNotifier,
                ExternalNotificationType notificationType,
                Object message,
                String routingKey
                )
        {
            try
            {
                externalNotifier.send(notificationType, message, routingKey);
            }
            catch (Exception e)
            {
                log_.warn("Impossible to send to external " + e.getMessage());
            }
        }

}
