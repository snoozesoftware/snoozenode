package org.inria.myriads.snoozenode.idgenerator.api.impl;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozenode.idgenerator.api.IdGenerator;

/**
 * @author msimonin
 *
 */
public class ShortnameIdGenerator implements IdGenerator
{

    @Override
    public String generate(LocalControllerDescription localController)
    {
        String hostname = localController.getHostname();
        String shortname = hostname.split("\\.")[0];
        return shortname;
    }

}
