package org.inria.myriads.snoozenode.groupmanager.anomaly.listener;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;

public interface AnomalyResolverListener
{
    /**
     * Called when migration plan was enforced.
     */
     void onAnomalyResolved(LocalControllerDescription localController);
   
}
