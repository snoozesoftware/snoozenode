package org.inria.myriads.snoozenode.idgenerator.api.impl;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozenode.idgenerator.api.IdGenerator;

/**
 * @author msimonin
 *
 */
public class HostnameIdGenerator implements IdGenerator
{

    @Override
    public String generate(LocalControllerDescription localController)
    {
       return localController.getHostname();
    }

}
