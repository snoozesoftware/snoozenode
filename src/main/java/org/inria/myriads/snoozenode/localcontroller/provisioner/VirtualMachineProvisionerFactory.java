package org.inria.myriads.snoozenode.localcontroller.provisioner;

import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.HypervisorSettings;
import org.inria.myriads.snoozenode.configurator.imagerepository.ImageRepositorySettings;
import org.inria.myriads.snoozenode.localcontroller.provisioner.api.VirtualMachineProvisioner;
import org.inria.myriads.snoozenode.localcontroller.provisioner.api.impl.LibvirtProvisioner;

public class VirtualMachineProvisionerFactory
{
    /**
     * Hide the consturctor.
     */
    private VirtualMachineProvisionerFactory() 
    {
        throw new UnsupportedOperationException();
    }
 
    
    public static VirtualMachineProvisioner newProvisioner(
            HypervisorSettings hypervisorSettings,
            ImageRepositorySettings imageSettings)
    {
        return new LibvirtProvisioner(hypervisorSettings, imageSettings);
    }
}
