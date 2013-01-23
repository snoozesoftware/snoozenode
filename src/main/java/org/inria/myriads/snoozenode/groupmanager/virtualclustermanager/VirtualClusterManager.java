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
package org.inria.myriads.snoozenode.groupmanager.virtualclustermanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualClusterSubmissionRequest;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualClusterSubmissionResponse;
import org.inria.myriads.snoozecommon.exception.VirtualClusterParserException;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.parser.VirtualClusterParserFactory;
import org.inria.myriads.snoozecommon.parser.api.VirtualClusterParser;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.configurator.scheduler.GroupLeaderSchedulerSettings;
import org.inria.myriads.snoozenode.database.api.GroupLeaderRepository;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.GroupLeaderPolicyFactory;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.DispatchingPolicy;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.enums.Dispatching;
import org.inria.myriads.snoozenode.groupmanager.virtualclustermanager.listener.VirtualClusterSubmissionListener;
import org.inria.myriads.snoozenode.groupmanager.virtualclustermanager.worker.VirtualClusterSubmissionWorker;
import org.inria.myriads.snoozenode.groupmanager.virtualnetworkmanager.VirtualNetworkFactory;
import org.inria.myriads.snoozenode.groupmanager.virtualnetworkmanager.api.VirtualNetworkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Virtual Cluster Manager.
 * 
 * @author Eugen Feller
 */
