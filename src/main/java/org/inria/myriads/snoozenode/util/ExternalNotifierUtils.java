package org.inria.myriads.snoozenode.util;

import org.inria.snoozenode.external.notifier.ExternalNotificationType;
import org.inria.snoozenode.external.notifier.ExternalNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalNotifierUtils
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
            catch(Exception e)
            {
                log_.warn("Impossible to send to external " + e.getMessage());
            }
        }

}
