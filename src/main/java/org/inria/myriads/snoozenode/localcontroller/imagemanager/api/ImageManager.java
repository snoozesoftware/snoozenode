package org.inria.myriads.snoozenode.localcontroller.imagemanager.api;

import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;

/**
 * 
 * 
 * @author msimonin
 *
 */
public interface ImageManager
{
    /**
     * 
     * Fetch the image.
     * 
     * @param virtualMachine    The virtual machine meta data.
     * @return  true iff the image has been successfully fetched.
     */
    boolean fetchImage(VirtualMachineMetaData virtualMachine);
}
