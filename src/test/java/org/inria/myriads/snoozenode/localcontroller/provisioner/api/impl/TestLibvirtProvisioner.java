package org.inria.myriads.snoozenode.localcontroller.provisioner.api.impl;

import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.HypervisorDriver;
import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.HypervisorSettings;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.virtualmachineimage.VirtualMachineImage;
import org.inria.myriads.snoozenode.configurator.imagerepository.ImageRepositorySettings;
import org.inria.myriads.snoozenode.configurator.provisioner.ProvisionerSettings;

import junit.framework.TestCase;

public class TestLibvirtProvisioner extends TestCase
{
    
    public void testQemuHypervisor()
    {
                
        ProvisionerSettings provisionerSettings = new ProvisionerSettings();
        
        HypervisorSettings hypervisorSettings = new HypervisorSettings();
        hypervisorSettings.setDriver(HypervisorDriver.qemu);
        ImageRepositorySettings imageSettings = new ImageRepositorySettings();
        
        LibvirtProvisioner libvirtProvisioner = new LibvirtProvisioner(
                provisionerSettings,
                hypervisorSettings,
                imageSettings
                );
        
        VirtualMachineMetaData virtualMachine = new VirtualMachineMetaData();
        virtualMachine.setXmlRepresentation("<domain></domain>");    
        
        libvirtProvisioner.provision(virtualMachine);
        String xml = virtualMachine.getXmlRepresentation();
        assertTrue(xml.contains("domain type=\"qemu\""));        
    }
    
    public void testKvmHypervisor()
    {
                
        ProvisionerSettings provisionerSettings = new ProvisionerSettings();
        
        HypervisorSettings hypervisorSettings = new HypervisorSettings();
        hypervisorSettings.setDriver(HypervisorDriver.kvm);
        ImageRepositorySettings imageSettings = new ImageRepositorySettings();
        
        LibvirtProvisioner libvirtProvisioner = new LibvirtProvisioner(
                provisionerSettings,
                hypervisorSettings,
                imageSettings
                );
        
        VirtualMachineMetaData virtualMachine = new VirtualMachineMetaData();
        virtualMachine.setXmlRepresentation("<domain></domain>");    
        
        libvirtProvisioner.provision(virtualMachine);
        String xml = virtualMachine.getXmlRepresentation();
        assertTrue(xml.contains("domain type=\"kvm\""));        
    }
    
}
