package org.inria.myriads.snoozenode.localcontroller.imagemanager.api;

import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;

public interface ImageManager
{
    boolean fetchImage(VirtualMachineMetaData virtualMachine);
}
