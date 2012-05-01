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
package org.inria.myriads.snoozenode.groupmanager.virtualclustermanager.worker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.rest.CommunicatorFactory;
import org.inria.myriads.snoozecommon.communication.rest.api.GroupManagerAPI;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualClusterErrorCode;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineErrorCode;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualClusterSubmissionResponse;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineSubmission;
import org.inria.myriads.snoozecommon.util.TimeUtils;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.database.api.GroupLeaderRepository;
import org.inria.myriads.snoozenode.exception.DispatchPlanException;
import org.inria.myriads.snoozenode.exception.MissingGroupManagerException;
import org.inria.myriads.snoozenode.groupmanager.energysaver.util.EnergySaverUtils;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.DispatchingPolicy;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.plan.DispatchPlan;
import org.inria.myriads.snoozenode.groupmanager.virtualclustermanager.listener.VirtualClusterSubmissionListener;
import org.inria.myriads.snoozenode.util.ManagementUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Virtual Cluster submission logic.
 * 
 * @author Eugen Feller
 */
public final class VirtualClusterSubmissionWorker   
    implements Runnable
{
    /** Logging instance. */
    private static final Logger log_ = LoggerFactory.getLogger(VirtualClusterSubmissionWorker.class);
    
    /** Virtual machines. */
    private ArrayList<VirtualMachineMetaData> virtualMachines_;
    
    /** Node configuration. */
    private NodeConfiguration nodeConfiguration_;

    /** Virtual cluster dispatch policy. */
    private DispatchingPolicy dispatchingPolicy_;

    /** Group leader repository. */
    private GroupLeaderRepository repository_;
    
    /** Virtual cluster manager. */
    private VirtualClusterSubmissionListener submissionListener_;

    /** Task identifier. */
    private String taskIdentifier_;
    
    /**
     * Constructor.
     * 
     * @param taskIdentifier       The task identifier
     * @param nodeConfiguration    The node configuration
     * @param dispatchingPolicy    The dispatching policy
     * @param repository           The group leader repository
     * @param virtualMachines      The virtual machines
     * @param submissionListener   The submission listener
     */
    public VirtualClusterSubmissionWorker(String taskIdentifier,
                                          ArrayList<VirtualMachineMetaData> virtualMachines,
                                          NodeConfiguration nodeConfiguration, 
                                          DispatchingPolicy dispatchingPolicy, 
                                          GroupLeaderRepository repository,
                                          VirtualClusterSubmissionListener submissionListener)
    {
        log_.debug("Initializing the virtual cluster submission");
        
        taskIdentifier_ = taskIdentifier;
        virtualMachines_ = virtualMachines;
        nodeConfiguration_ = nodeConfiguration;
        repository_ = repository;
        dispatchingPolicy_ = dispatchingPolicy;
        submissionListener_ = submissionListener;
    }
        
    /** Run method. */
    public void run() 
    {
        VirtualClusterSubmissionResponse response = new VirtualClusterSubmissionResponse();
        try 
        {          
            startVirtualClusterDispatching(virtualMachines_);
        }
        catch (DispatchPlanException exception)
        {
            log_.error("Error during dispatching", exception);
            response.setErrorCode(VirtualClusterErrorCode.DISPATCH_PLAN_IS_INVALID);  
        }
        catch (MissingGroupManagerException exception)
        {
            log_.error("Group managers missing", exception);
            response.setErrorCode(VirtualClusterErrorCode.GROUP_MANAGERS_MISSING);  
        }
        catch (Exception exception)
        {
            log_.error("General exception", exception);
            response.setErrorCode(VirtualClusterErrorCode.GENERAL_EXCEPTION); 
        }
        finally
        {      
            log_.debug("Adding virtual cluster response to the response map");            
            response.setVirtualMachineMetaData(virtualMachines_);
            submissionListener_.onVirtualClusterSubmissionFinished(taskIdentifier_, response);
        }
    }
    
    /**
     * Dispatches the virtual cluster submission request.
     * 
     * @param virtualMachines            The virtual machines
     * @throws DispatchPlanException 
     * @throws MissingGroupManagerException 
     */
    private void startVirtualClusterDispatching(List<VirtualMachineMetaData> virtualMachines)
        throws DispatchPlanException, MissingGroupManagerException
    {
        log_.debug("Executing the virtual cluster dispatching request");                      
        
        int numberOfMonitoringEntries = nodeConfiguration_.getEstimator().getNumberOfMonitoringEntries();
        List<GroupManagerDescription> groupManagers = 
            repository_.getGroupManagerDescriptions(numberOfMonitoringEntries);
        if (groupManagers.size() == 0)
        {
            throw new MissingGroupManagerException("No group managers available yet!");
        }
        
        boolean isEnergySavings = nodeConfiguration_.getEnergyManagement().isEnabled();
        if (isEnergySavings)
        {
            EnergySaverUtils.suspendEnergySavers(groupManagers);
        }
                        
        ArrayList<VirtualMachineMetaData> virtualMachinesCopy = 
            new ArrayList<VirtualMachineMetaData>(Arrays.asList(new VirtualMachineMetaData[virtualMachines.size()]));  
        Collections.copy(virtualMachinesCopy, virtualMachines);
        
        DispatchPlan dispatchPlan = dispatchingPolicy_.dispatch(virtualMachinesCopy, groupManagers); 
        if (dispatchPlan == null)
        {
            throw new DispatchPlanException("Dispatch plan is not available!");
        }
        
        startVirtualCluster(dispatchPlan);    
        
        if (isEnergySavings)
        {
            EnergySaverUtils.resumeEnergySavers(groupManagers);
        }
    }
               
    /**
     * Attempts to start the virtual cluster on the assigned group managers.
     * 
     * @param dispatchPlan              The dispatching plan
     * @throws DispatchPlanException 
     */
    private void startVirtualCluster(DispatchPlan dispatchPlan) 
        throws DispatchPlanException 
    {        
        log_.debug("Starting virtual cluster submission");
           
        Map<String, GroupManagerDescription> submissionResponses = new HashMap<String, GroupManagerDescription>(); 
        List<GroupManagerDescription> groupManagers = dispatchPlan.getGroupManagers();
        for (GroupManagerDescription groupManager : groupManagers) 
        {
            ArrayList<VirtualMachineMetaData> assignedVirtualMachines = groupManager.getVirtualMachines();
            if (assignedVirtualMachines.size() == 0)
            {
                log_.debug("No virtual machines assigned to this group manager");
                continue;
            }
            
            String taskIdentifier = startVirtualMachines(assignedVirtualMachines, groupManager);
            if (taskIdentifier == null)
            {   
                log_.debug(String.format("Failed to start virtual machine scheduling on group manager %s", 
                                         groupManager.getId()));
                ManagementUtils.updateAllVirtualMachineMetaData(assignedVirtualMachines,
                                                              VirtualMachineStatus.ERROR,
                                                              VirtualMachineErrorCode.UNABLE_TO_START_ON_GROUP_MANAGER);
                continue;
            }
            
            log_.debug(String.format("Scheduling of %d virtual machines started on group manager: %s with %s : %d",
                                      assignedVirtualMachines.size(),
                                      groupManager.getId(),
                                      groupManager.getListenSettings().getControlDataAddress().getAddress(),
                                      groupManager.getListenSettings().getControlDataAddress().getPort()));
            
            submissionResponses.put(taskIdentifier, groupManager);
        }
        
        if (submissionResponses.size() > 0)
        {
            startCollectionPolling(submissionResponses);
        }
    }
            
    /**
     * Starts the submission monitoring.
     * 
     * @param responses         The submission responses
     */
    private void startCollectionPolling(Map<String, GroupManagerDescription> responses) 
    {
        log_.debug("Starting submission response collection");
        
        int numberOfResponses = 0;
        int numberOfSubmissions = responses.size();  
        int numberOfRetries = nodeConfiguration_.getSubmission().getCollection().getNumberOfRetries();
        int collectionInterval = nodeConfiguration_.getSubmission().getCollection().getRetryInterval();
        
        while (numberOfRetries > 0)
        {
            if (numberOfResponses == numberOfSubmissions)
            {
                log_.debug("Received all virtual machine responses! Terminating polling!");
                break;
            }
         
            try 
            {
                log_.debug(String.format("Waiting %d seconds more before collecting responses", collectionInterval));
                Thread.sleep(TimeUtils.convertSecondsToMilliseconds(collectionInterval));
            } 
            catch (InterruptedException exception) 
            {
                log_.error("Submission monitoring was interrupted!", exception);
                break;
            }
            
            for (Iterator<Map.Entry<String, GroupManagerDescription>> iterator = 
                 responses.entrySet().iterator(); iterator.hasNext();)
            {
                Map.Entry<String, GroupManagerDescription> entry = iterator.next();
                
                String taskIdentifier = entry.getKey();
                GroupManagerDescription groupManager = entry.getValue();
                VirtualMachineSubmission submissionResponse = getVirtualMachineResponse(taskIdentifier, groupManager);
                if (submissionResponse == null)
                {
                    log_.debug(String.format("No submission %s response available yet!", taskIdentifier));
                    ManagementUtils.updateAllVirtualMachineMetaData(virtualMachines_, 
                        VirtualMachineStatus.ERROR, VirtualMachineErrorCode.UNABLE_TO_COLLECT_GROUP_MANAGER_RESPONSE);
                    continue;
                }
                                                
                processVirtualMachineSubmissionResponse(submissionResponse, groupManager);
                iterator.remove();
                numberOfResponses++;
            }
            
            numberOfRetries--;
            log_.debug(String.format("Will try to collect responses %d more times", numberOfRetries));
        }     
    }
    
    /**
     * Starts updating virtual machine meta data.
     * 
     * @param submissionResponse    The submission response
     * @param groupManager          The group managers
     */
    private void processVirtualMachineSubmissionResponse(VirtualMachineSubmission submissionResponse,
                                                         GroupManagerDescription groupManager) 
    {   
        List<VirtualMachineMetaData> responseMetaData = submissionResponse.getVirtualMachineMetaData();
        List<VirtualMachineMetaData> groupManagerMetaData = groupManager.getVirtualMachines();
                        
        log_.debug(String.format("Starting virtual machine submission response processing for %d virtual machines",
                                 responseMetaData.size()));  
        
        for (int i = 0; i < groupManagerMetaData.size(); i++)
        {
            VirtualMachineMetaData originalMetaData = groupManagerMetaData.get(i);
            VirtualMachineMetaData newMetaData = responseMetaData.get(i);
            ManagementUtils.updateVirtualMachineMetaData(originalMetaData,
                                                         newMetaData.getStatus(), 
                                                         newMetaData.getErrorCode());
            originalMetaData.setVirtualMachineLocation(newMetaData.getVirtualMachineLocation());
        }
    }

    /**
     * Try to start the virtual machines on the selected group manager.
     * 
     * @param metaData          The virtual machine meta data
     * @param groupManager      The group manager description
     * @return                  The task identifier
     */
    private String startVirtualMachines(ArrayList<VirtualMachineMetaData> metaData,  
                                        GroupManagerDescription groupManager)
    {
        log_.debug(String.format("Sending virtual machines submission request to group manager: %s", 
                                 groupManager.getId()));
     
        String taskIdentifier = null;
        VirtualMachineSubmission submissionRequest = new VirtualMachineSubmission();
        submissionRequest.setVirtualMachineMetaData(metaData);
        
        NetworkAddress address = groupManager.getListenSettings().getControlDataAddress();
        GroupManagerAPI communicator = CommunicatorFactory.newGroupManagerCommunicator(address);
        
        int numberOfRetries = nodeConfiguration_.getSubmission().getDispatching().getNumberOfRetries();
        int retryInterval = nodeConfiguration_.getSubmission().getDispatching().getRetryInterval();       
        for (int i = 0; i < numberOfRetries; i++)
        {
            taskIdentifier = communicator.startVirtualMachines(submissionRequest);
            if (taskIdentifier == null)
            {   
                log_.debug(String.format("This is the %d virtual machine start attempt to schedule virtual machines " +
                                         "on group manager %s! Is it BUSY?! Waiting for %s seconds to try again!", 
                                         i,
                                         groupManager.getId(),
                                         retryInterval));
                try 
                {
                    Thread.sleep(TimeUtils.convertSecondsToMilliseconds(retryInterval));
                } 
                catch (InterruptedException exception) 
                {
                    log_.error("Interrupted exception", exception);
                    return null;
                }
                
                continue;
            }
            
            break;
        }
        
        return taskIdentifier;
    }
    
    /**
     * Sends a message to get virtual machine response.
     * 
     * @param taskIdentifier     The task identifier
     * @param groupManager       The group manager description
     * @return                   The virtual machine response
     */
    private VirtualMachineSubmission getVirtualMachineResponse(String taskIdentifier,  
                                                               GroupManagerDescription groupManager)
    {      
        log_.debug(String.format("Sending virtual submission: %s response retrieval request to group manager: %s", 
                                 taskIdentifier, 
                                 groupManager.getId()));
        
        NetworkAddress address = groupManager.getListenSettings().getControlDataAddress();
        GroupManagerAPI communicator = CommunicatorFactory.newGroupManagerCommunicator(address);
        VirtualMachineSubmission response = communicator.getVirtualMachineResponse(taskIdentifier);      
        return response;
    }
}

