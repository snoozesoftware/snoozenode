package org.inria.myriads.snoozenode.localcontroller.provisioner;

import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.HypervisorSettings;
import org.inria.myriads.snoozenode.configurator.imagerepository.ImageRepositorySettings;
import org.inria.myriads.snoozenode.configurator.provisioner.ProvisionerSettings;
import org.inria.myriads.snoozenode.localcontroller.provisioner.api.VirtualMachineProvisioner;
import org.inria.myriads.snoozenode.localcontroller.provisioner.api.impl.LibvirtProvisioner;

/**
 * 
 * Virtual machine provisioner.
 * 
 * @author msimonin
 *
 */
public final class VirtualMachineProvisionerFactory
{
    /**
     * Hide the consturctor.
     */
    private VirtualMachineProvisionerFactory() 
    {
        throw new UnsupportedOperationException();
    }
 
    
    /**
     * 
     * Creates a new provisioner.
     * 
     * @param provisionerSettings   The provisioner settings.
     * @param hypervisorSettings    The hypervisor settings.
     * @param imageSettings         The image settings.
     * @return  Virtual Machine Provisioner.
     */
    public static VirtualMachineProvisioner newProvisioner(
            ProvisionerSettings provisionerSettings, HypervisorSettings hypervisorSettings,
            ImageRepositorySettings imageSettings)
    {
        return new LibvirtProvisioner(provisionerSettings, hypervisorSettings, imageSettings);
    }
}