public final class VirtualClusterManager 
    implements VirtualClusterSubmissionListener
{
    /** Logging instance. */
    private Logger log_ = LoggerFactory.getLogger(VirtualClusterManager.class);
            
    /** Map which holds the virtual cluster responses. */
    private Map<String, VirtualClusterSubmissionResponse> virtualClusterResponses_;

    /** Queue for tasks. */
    private Queue<VirtualClusterSubmissionWorker> workerQueue_;

    /** Virtual cluster dispatching. */
    private DispatchingPolicy virtualClusterDispatching_;

    /** Node configuration. */
    private NodeConfiguration nodeConfiguration_;

    /** Resource demand estimator. */
    private ResourceDemandEstimator estimator_;

    /** Group leader repository. */
    private GroupLeaderRepository repository_;

    /** Virtual network manager. */
    private VirtualNetworkManager virtualNetworkManager_;
    
    /**
     * Constructor.
     *
     * @param nodeConfiguration   The node configuration
     * @param repository          The group leader repository
     * @param estimator           The resource demand estimator
     */
    public VirtualClusterManager(NodeConfiguration nodeConfiguration,
                                 GroupLeaderRepository repository,
                                 ResourceDemandEstimator estimator)
    {
        log_.debug("Initializing the virtual cluster manager");
        
        nodeConfiguration_ = nodeConfiguration;
        repository_ = repository;
        estimator_ = estimator;
        virtualClusterResponses_ = new HashMap<String, VirtualClusterSubmissionResponse>();
        workerQueue_ = new LinkedList<VirtualClusterSubmissionWorker>();
        virtualNetworkManager_ = VirtualNetworkFactory.newVirtualNetworkManager(repository);
        initializeDispatchingPolicy();
    }

    /**
     * Initializes the dispatching policy.
     */
    private void initializeDispatchingPolicy()
    {
        GroupLeaderSchedulerSettings schedulerSettings = nodeConfiguration_.getGroupLeaderScheduler();
        Dispatching virtualClusterDispatch = schedulerSettings.getDispatchingPolicy();
        virtualClusterDispatching_ = GroupLeaderPolicyFactory.newVirtualClusterPlacement(virtualClusterDispatch,
                                                                                         estimator_);
    }
    
    /**
     * Generates the virtual machine meta data.
     * 
     * @param submissionRequest               The submission request
     * @return                                The virtual machine meta data
     * @throws VirtualClusterParserException  The virtual cluster parser exception
     */
    protected ArrayList<VirtualMachineMetaData> 
        generateVirtualMachineMetaData(VirtualClusterSubmissionRequest submissionRequest) 
        throws VirtualClusterParserException
    {
        Guard.check(submissionRequest);
        log_.debug("Generating virtual machine meta data");
        
        VirtualClusterParser parser = VirtualClusterParserFactory.newVirtualClusterParser();
        ArrayList<VirtualMachineMetaData> virtualMachines = 
            parser.createVirtualMachineMetaData(submissionRequest);      
        return virtualMachines;
    }
            
    /**
     * Dispatches the virtual cluster submission request.
     * 
     * @param submissionRequest       The submission request
     * @return                        The task identifier
     */
    public synchronized String startVirtualClusterSubmission(VirtualClusterSubmissionRequest submissionRequest) 
    {
        Guard.check(submissionRequest);
        log_.debug("Executing the virtual cluster start request");
                
        String taskIdentifier = null;
        ArrayList<VirtualMachineMetaData> virtualMachines = null;
        try
        {
            virtualMachines = generateVirtualMachineMetaData(submissionRequest);  
            boolean isAssigned = virtualNetworkManager_.assignIpAddresses(virtualMachines);
            if (!isAssigned)
            {
                log_.error("Failed to assign IP addresses!");
               return null;
            }
        }
        catch (Exception exception) 
        {
            log_.error("Error generating virtual machine meta data", exception); 
        } 
        finally
        {
            if (virtualMachines == null)
            {
                return null;
            }
            
            taskIdentifier = startSubmissionWorker(virtualMachines);
        }
               
        return taskIdentifier;
    }
    
    /**
     * Starts the submission worker.
     * 
     * @param virtualMachines   The virtual machines
     * @return                  The task identifier
     */ 
    private String startSubmissionWorker(ArrayList<VirtualMachineMetaData> virtualMachines)
    {
        String taskIdentifier = UUID.randomUUID().toString();
        VirtualClusterSubmissionWorker submission = new VirtualClusterSubmissionWorker(taskIdentifier,
                                                                                       virtualMachines,
                                                                                       nodeConfiguration_,
                                                                                       virtualClusterDispatching_,
                                                                                       repository_,
                                                                                       this);   
        if (workerQueue_.size() == 0)
        {
            log_.debug(String.format("Starting virtual cluster submission thread for task: %s!", 
                                     taskIdentifier));
            new Thread(submission).start();
        }
        
        workerQueue_.add(submission);
        return taskIdentifier;
    }
    
    /**
     * Adds a virtual cluster response.
     *
     * @param taskIdentifier    The task identifier
     * @param response          The virtual cluster response
     */
    @Override
    public synchronized void onVirtualClusterSubmissionFinished(String taskIdentifier, 
                                                                VirtualClusterSubmissionResponse response) 
    {
        Guard.check(taskIdentifier, response);
        log_.debug(String.format("Adding virtual cluster response for: %s", taskIdentifier));
        
        postVirtualClusterSubmission(response.getVirtualMachineMetaData());
        virtualClusterResponses_.put(taskIdentifier, response);
        
        workerQueue_.poll();
        VirtualClusterSubmissionWorker submissionWorker = workerQueue_.peek();
        if (submissionWorker != null)
        {
            new Thread(submissionWorker).start();
        }
    }
     
    /**
     * Called post virtual cluster submission.
     * 
     * @param virtualMachines   The virtual machine meta data
     */
    private void postVirtualClusterSubmission(List<VirtualMachineMetaData> virtualMachines)
    {
        for (VirtualMachineMetaData metaData : virtualMachines)
        {
            boolean isRunning = metaData.getStatus().equals(VirtualMachineStatus.RUNNING);
            if (!isRunning)
            {
                log_.debug("Releasing IP address!");
                virtualNetworkManager_.releaseIpAddress(metaData);
            }
        }
    }
    
    /**
     * Returns virtual cluster response if available.
     * 
     * @param taskIdentifier    The task identifier
     * @return                  The virtual cluster response
     */
    public VirtualClusterSubmissionResponse getVirtualClusterResponse(String taskIdentifier)
    {
        Guard.check(taskIdentifier);        
        
        VirtualClusterSubmissionResponse virtualClusterResponse = virtualClusterResponses_.get(taskIdentifier);
        if (virtualClusterResponse != null)
        {
            virtualClusterResponses_.remove(taskIdentifier);
        }
        
        return virtualClusterResponse;
    }
}
