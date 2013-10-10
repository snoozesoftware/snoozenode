/**
 * Copyright (C) 2010-2013 Eugen Feller, INRIA <eugen.feller@inria.fr>
 *
 * This file is part of Snooze, a scalable, autonomic, and
 * energy-aware virtual machine (VM) management framework.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package org.inria.myriads.snoozenode.localcontroller.actuator.api.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.inria.myriads.libvirt.domain.LibvirtConfigDomain;
import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.HypervisorSettings;
import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.MigrationMethod;
import org.inria.myriads.snoozecommon.communication.virtualcluster.migration.MigrationRequest;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.exception.ConnectorException;
import org.inria.myriads.snoozenode.localcontroller.actuator.api.VirtualMachineActuator;
import org.inria.myriads.snoozenode.localcontroller.connector.Connector;
import org.inria.myriads.snoozenode.localcontroller.connector.util.LibVirtUtil;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * Implementation of the libvirt-based host actuator communicator.
 * 
 * @author Eugen Feller
 */
public final class LibVirtVirtualMachineActuator 
    implements VirtualMachineActuator 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LibVirtVirtualMachineActuator.class);
        
    /** Connection to the hypervisor. */
    private Connect connect_;
        
    /**
     * Constructor.
     * 
     * @param connector                             The connector
     */
    public LibVirtVirtualMachineActuator(Connector connector)
    {
        Guard.check(connector);
        log_.debug("Initializing the libvirt infrastructure communicator");
        
        connect_ = (Connect) connector.getConnector();
    }
    
    /**
     * Checks if a virtual machine is active.
     * 
     * @param virtualMachineId      The virtual machine identifier
     * @return                      true if active, false otherwise
     */
    @Override
    public boolean isActive(String virtualMachineId)
    {
        Guard.check(virtualMachineId);
        log_.debug(String.format("Checking if virtual machine: %s is active", virtualMachineId));
        
        Domain domain = null;
        try 
        {           
            domain = connect_.domainLookupByName(virtualMachineId);
        } 
        catch (LibvirtException exception) 
        {
            log_.debug(String.format("Error during virtual machine lookup: %s", 
                                     exception.getMessage()));
        } 
        finally
        {
            if (domain != null)
            {
                return true;
            }
        }
        
        return false;       
    }
    
    /**
     * Launches a new Linux guest domain based on XML description.
     * 
     * @param xmlDescription        XML description of the domain
     * @return                      true if everything ok, false otherwise
     */
    @Override
    public boolean start(String xmlDescription)
    {
        Guard.check(xmlDescription);
        
        String newXmlDescription = null;
        //fill with capabilities before hard coded to test.
        try
        {
            JAXBContext context_ = JAXBContext.newInstance(LibvirtConfigDomain.class);
            Unmarshaller jaxbUnmarshaller = context_.createUnmarshaller();
            InputStream input = new ByteArrayInputStream(xmlDescription.getBytes());
            LibvirtConfigDomain domain = (LibvirtConfigDomain) jaxbUnmarshaller.unmarshal(input);
            domain.setType("kvm");
            //marshal it again
            Marshaller jaxbMarshaller = context_.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            jaxbMarshaller.marshal(domain, stream);
            newXmlDescription = stream.toString();
        }
        catch (Exception e)
        {
            log_.error("Unable to add capabilities to vm xml desc");
            e.printStackTrace();
        }
        
        
        log_.debug(String.format("Creating domain from XML: %s", newXmlDescription));
        
        if (newXmlDescription == null)
        {
            log_.debug("XML description is empty!!");
            return false;
        }
        
        try 
        {   
            connect_.domainCreateLinux(newXmlDescription, 0);
        } 
        catch (LibvirtException exception) 
        {
            log_.debug(String.format("Libvirt exception happened: %s", exception.getMessage()));
            return false;
        }
        
        log_.debug("Domain created successfully!");
        
        return true;
    }
    
    /**
     * Suspend a virtual machine.
     * 
     * @param virtualMachineId      The virtual machine identifier
     * @return                      true if everything ok, false otherwise
     */
    @Override
    public boolean suspend(String virtualMachineId) 
    {
        Guard.check(virtualMachineId);
        log_.debug(String.format("Suspending virtual machine: %s", virtualMachineId));
        
        try 
        {
            connect_.domainLookupByName(virtualMachineId).suspend();
        } 
        catch (LibvirtException exception) 
        {
            log_.debug(String.format("Unable to suspend virtual machine: %s", 
                                     exception.getMessage()));
            return false;
        }
        
        return true;
    }
  
    /**
     * Resume a virtual machine.
     * 
     * @param virtualMachineId      The virtual machine identifier
     * @return                      true if everything ok, false otherwise
     */
    @Override
    public boolean resume(String virtualMachineId) 
    {
        Guard.check(virtualMachineId);
        log_.debug(String.format("Resuming virtual machine: %s", virtualMachineId));
        
        try 
        {
            connect_.domainLookupByName(virtualMachineId).resume();
        } 
        catch (LibvirtException exception) 
        {
            log_.debug(String.format("Unable to resume virtual machine: %s",
                                     exception.getMessage()));
            return false;
        }
        
        return true;
    }

    /**
     * Shutdown a virtual machine.
     * 
     * @param virtualMachineId      The virtual machine identifier
     * @return                      true if everything ok, false otherwise
     */
    @Override
    public boolean shutdown(String virtualMachineId) 
    {
        Guard.check(virtualMachineId);
        log_.debug(String.format("Shutting down virtual machine: %s", virtualMachineId));
        
        try 
        {
            connect_.domainLookupByName(virtualMachineId).shutdown();
            
        } 
        catch (LibvirtException exception) 
        {
            log_.debug(String.format("Unable to shutdown virtual machine: %s", 
                                     exception.getMessage()));
            return false;
        }
        
        log_.debug("Shutdown was successfull");
        
        return true;
    }

    /** 
     * Reboot virtual machine. 
     * 
     * @param virtualMachineId    The virtual machine identifier
     * @return                    true if everything ok, false otherwise
     */
    @Override
    public boolean reboot(String virtualMachineId)
    {
        Guard.check(virtualMachineId);
        log_.debug(String.format("Rebooting virtual machine: %s", virtualMachineId));
        try 
        {
            connect_.domainLookupByName(virtualMachineId).reboot(0);
        } 
        catch (LibvirtException exception) 
        {
            log_.debug(String.format("Unable to reboot virtual machine: %s", 
                                     exception.getMessage()));
            return false;
        }
        
        log_.debug("Shutdown was successfull");
        
        return true;
    }
    
    /**
     * Shutdown a virtual machine.
     * 
     * @param virtualMachineId      The virtual machine identifier
     * @return                      true if everything ok, false otherwise
     */
    @Override
    public boolean destroy(String virtualMachineId) 
    {
        Guard.check(virtualMachineId);
        log_.debug(String.format("Destrying virtual machine: %s", virtualMachineId));
        
        try 
        {
            connect_.domainLookupByName(virtualMachineId).destroy();
        } 
        catch (LibvirtException exception) 
        {
            log_.debug(String.format("Unable to destroy virtual machine: %s",
                                      exception.getMessage()));
            return false;
        }
        
        log_.debug("Destroy was successfull");
        
        return true;
    }

    /**
     * Migrates a virtual machine to the destination local controller.
     * 
     * @param migrationRequest      The migration request
     * @return                      true if everything ok, false otherwise
     */
    @Override
    public boolean migrate(MigrationRequest migrationRequest) 
    {
        Guard.check(migrationRequest);
        String virtualMachineId =
            migrationRequest.getSourceVirtualMachineLocation().getVirtualMachineId();
        String destinationAddress = 
            migrationRequest.getDestinationVirtualMachineLocation().getLocalControllerControlDataAddress().getAddress();
        int destinationHypervisorPort = 
            migrationRequest.getDestinationHypervisorSettings().getPort();
        
        log_.debug(String.format("Migrating virtual machine: %s to local controller: %s: %d",
                                 virtualMachineId, 
                                 destinationAddress,
                                 destinationHypervisorPort));      
        try 
        {
            Domain domain = connect_.domainLookupByName(virtualMachineId);
            if (domain == null)
            {
                log_.debug("Such domain does not exist!");
                return false;
            }
            
            HypervisorSettings hypervisor = migrationRequest.getDestinationHypervisorSettings();
            MigrationMethod migrationMethod = hypervisor.getMigration().getMethod();
            log_.debug(String.format("Selected migration method is: %d", migrationMethod.getValue()));
            
            Connect remoteConnection = LibVirtUtil.connectToHypervisor(destinationAddress, hypervisor);
            Domain newDomain = domain.migrate(remoteConnection, migrationMethod.getValue(), null, null, 0);
            if (newDomain == null)
            {
                log_.debug("Error during live migration!");
                return false;
            }
        } 
        catch (LibvirtException exception) 
        {
            log_.error(String.format("Unable to migrate virtual machine: %s", exception.getMessage()));
            return false;
        } 
        catch (ConnectorException exception) 
        {
            log_.error(String.format("Error creating hypervisor connector: %s", exception.getMessage()));
            return false;
        }
        catch (Exception exception)
        {
            log_.error(String.format("General migration exception: %s", exception.getMessage()));
            return false;
        }
        
        return true;
    }

    /**
     * Dynamically changes the maximum amount of physical memory allocated to a virtual machine.
     * 
     * @param virtualMachineId      The virtual machine identifier
     * @param memory                The amount of memory to set
     * @return                      true if active, false otherwise
     */
    public boolean setMemory(String virtualMachineId, long memory)
    {
        Guard.check(virtualMachineId);
        log_.debug(String.format("Set memory of virtual machine : %s to %d", virtualMachineId, memory));
        
        try 
        {
            connect_.domainLookupByName(virtualMachineId).setMemory(memory);
        } 
        catch (LibvirtException exception) 
        {
            log_.debug(String.format("Unable to set virtual machine memory: %s",
                                      exception.getMessage()));
            return false;
        }
        
        log_.debug("Memory set");
        
        return true;
    }

    /**
     * Dynamically changes the maximum vcpu allocated to a virtual machine.
     * 
     * @param virtualMachineId      The virtual machine identifier
     * @param vcpu                  The number of vcpu to set
     * @return                      true if active, false otherwise
     */
    public boolean setVcpu(String virtualMachineId, int vcpu)
    {
        Guard.check(virtualMachineId);
        log_.debug(String.format("Set vcpu of virtual machine: %s", virtualMachineId));
        
        try 
        {
            connect_.domainLookupByName(virtualMachineId).setVcpus(vcpu);
        } 
        catch (LibvirtException exception) 
        {
            log_.debug(String.format("Unable to set virtual machine vcpu : %s",
                                      exception.getMessage()));
            return false;
        }
        
        log_.debug("VCPU set");
        
        return true;
    }



}
