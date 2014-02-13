package org.inria.myriads.snoozenode.idgenerator.api.impl;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozenode.idgenerator.api.IdGenerator;

/**
 * @author msimonin
 *
 */
public class HostnamePortIdGenerator implements IdGenerator
{

    @Override
    public String generate(LocalControllerDescription localController)
    {
        int port = localController.getControlDataAddress().getPort();
        String hostname = localController.getHostname();
        
        return hostname + ":" + String.valueOf(port); 
    }

}
