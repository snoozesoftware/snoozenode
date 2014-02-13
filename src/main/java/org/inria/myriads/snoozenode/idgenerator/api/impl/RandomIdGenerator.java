package org.inria.myriads.snoozenode.idgenerator.api.impl;

import java.util.UUID;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozenode.idgenerator.api.IdGenerator;

/**
 * @author msimonin
 *
 */
public class RandomIdGenerator implements IdGenerator
{

    @Override
    public String generate(LocalControllerDescription localController)
    {
        return UUID.randomUUID().toString();
    }

}
