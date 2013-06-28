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
package org.inria.myriads.snoozenode.groupmanager.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.HypervisorSettings;
import org.inria.myriads.snoozecommon.communication.rest.CommunicatorFactory;
import org.inria.myriads.snoozecommon.communication.rest.api.GroupManagerAPI;
import org.inria.myriads.snoozecommon.communication.rest.api.LocalControllerAPI;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.migration.MigrationRequest;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.exception.MigrationPlanEnforcerException;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.ReconfigurationPlan;
import org.inria.myriads.snoozenode.groupmanager.migration.listener.MigrationListener;
import org.inria.myriads.snoozenode.groupmanager.migration.listener.MigrationPlanListener;
import org.inria.myriads.snoozenode.groupmanager.migration.watchdog.MigrationWatchdog;
import org.inria.myriads.snoozenode.groupmanager.migration.worker.MigrationWorker;
import org.inria.myriads.snoozenode.message.ManagementMessage;
import org.inria.myriads.snoozenode.message.ManagementMessageType;
import org.inria.myriads.snoozenode.monitoring.datasender.api.DataSender;
import org.inria.snoozenode.external.notifier.ExternalNotificationType;
import org.inria.snoozenode.external.notifier.ExternalNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migration plan enforcer.
 * 
 * @author Eugen Feller
 */
