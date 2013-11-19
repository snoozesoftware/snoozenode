package org.inria.myriads.snoozenode.localcontroller.provisioner.api.impl;

import java.util.ArrayList;

import org.inria.myriads.libvirt.domain.LibvirtConfigSerialConsole;
import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.HypervisorSettings;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.parser.VirtualClusterParserFactory;
import org.inria.myriads.snoozecommon.parser.api.VirtualClusterParser;
import org.inria.myriads.snoozecommon.parser.api.impl.LibVirtXMLParser;
import org.inria.myriads.snoozenode.configurator.imagerepository.ImageRepositorySettings;
import org.inria.myriads.snoozenode.localcontroller.provisioner.api.VirtualMachineProvisioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibvirtProvisioner implements VirtualMachineProvisioner
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LibvirtProvisioner.class);
    
    /** hypervisor settings. */
    private HypervisorSettings hypervisorSettings_;

    /** image repository settings. */
    private ImageRepositorySettings imageSettings_;

    
    /**
     * 
     */
    public LibvirtProvisioner(
            HypervisorSettings hypervisorSettings,
            ImageRepositorySettings imageSettings
            )
    {
       hypervisorSettings_ = hypervisorSettings;
       imageSettings_ = imageSettings;
       
    }




    @Override
    public boolean provision(VirtualMachineMetaData virtualMachine)
    {
        log_.debug("Starting to provision the virtual machine");
        VirtualClusterParser parser = 
                VirtualClusterParserFactory.newVirtualClusterParser();
        
        log_.debug("Removing previous disks");
        String xmlDesc = parser.removeDisk(virtualMachine.getXmlRepresentation(), virtualMachine.getImage().getName());
        virtualMachine.setXmlRepresentation(xmlDesc);
        
        log_.debug("Adding the disk");
        xmlDesc = parser.addDiskImage(virtualMachine.getXmlRepresentation(),virtualMachine.getImage());
        virtualMachine.setXmlRepresentation(xmlDesc);
        
        log_.debug("Adding the serial console");
        String xmlDescription = virtualMachine.getXmlRepresentation();
        xmlDesc = parser.addSerial(xmlDescription, "pty", "0");
        virtualMachine.setXmlRepresentation(xmlDesc);
        
        log_.debug("Adding the cdrom contextualization file");
        xmlDescription = virtualMachine.getXmlRepresentation();
        xmlDesc = parser.addCdRomImage(xmlDescription, imageSettings_.getSource() + "/context.iso");
        virtualMachine.setXmlRepresentation(xmlDesc);
        
        xmlDescription = virtualMachine.getXmlRepresentation();
        xmlDesc = parser.addConsole(xmlDescription, "pty", "0", "serial");
        virtualMachine.setXmlRepresentation(xmlDesc);
        
        log_.debug("set the domain type");
        xmlDescription = virtualMachine.getXmlRepresentation();
        xmlDesc = parser.setDomainType(xmlDescription, hypervisorSettings_.getDriver());
        virtualMachine.setXmlRepresentation(xmlDesc);
        
        log_.debug("set the os type");
        xmlDescription = virtualMachine.getXmlRepresentation();
        xmlDesc = parser.setOsType(xmlDescription, hypervisorSettings_.getDriver());
        virtualMachine.setXmlRepresentation(xmlDesc);
        
        log_.debug("Enable vnc");
        xmlDescription = virtualMachine.getXmlRepresentation();
        xmlDesc = parser.addGraphics(xmlDescription, "vnc", "0.0.0.0", "5900");
        virtualMachine.setXmlRepresentation(xmlDesc);
        
        return true;
    }

}
