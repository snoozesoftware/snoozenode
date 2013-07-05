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
package org.inria.myriads.snoozenode.groupmanager.migration.worker;

import java.util.ArrayList;
import java.util.List;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.rest.CommunicatorFactory;
import org.inria.myriads.snoozecommon.communication.rest.api.LocalControllerAPI;
import org.inria.myriads.snoozecommon.communication.virtualcluster.migration.MigrationRequest;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.groupmanager.migration.listener.MigrationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migration worker.
 * 
 * @author Eugen Feller
 */
public final class MigrationWorker 
    implements Runnable
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(MigrationWorker.class);
    
    /** Virtual machine identifier. */
    private MigrationRequest migrationRequest_;

    /** Migration listener. */
    private List<MigrationListener> migrationListeners_;
     
    /**
     * Constructor.
     * 
     * @param migrationRequest      The migration request
     */
    public MigrationWorker(MigrationRequest migrationRequest)
    {
        log_.debug("Initialzing migration thread");
        Guard.check(migrationRequest);
        migrationRequest_ = migrationRequest;
        migrationListeners_ = new ArrayList<MigrationListener>();
    }
   
    /**
     * Adds a migration listener.
     * 
     * @param migrationListener     The migration listener
     */
    public void addMigrationListener(MigrationListener migrationListener)
    {
        migrationListeners_.add(migrationListener);
    }
    
    /**
     * Migrates a virtual machine.
     * 
     * @return                      true if everything ok, false otherwise
     */
    private boolean migrateVirtualMachine()
    {
        NetworkAddress sourceAddress = 
            migrationRequest_.getSourceVirtualMachineLocation().getLocalControllerControlDataAddress();
        LocalControllerAPI communicator = CommunicatorFactory.newLocalControllerCommunicator(sourceAddress);        
        boolean isMigrated = communicator.migrateVirtualMachine(migrationRequest_);
        return isMigrated;
    }
    
    /**
     * Run method.
     */
    @Override
    public void run() 
    {
        String virtualMachineId = migrationRequest_.getSourceVirtualMachineLocation().getVirtualMachineId();
        NetworkAddress destinationAddress = migrationRequest_.getDestinationVirtualMachineLocation().
                                                                        getLocalControllerControlDataAddress();
        log_.debug(String.format("Starting virtual machine: %s migration to local controller at %s with " +
                                 "control data port: %d and hypervisor port: %d",
                                 virtualMachineId, 
                                 destinationAddress.getAddress(),
                                 destinationAddress.getPort(),
                                 migrationRequest_.getDestinationHypervisorSettings().getPort()));            
        boolean isMigrated = migrateVirtualMachine();       
        migrationRequest_.setMigrated(isMigrated);
        for (MigrationListener migrationListener : migrationListeners_)
        {
            migrationListener.onMigrationEnded(migrationRequest_);
        }
    }
}
