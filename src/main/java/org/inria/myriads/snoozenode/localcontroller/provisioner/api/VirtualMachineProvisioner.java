package org.inria.myriads.snoozenode.localcontroller.provisioner.api;

import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;

public interface VirtualMachineProvisioner
{
    public boolean provision(VirtualMachineMetaData virtualMachine);
}
