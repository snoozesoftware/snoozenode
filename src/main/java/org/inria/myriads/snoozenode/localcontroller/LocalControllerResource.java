/**
 * Copyright (C) 2010-2012 Eugen Feller, INRIA <eugen.feller@inria.fr>
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
package org.inria.myriads.snoozenode.localcontroller;

import java.util.ArrayList;

import org.inria.myriads.snoozecommon.communication.rest.api.LocalControllerAPI;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.migration.MigrationRequest;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineErrorCode;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineSubmissionRequest;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineSubmissionResponse;
import org.inria.myriads.snoozecommon.communication.virtualmachine.ResizeRequest;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.parser.VirtualClusterParserFactory;
import org.inria.myriads.snoozecommon.parser.api.VirtualClusterParser;
import org.inria.myriads.snoozenode.configurator.energymanagement.enums.PowerSavingAction;
import org.inria.myriads.snoozenode.util.ManagementUtils;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local controller resource.
 * 
 * @author Eugen Feller
 */
public final class LocalControllerResource extends ServerResource 
    implements LocalControllerAPI
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LocalControllerResource.class);
    
    /** Backend reference holder. */
    private LocalControllerBackend backend_;
         
    /**
     * Constructor.
     */
    public LocalControllerResource()
    {
        log_.debug("Starting local controller resource");
        backend_ = (LocalControllerBackend) getApplication().getContext().getAttributes().get("backend");
    }
    
    /** 
     * Starts the virtual machines.
     * (called by the group manager)
     *  
     * @param submissionRequest     The submission request
     * @return                      true if everything ok, "false" otherwise
     */
    @Override
    public VirtualMachineSubmissionResponse startVirtualMachines(VirtualMachineSubmissionRequest submissionRequest) 
    {
        Guard.check(submissionRequest);
        
        if (!isBackendActive())
        {
            log_.warn("Backend is not initialized yet!");
            return null;
        }
        
        ArrayList<VirtualMachineMetaData> virtualMachines = submissionRequest.getVirtualMachineMetaData();
        log_.debug(String.format("Received a request to start %d virtual machines", virtualMachines.size()));
        
        for (VirtualMachineMetaData virtualMachine : submissionRequest.getVirtualMachineMetaData())
        {
            VirtualMachineLocation location = virtualMachine.getVirtualMachineLocation();
            log_.debug(String.format("Starting virtual machine: %s", location.getVirtualMachineId()));
            
            
            
            
            boolean isStarted = backend_.getVirtualMachineActuator().start(virtualMachine.getXmlRepresentation());
            if (!isStarted)
            {
                log_.error("Failed to start virtual machine on hypervisor!");
                ManagementUtils.updateVirtualMachineMetaData(virtualMachine,
                                                             VirtualMachineStatus.ERROR,
                                                             VirtualMachineErrorCode.FAILED_TO_START_ON_HYPERVISOR);
                continue;
            }
                                 
            log_.debug("Virtual machine started! Now starting the monitoring!");
            isStarted = backend_.getVirtualMachineMonitoringService().start(virtualMachine);
            if (!isStarted)
            {
                log_.error("Failed to start virtual machine monitoring!");
                ManagementUtils.updateVirtualMachineMetaData(virtualMachine,
                                                             VirtualMachineStatus.ERROR,
                                                             VirtualMachineErrorCode.FAILED_TO_START_MONITORING);
                continue;
            }
        }
        
        VirtualMachineSubmissionResponse submissionResponse = new VirtualMachineSubmissionResponse();
        submissionResponse.setVirtualMachineMetaData(submissionRequest.getVirtualMachineMetaData());
        return submissionResponse;
    }
    
    /**
     * Routine to migrate a virtual machine.
     * 
     * @param migrationRequest   The migration request
     * @return                   true if everything ok, false otherwise
     */
    @Override
    public boolean migrateVirtualMachine(MigrationRequest migrationRequest)
    {
        Guard.check(migrationRequest);
        String virtualMachineId = migrationRequest.getSourceVirtualMachineLocation().getVirtualMachineId();
        log_.debug(String.format("Received virtual machine %s migration request", virtualMachineId));
        
        if (!isBackendActive())
        {
            log_.warn("Backend is not initialized!");
            return false;
        } 
                
        boolean isStopped = backend_.getVirtualMachineMonitoringService().stop(virtualMachineId);
        if (!isStopped)
        {
            log_.debug("Failed to stop the virtual machine monitoring!");
            return isStopped;
        }
        
        boolean isMigrated = backend_.getVirtualMachineActuator().migrate(migrationRequest);
        if (!isMigrated)
        {
            log_.debug("Failed to migrate virtual machine!");
            backend_.getVirtualMachineMonitoringService().restart(virtualMachineId);
            return isMigrated;
        }
        
        boolean isDropped = backend_.getRepository().dropVirtualMachineMetaData(virtualMachineId);
        if (!isDropped)
        {
            log_.debug("Failed to drop the virtual machine meta data!");
            return isDropped;
        }
        
        return isMigrated;
    } 
    
    /**
     * Routine to suspend a virtual machine.
     * 
     * @param virtualMachineId   The virtual machine identifier
     * @return                   true if everything ok, "false" otherwise
     */
    @Override
    public boolean suspendVirtualMachine(String virtualMachineId)
    {
        log_.debug(String.format("Suspending virtual machine: %s on hypervisor", virtualMachineId));
        
        if (!isBackendActive())
        {
            log_.warn("Backend is not initialized yet!");
            return false;
        }
                   
        boolean isSuspended = backend_.getVirtualMachineMonitoringService().suspend(virtualMachineId);
        if (!isSuspended)
        {
            log_.error("Failed to suspend virtual machine monitoring!");
            return false;    
        }
        
        isSuspended = backend_.getVirtualMachineActuator().suspend(virtualMachineId);
        if (!isSuspended)
        {
            log_.error("Failed to suspend virtual machine!");
            return false;    
        }
        
        boolean isChanged = backend_.getRepository().changeVirtualMachineStatus(virtualMachineId, 
                                                                                VirtualMachineStatus.PAUSED);
        if (!isChanged)
        {
            log_.error("Failed to change virtual machine status!");
            return false;
        }
        
        return true; 
    }
    
    /**
     * Routine to resume a virtual machine.
     * 
     * @param virtualMachineId   The virtual machine identifier
     * @return                   true if everything ok, "false" otherwise
     */
    @Override
    public boolean resumeVirtualMachine(String virtualMachineId)
    {
        log_.debug(String.format("Resuming virtual machine: %s on hypervisor", virtualMachineId));
        
        if (!isBackendActive())
        {
            log_.warn("Backend is not initialized yet!");
            return false;
        }
        
        boolean isResumed = backend_.getVirtualMachineActuator().resume(virtualMachineId);
        if (!isResumed)
        {
            log_.error("Failed to resume virtual machine monitoring!");
            return false;     
        }
        
        isResumed = backend_.getVirtualMachineMonitoringService().resume(virtualMachineId);
        if (!isResumed)
        {
            log_.error("Failed to resume virtual machine!");
            return false;     
        }
        
        boolean isChanged = backend_.getRepository().changeVirtualMachineStatus(virtualMachineId, 
                                                                                VirtualMachineStatus.RUNNING);
        if (!isChanged)
        {
            log_.error("Failed to change virtual machine status!");
            return false;
        }
      
        return true;   
    }   
    
    /**
     * Routine to shutdown a virtual machine.
     * 
     * @param virtualMachineId   The virtual machine identifier
     * @return                   true if everything ok, false otherwise
     */
    @Override
    public boolean shutdownVirtualMachine(String virtualMachineId)
    {
        log_.debug(String.format("Shutdown virtual machine: %s on hypervisor", virtualMachineId));
        
        if (!isBackendActive())
        {
            log_.warn("Backend is not initialized yet!");
            return false;
        }
                                        
        boolean isShutdown = backend_.getVirtualMachineActuator().shutdown(virtualMachineId);
        if (!isShutdown)
        {
            log_.error("Unable to shutdown the virtual machine");
            return false;  
        }
        
        boolean isChanged = backend_.getRepository().changeVirtualMachineStatus(virtualMachineId, 
                                                                                VirtualMachineStatus.SHUTDOWN_PENDING);
        if (!isChanged)
        {
            log_.error("Failed to change virtual machine status!");
            return false;
        }
        
        return true;    
    } 

    /**
     * Routine to reboot a virtual machine.
     * 
     * @param virtualMachineId   The virtual machine identifier
     * @return                   true if everything ok, false otherwise
     */
    @Override
    public boolean rebootVirtualMachine(String virtualMachineId)
    {
        log_.debug(String.format("Reboot virtual machine: %s on hypervisor", virtualMachineId));
        
        if (!isBackendActive())
        {
            log_.warn("Backend is not initialized yet!");
            return false;
        }
                                        
        boolean isRebooted = backend_.getVirtualMachineActuator().reboot(virtualMachineId);
        if (!isRebooted)
        {
            log_.error("Unable to reboot the virtual machine");
            return false;  
        }
        
        boolean isChanged = backend_.getRepository().changeVirtualMachineStatus(virtualMachineId, 
                                                                                VirtualMachineStatus.RUNNING);
        if (!isChanged)
        {
            log_.error("Failed to change virtual machine status!");
            return false;
        }
        
        return true;    
    } 
    
    /**
     * Routine to destroy a virtual machine.
     * 
     * @param virtualMachineId   The virtual machine identifier
     * @return                   true if everything ok, false otherwise
     */
    @Override
    public boolean destroyVirtualMachine(String virtualMachineId)
    {
        log_.debug(String.format("Destroy virtual machine: %s on hypervisor", virtualMachineId));
        
        if (!isBackendActive())
        {
            log_.warn("Backend is not initialized yet!");
            return false;
        }
        
        boolean isStopped = backend_.getVirtualMachineMonitoringService().stop(virtualMachineId);
        if (!isStopped)
        {
            log_.error("Unable to stop virtual machine monitoring");
            return false;     
        }
                            
        boolean isDestroyed = backend_.getVirtualMachineActuator().destroy(virtualMachineId);
        if (!isDestroyed)
        {
            log_.error("Unable to destroy the virtual machine");
            return false; 
        }
        
        boolean isDropped = backend_.getRepository().dropVirtualMachineMetaData(virtualMachineId);
        if (!isDropped)
        {
            log_.error("Failed to drop the virtual machine meta data!");
            return false;
        }
        
        return true;  
    } 
            
    /**
     * Routine to suspend the local controller to ram.
     * 
     * @return  true if everything ok, false otherwise
     */
    @Override
    public boolean suspendNodeToRam()
    {
        log_.debug("Received suspend to ram request");
        
        boolean isSuspended = backend_.powerCycle(PowerSavingAction.suspendToRam);
        return isSuspended;     
    }
    
    /**
     * Routine to suspend the local controller to disk.
     * 
     * @return  true if everything ok, false otherwise
     */
    @Override
    public boolean suspendNodeToDisk()
    {
        log_.debug("Received suspend to disk request");
        
        if (!isBackendActive())
        {
            log_.warn("Backend is not initialized yet!");
            return false;
        }
        
        boolean isSuspended = backend_.powerCycle(PowerSavingAction.suspendToDisk);
        return isSuspended;     
    }    
    
    /**
     * Routine to suspend the local controller to disk.
     * 
     * @return  true if everything ok, false otherwise
     */
    @Override
    public boolean suspendNodeToBoth()
    {
        log_.debug("Received suspend to both request");
        
        if (!isBackendActive())
        {
            log_.warn("Backend is not initialized yet!");
            return false;
        }
        
        boolean isSuspended = backend_.powerCycle(PowerSavingAction.suspendToBoth);
        return isSuspended;     
    }   
    
    /**
     * Routine to shutdown the host.
     * 
     * @return  true if everything ok, false otherwise
     */
    @Override
    public boolean shutdownNode()
    {
        log_.debug("Received request to shutdown the host");
        
        if (!isBackendActive())
        {
            log_.warn("Backend is not initialized yet!");
            return false;
        }
        
        boolean isShutdown = backend_.powerCycle(PowerSavingAction.shutdown);        
        return isShutdown;     
    }
    
    /** 
     * Check backend activity.
     * 
     * @return  true if active, false otherwise
     */
    private boolean isBackendActive()
    {
        if (backend_ == null) 
        {
            return false;
        }
        
        return true;
    }

    /**
     * Starts virtual machine monitoring.
     * 
     * @param virtualMachineMetaData      The virtual machine meta data
     * @return                            true if started, false otherwise
     */
    @Override
    public boolean startVirtualMachineMonitoring(VirtualMachineMetaData virtualMachineMetaData) 
    {
        log_.debug("Start virtual machine monitoring request arrived");
        
        if (!isBackendActive())
        {
            log_.warn("Backend is not initialized yet!");
            return false;
        }
        
        return backend_.getVirtualMachineMonitoringService().start(virtualMachineMetaData);
    }

    
    /**
     * 
     * Resizes a virtual machine.
     * 
     * @param resizeRequest             resize request
     * @return                          the new virtual machine meta data 
     * 
     */
    public VirtualMachineMetaData softResizeVirtualMachine(ResizeRequest resizeRequest) 
    {
        log_.debug("Soft resize virtual machine request arrived");
        
        
        if (!isBackendActive())
        {
           log_.warn("Backend is not initialized yet!");
               return null;
           }
       String virtualMachineId = resizeRequest.getVirtualMachineLocation().getVirtualMachineId();
       VirtualMachineMetaData virtualMachine = backend_.getRepository().getVirtualMachineMetaData(virtualMachineId);
       virtualMachine.setRequestedCapacity(resizeRequest.getResizedCapacity());
       Long memory = resizeRequest.getResizedCapacity().get(1).longValue();
       
       boolean isResized = 
               backend_.getVirtualMachineActuator().setMemory(virtualMachineId, memory);
       
       if (!isResized)
           return null;
       
       int vcpu = new Double(resizeRequest.getResizedCapacity().get(0)).intValue();
       isResized =
               backend_.getVirtualMachineActuator().setVcpu(virtualMachineId, vcpu);

       if (!isResized)
           return null;

       
       return virtualMachine;
    }
    
    
}

