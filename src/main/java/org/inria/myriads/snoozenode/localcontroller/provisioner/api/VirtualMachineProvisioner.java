package org.inria.myriads.snoozenode.localcontroller.provisioner.api;

import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;

/**
 * 
 * Virtual machine provisioner.
 * 
 * @author msimonin
 *
 */
public interface VirtualMachineProvisioner
{
    /**
     * 
     * Provision a virtual machine.
     * (generates a proper xml description).
     * 
     * @param virtualMachine    The virtual machine meta data.
     * @return  true iff everything is fine.
     */
    boolean provision(VirtualMachineMetaData virtualMachine);
    
}
