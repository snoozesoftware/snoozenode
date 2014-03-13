package org.inria.myriads.snoozenode.groupmanager.anomaly.listener;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;

/**
 *  
 * @author msimonin
 *
 */
public interface AnomalyResolverListener
{
    /**
    * 
    * Called when migration plan was enforced.
    * 
    * @param localController    The local controller description.
    */
    void onAnomalyResolved(LocalControllerDescription localController);
   
}