public final class MigrationPlanEnforcer 
    implements MigrationListener
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(MigrationPlanEnforcer.class);
    
    /** Number of monitoring entries. */
    private static final int NUMBER_OF_MONITORING_ENTRIES = 1;
    
    /** Group manager repository. */
    private GroupManagerRepository groupManagerRepository_;
    
    /** Migration plan listener. */
    private MigrationPlanListener listener_;
    
    /** Finished migrations. */
    private List<MigrationRequest> finishedMigrations_;

    /** Number of migrations. */
    private int numberOfMigrations_;
    
    
    /** External Sender. */
    private ExternalNotifier externalNotifier_;
    
    /**
     * Constructor.
     * 
     * @param groupManagerRepository     The group manager repository
     * @param listener                   Migration plan listener
     */
    public MigrationPlanEnforcer(
                                GroupManagerRepository groupManagerRepository, 
                                MigrationPlanListener listener,
                                ExternalNotifier externalNotifier
                                 )
    {
        Guard.check(groupManagerRepository);
        log_.debug("Initializing the migration plan enforcer");
        externalNotifier_ = externalNotifier;
        groupManagerRepository_ = groupManagerRepository;
        listener_ = listener;
        finishedMigrations_ = new ArrayList<MigrationRequest>();
    }
           
    /**
     * Proceses the migration request.
     * 
     * @param migrationRequest                  The migration request
     * @return                                  true if everything ok, false otherwise
     * @throws MigrationPlanEnforcerException 
     */
    private boolean processFinishedMigration(MigrationRequest migrationRequest) 
        throws MigrationPlanEnforcerException 
    {        
        String virtualMachineId = migrationRequest.getSourceVirtualMachineLocation().getVirtualMachineId();
        log_.debug(String.format("Processing finished virtual machine %s migration", virtualMachineId));
        
        if (!migrationRequest.isMigrated())
        {
            log_.debug(String.format("Virtual machine: %s was not migrated! Not good!", virtualMachineId));
            return false;
        }
        
        NetworkAddress destinationAddress =
            migrationRequest.getDestinationVirtualMachineLocation().getLocalControllerControlDataAddress();
        
        if (destinationAddress == null)
        {
            log_.error("Local controller destination address invalid!");
            throw new MigrationPlanEnforcerException("Local controller destination address invalid!");
        }
        
        String destinationId =
            migrationRequest.getDestinationVirtualMachineLocation().getLocalControllerId();
        if (destinationId == null)
        {
            log_.error("Local controller identifier is invalid!");
            throw new MigrationPlanEnforcerException("Local controller identifier is invalid!");
        }
        
        VirtualMachineLocation oldLocation = migrationRequest.getSourceVirtualMachineLocation();
        
        
        VirtualMachineLocation newLocation = migrationRequest.getDestinationVirtualMachineLocation();
        newLocation.setVirtualMachineId(oldLocation.getVirtualMachineId());
        
        boolean isUpdated = false;
        VirtualMachineMetaData metaData = null;
        log_.debug("Migration terminated : updating the group manager repository");
        
        if (oldLocation.getGroupManagerId().equals(newLocation.getGroupManagerId()))
        {
            log_.debug("Migration terminated : updating the LOCAL group manager repository");
            isUpdated = groupManagerRepository_.updateVirtualMachineLocation(oldLocation, newLocation);
            metaData = groupManagerRepository_.getVirtualMachineMetaData(newLocation, NUMBER_OF_MONITORING_ENTRIES);
        }
        else
        {
            log_.debug("Migration terminated : updating the REMOTE group manager repository");
            metaData = groupManagerRepository_.getVirtualMachineMetaData(oldLocation, NUMBER_OF_MONITORING_ENTRIES);
            NetworkAddress newGroupManager = newLocation.getGroupManagerControlDataAddress();
            log_.debug(String.format("Migration terminated : sending a request to update the group manager %s", 
                    newGroupManager.getAddress()
                    ));
            
            boolean isRemoved = groupManagerRepository_.dropVirtualMachineData(oldLocation);
            if (!isRemoved)
            {
                throw new MigrationPlanEnforcerException("Failed to update virtual machine location!");
            }

            
            metaData.setVirtualMachineLocation(newLocation);
            GroupManagerAPI communicator = CommunicatorFactory.newGroupManagerCommunicator(newGroupManager);
            isUpdated = communicator.addVirtualMachineAfterMigration(metaData);
            
        }
        if (!isUpdated)
        {
            throw new MigrationPlanEnforcerException("Failed to update virtual machine location!");
        }
        
        if (metaData == null)
        {
            throw new MigrationPlanEnforcerException("Virtual machine meta data is invalid!");
        }
        
        boolean isStarted = startVirtualMachineMonitoring(destinationAddress, metaData);       
        
        if (!isStarted)
        {            
            throw new MigrationPlanEnforcerException("Unable to start virtual machine monitoring on destination!");
        }
        

        
        return true;
    }
    
   
    /**
     * Starts virtual machine monitoring.
     * 
     * @param localControllerAddress   The local controller address
     * @param metaData                 The virtual machine meta data
     * @return                         true if everything ok, false otherwise
     */
    private boolean startVirtualMachineMonitoring(NetworkAddress localControllerAddress,
                                                  VirtualMachineMetaData metaData)
    {
        log_.debug(String.format("Sending virtual machine monitoring start request to local controller %s: %d",
                                 localControllerAddress.getAddress(), localControllerAddress.getPort()));
        
        LocalControllerAPI communicator = CommunicatorFactory.newLocalControllerCommunicator(localControllerAddress);
        return communicator.startVirtualMachineMonitoring(metaData);        
    }
    
    /**
     * Migration callback.
     * 
     * @param migrationRequest      The migration request
     */
    @Override
    public synchronized void onMigrationEnded(MigrationRequest migrationRequest) 
    {                     
        log_.debug(String.format("Adding virtual machine %s to finished migrations list", 
                                 migrationRequest.getSourceVirtualMachineLocation().getVirtualMachineId()));
        
        finishedMigrations_.add(migrationRequest);
        if (numberOfMigrations_ == finishedMigrations_.size())
        {
            log_.debug("All migrations finished! Starting the processing phase!");
            for (MigrationRequest finishedMigration : finishedMigrations_)
            {
                try 
                {               
                    processFinishedMigration(finishedMigration);

                } 
                catch (MigrationPlanEnforcerException exception) 
                {
                    log_.error("Exception during migration processing", exception);
                    
                    externalNotifier_.send(
                            ExternalNotificationType.MANAGEMENT,
                            new ManagementMessage(ManagementMessageType.ERROR , finishedMigration),
                            groupManagerRepository_.getGroupManagerId() + "." +
                            finishedMigration.getSourceVirtualMachineLocation().getLocalControllerId() + "." + 
                            finishedMigration.getSourceVirtualMachineLocation().getVirtualMachineId() + "." +
                            "MIGRATION"
                            );
                }
                externalNotifier_.send(
                        ExternalNotificationType.MANAGEMENT,
                        new ManagementMessage(ManagementMessageType.PROCESSED , finishedMigration),
                        groupManagerRepository_.getGroupManagerId() + "." +
                        finishedMigration.getSourceVirtualMachineLocation().getLocalControllerId() + "." + 
                        finishedMigration.getSourceVirtualMachineLocation().getVirtualMachineId() + "." +
                        "MIGRATION"
                        );
                
            }
            
            log_.debug("Migration plan enforced!");
            
            
            listener_.onMigrationPlanEnforced();
            finishedMigrations_.clear();
        }
    }
    
    /**
     * Creates a migration request.
     * 
     * @param sourceVirtualMachineLocation          The source virtual machine location
     * @param destinationVirtualMachineLocation     The destination virtual machine location
     * @param remoteHypervisorSettings              The remote hypervisor settings
     * @return                                      The migration request
     */
    private MigrationRequest createMigrationRequest(VirtualMachineLocation sourceVirtualMachineLocation,
                                                    VirtualMachineLocation destinationVirtualMachineLocation,
                                                    HypervisorSettings remoteHypervisorSettings)
    {
        log_.debug("Creating migration request");        
        MigrationRequest migrationRequest = new MigrationRequest();
        migrationRequest.setDestinationVirtualMachineLocation(destinationVirtualMachineLocation);
        migrationRequest.setSourceVirtualMachineLocation(sourceVirtualMachineLocation);
        migrationRequest.setDestinationHypervisorSettings(remoteHypervisorSettings);   
        return migrationRequest;
    }
        
    /**
     * Creates new virtual machine location from local controller description.
     * 
     * @param virtualMachineId    The virtual machine identifier
     * @param localControllerId   The local controller identifier
     * @param controlDataAddress  The control data address
     * @return                    The virtual machine location
     */
    private VirtualMachineLocation createNewVirtualMachineLocation(VirtualMachineLocation sourceLocation,
                                                                   String localControllerId,
                                                                   NetworkAddress controlDataAddress)
    {
        VirtualMachineLocation location = new VirtualMachineLocation();
        location.setVirtualMachineId(sourceLocation.getVirtualMachineId());
        location.setLocalControllerId(localControllerId);
        location.setLocalControllerControlDataAddress(controlDataAddress);
        location.setGroupManagerControlDataAddress(sourceLocation.getGroupManagerControlDataAddress());
        location.setGroupManagerId(location.getGroupManagerId());
        return location;
    }

    /**
     * Migrates a virtual machine to the specified group manager.
     *  
     * @param migrationRequest      The migration request
     */
    public void startMigration(MigrationRequest migrationRequest) 
    {    
        log_.debug(String.format("Starting to migrate virtual machine %s from local controller %s:%d to %s:%d",
            migrationRequest.getSourceVirtualMachineLocation().getVirtualMachineId(),
            migrationRequest.getSourceVirtualMachineLocation().getLocalControllerControlDataAddress().getAddress(), 
            migrationRequest.getSourceVirtualMachineLocation().getLocalControllerControlDataAddress().getPort(), 
            migrationRequest.getDestinationVirtualMachineLocation().getLocalControllerControlDataAddress().getAddress(),
            migrationRequest.getDestinationVirtualMachineLocation().getLocalControllerControlDataAddress().getPort()));
                       
        MigrationWorker migrationThread = new MigrationWorker(migrationRequest);
        MigrationWatchdog watchdogThread = new MigrationWatchdog(migrationRequest, this);      
        migrationThread.addMigrationListener(watchdogThread);
        migrationThread.addMigrationListener(this);
        new Thread(migrationThread).start();
        new Thread(watchdogThread).start();
    }
        
    /**
     * Enforces the migration plan.
     * 
     * @param migrationPlan                         The migration plan
     * @throws MigrationPlanEnforcerException 
     */
    public void enforceMigrationPlan(ReconfigurationPlan migrationPlan) 
        throws MigrationPlanEnforcerException 
    {
        if (migrationPlan == null)
        {
            throw new MigrationPlanEnforcerException("Migration plan is not available!");        
        }
        
        if (migrationPlan.getNumberOfReleasedNodes() == 0)
        {   
            throw new MigrationPlanEnforcerException("Migration plan does not yield to less hosts!"); 
        }
        
        numberOfMigrations_ = migrationPlan.getNumberOfMigrations();
        if (numberOfMigrations_ == 0)
        {   
            throw new MigrationPlanEnforcerException("The number of migrations is 0!"); 
        }
        
        log_.debug(String.format("Starting to enforce the migration plan. Number of used " +
                                 "and released nodes is:  %d / %d", 
                                 migrationPlan.getNumberOfUsedNodes(), 
                                 migrationPlan.getNumberOfReleasedNodes()));
        
        log_.debug(String.format("Number of migrations: %s", numberOfMigrations_));
        
        
        externalNotifier_.send(ExternalNotificationType.SYSTEM,
                migrationPlan, 
                groupManagerRepository_.getGroupManagerId() + "." + 
                "reconfiguration"
                );
        
        Map<VirtualMachineMetaData, LocalControllerDescription> mapping = migrationPlan.getMapping();
        for (Map.Entry<VirtualMachineMetaData, LocalControllerDescription> entry : mapping.entrySet())
        {
            VirtualMachineMetaData virtualMachine = entry.getKey();            
            LocalControllerDescription localController = entry.getValue();
            
            VirtualMachineLocation sourceLocation = virtualMachine.getVirtualMachineLocation();            
            VirtualMachineLocation destinationLocation = 
                createNewVirtualMachineLocation(sourceLocation,
                                                localController.getId(), 
                                                localController.getControlDataAddress());
            
            MigrationRequest migrationRequest = createMigrationRequest(sourceLocation,
                                                                       destinationLocation,
                                                                       localController.getHypervisorSettings()); 
            startMigration(migrationRequest);
        }
    }
    
    /**
     * 
     * Start a migration.
     * Call by the client.
     * 
     * @param migrationRequest
     */
    public void startManualMigration(MigrationRequest migrationRequest)
    {
        numberOfMigrations_ = 1;
        startMigration(migrationRequest);
    }  
}
