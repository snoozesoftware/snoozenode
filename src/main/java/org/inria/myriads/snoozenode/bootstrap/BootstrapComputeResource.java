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
package org.inria.myriads.snoozenode.bootstrap;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.rest.CommunicatorFactory;
import org.inria.myriads.snoozecommon.communication.rest.api.GroupManagerAPI;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualClusterSubmissionRequest;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.groupmanager.statemachine.VirtualMachineCommand;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bootstrap resource.
 * 
 * @author msimonin
 */
public final class BootstrapComputeResource extends ServerResource 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(BootstrapComputeResource.class);
          
    /** Backend backend reference. */
    private BootstrapBackend backend_;
             
    /**
     * Constructor.
     */
    public BootstrapComputeResource()
    {
        log_.debug("Starting bootstrap resource");
        backend_ = (BootstrapBackend) getApplication().getContext().getAttributes().get("backend");
    }

    /**
     * 
     * Get Virtual Machine Meta Data.
     * 
     * @return  the virtual machine meta data.
     */
    @Get
    public VirtualMachineMetaData getVirtualMachine()
    {
        String virtualMachineId = (String) this.getRequestAttributes().get("id");
        VirtualMachineMetaData virtualMachine = 
                backend_
                .getRepository().
                getVirtualMachineMetaData(
                        virtualMachineId, 
                        0, 
                        backend_.getGroupLeaderDescription());
        return virtualMachine;
    }
    
    /**
     * 
     * Change the status of a virtualmachine.
     * 
     * @return  true iff the command is processing
     */
    @Put
    public boolean commandVirtualMachine()
    {
        String virtualMachineId = (String) this.getRequestAttributes().get("id");
        String action = (String) this.getQuery().getFirstValue("action");
        boolean isProcessing = false;
        try
        {
            isProcessing = backend_.commandVirtualMachine(VirtualMachineCommand.fromString(action), virtualMachineId);
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
        
        return isProcessing;
    }
    
    /**
     * 
     * Start virtual cluster.
     * 
     * @param virtualClusterDescription     The virtual cluster description.
     * 
     * @return  the virtual cluster request identifier.
     */
    @Post
    public String startVirtualCluster(VirtualClusterSubmissionRequest virtualClusterDescription)
    {
        Guard.check(virtualClusterDescription);
        log_.debug("Received virtual cluster start request");
        
        GroupManagerDescription groupLeader = backend_.getGroupLeaderDescription();
        NetworkAddress groupLeaderAddress = groupLeader.getListenSettings().getControlDataAddress();
        
        GroupManagerAPI groupLeaderCommunicator = CommunicatorFactory.newGroupManagerCommunicator(groupLeaderAddress);
        String taskIdentifier = groupLeaderCommunicator.startVirtualCluster(virtualClusterDescription);  
        log_.debug(String.format("Returning task identifier: %s", taskIdentifier));
        
        return taskIdentifier;  
    }
    
}
