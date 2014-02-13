package org.inria.myriads.snoozenode.idgenerator.api;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;

/**
 * @author msimonin
 *
 */
public interface IdGenerator
{

    
    /**
     * 
     * Generates a new id by side effect.
     * 
     * @param localController   The localControllerDescription
     * @return  the id.
     */
    String generate(LocalControllerDescription localController);
    
}
