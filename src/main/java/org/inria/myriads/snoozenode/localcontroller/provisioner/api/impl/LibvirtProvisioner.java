package org.inria.myriads.snoozenode.localcontroller.provisioner.api.impl;

import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.HypervisorSettings;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.parser.VirtualClusterParserFactory;
import org.inria.myriads.snoozecommon.parser.api.VirtualClusterParser;
import org.inria.myriads.snoozenode.configurator.imagerepository.ImageRepositorySettings;
import org.inria.myriads.snoozenode.configurator.provisioner.ProvisionerSettings;
import org.inria.myriads.snoozenode.localcontroller.provisioner.api.VirtualMachineProvisioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author msimonin
 *
 */
public class LibvirtProvisioner implements VirtualMachineProvisioner
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LibvirtProvisioner.class);
    
    /** hypervisor settings. */
    private HypervisorSettings hypervisorSettings_;

    /** image repository settings. */
    private ImageRepositorySettings imageSettings_;
    
    /** next vnc port to use.*/
    private int incrementVncPort_;

    /** provisioner settings.*/
    private ProvisionerSettings provisionerSettings_;
    
    /**
     * 
     * Constructor.
     * 
     * @param provisionerSettings   The provisioner settings.
     * @param hypervisorSettings    The hypervisor settings.
     * @param imageSettings         The image settings.
     */
    public LibvirtProvisioner(
            ProvisionerSettings provisionerSettings, HypervisorSettings hypervisorSettings,
            ImageRepositorySettings imageSettings
            )
    {
       provisionerSettings_ =  provisionerSettings;
       hypervisorSettings_ = hypervisorSettings;
       imageSettings_ = imageSettings;
       incrementVncPort_ = 0;
    }




    @Override
    public boolean provision(VirtualMachineMetaData virtualMachine)
    {
        log_.debug("Starting to provision the virtual machine");
        VirtualClusterParser parser = 
                VirtualClusterParserFactory.newVirtualClusterParser();
        
        String xmlDescription = virtualMachine.getXmlRepresentation();
        String xmlDesc = xmlDescription;
        
        log_.debug("Removing previous disks");
        xmlDescription = virtualMachine.getXmlRepresentation();
        xmlDesc = parser.removeDisk(xmlDescription, virtualMachine.getImage().getName());
        virtualMachine.setXmlRepresentation(xmlDesc);
        
        log_.debug("Adding the disk");
        xmlDescription = virtualMachine.getXmlRepresentation();
        String bus = provisionerSettings_.getFirstHdSettings().getDiskBusType();
        String dev = provisionerSettings_.getFirstHdSettings().getDiskDevice();
        xmlDesc = parser.addDiskImage(xmlDescription, virtualMachine.getImage(), bus, dev);
        virtualMachine.setXmlRepresentation(xmlDesc);
        
        log_.debug("Adding the cdrom contextualization file");
        xmlDescription = virtualMachine.getXmlRepresentation();
        bus = provisionerSettings_.getFirstCdSettings().getDiskBusType();
        dev = provisionerSettings_.getFirstCdSettings().getDiskDevice();
        xmlDesc = parser.addCdRomImage(xmlDescription, imageSettings_.getSource() + "/context.iso", bus, dev);
        virtualMachine.setXmlRepresentation(xmlDesc);
        
        log_.debug("set the domain type");
        xmlDescription = virtualMachine.getXmlRepresentation();
        xmlDesc = parser.setDomainType(xmlDescription, hypervisorSettings_.getDriver());
        virtualMachine.setXmlRepresentation(xmlDesc);
        
        log_.debug("set the os type");
        xmlDescription = virtualMachine.getXmlRepresentation();
        xmlDesc = parser.setOsType(xmlDescription, hypervisorSettings_.getDriver());
        virtualMachine.setXmlRepresentation(xmlDesc);
        
        if (provisionerSettings_.getVncSettings().isEnableVnc())
        {
            log_.debug("Enable vnc");
            xmlDescription = virtualMachine.getXmlRepresentation();
            int startPort = provisionerSettings_.getVncSettings().getStartPort();
            int portRange = provisionerSettings_.getVncSettings().getVncPortRange();
            String keymap = provisionerSettings_.getVncSettings().getKeymap();
            String listenAddress = provisionerSettings_.getVncSettings().getListenAddress();
            xmlDesc = parser.addGraphics(
                    xmlDescription,
                    "vnc",
                    listenAddress,
                    String.valueOf(startPort + incrementVncPort_),
                    keymap
                    );
            virtualMachine.setXmlRepresentation(xmlDesc);
            incrementVncPort_ = (incrementVncPort_ + 1) % portRange;
        }
        
        if (provisionerSettings_.isEnableSerial())
        {
            log_.debug("Adding the serial console");
            xmlDescription = virtualMachine.getXmlRepresentation();
            xmlDesc = parser.addSerial(xmlDescription, "pty", "0");
            virtualMachine.setXmlRepresentation(xmlDesc);
            
            xmlDescription = virtualMachine.getXmlRepresentation();
            xmlDesc = parser.addConsole(xmlDescription, "pty", "0", "serial");
            virtualMachine.setXmlRepresentation(xmlDesc);
        }
        
        return true;
    }

}
