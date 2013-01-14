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
package org.inria.myriads.snoozenode.groupmanager.migration.watchdog;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.rest.CommunicatorFactory;
import org.inria.myriads.snoozecommon.communication.rest.api.LocalControllerAPI;
import org.inria.myriads.snoozecommon.communication.virtualcluster.migration.MigrationRequest;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.util.TimeUtils;
import org.inria.myriads.snoozenode.groupmanager.migration.listener.MigrationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migration watchdog.
 * 
 * @author Eugen Feller
 */
public final class MigrationWatchdog
    implements Runnable, MigrationListener
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(MigrationWatchdog.class);
    
    /** Migration listener. */
    private MigrationListener migrationListener_;

    /**Migration request. */
    private MigrationRequest migrationRequest_;
    
    /** The lock. */
    private Object lockObject_;

    /** Indicates termination. */
    private boolean isTerminated_;
    
    /**
     * Constructor.
     * 
     * @param migrationRequest    The migration request
     * @param migrationListener   The migration listener
     */
    public MigrationWatchdog(MigrationRequest migrationRequest, MigrationListener migrationListener)
    {
        Guard.check(migrationRequest, migrationListener);
        log_.debug("Initializing the migration watchdog thread!");
        
        migrationRequest_ = migrationRequest;
        migrationListener_ = migrationListener;
        lockObject_ = new Object();
    }
    
    /**
     * Suspends a virtual machine.
     * 
     * @param virtualMachineId          The virtual machine identifier
     * @param localControllerAddress    The local controller address
     * @return                          true if everything ok, false otherwise
     */
    private boolean suspendVirtualMachine(String virtualMachineId, NetworkAddress localControllerAddress)
    {   
        Guard.check(virtualMachineId, localControllerAddress);
        log_.debug(String.format("Sending request to suspend virtual machine: %s on local controller: %s",
                                 virtualMachineId, localControllerAddress.getAddress()));
        
        LocalControllerAPI communicator = CommunicatorFactory.newLocalControllerCommunicator(localControllerAddress);
        return communicator.suspendVirtualMachineOnMigration(virtualMachineId); 
    }
    
    /**
     * Starts watching the migration.
     */
    @Override
    public void run() 
    {
        String virtualMachineId = migrationRequest_.getSourceVirtualMachineLocation().getVirtualMachineId();
        int convergenceTimeout = migrationRequest_.getDestinationHypervisorSettings().getMigration().getTimeout();
        NetworkAddress sourceLocalController = 
            migrationRequest_.getSourceVirtualMachineLocation().getLocalControllerControlDataAddress();
        try 
        {
            log_.debug(String.format("Starting to watch live migration of: %s", virtualMachineId));
            synchronized (lockObject_)
            {
                lockObject_.wait(TimeUtils.convertSecondsToMilliseconds(convergenceTimeout));
            }
        } 
        catch (InterruptedException exception) 
        {
            log_.debug(String.format("Migration watchdog thread was interrupted: %s!", exception.getMessage()));
        } 
        finally
        {
            if (isTerminated_)
            {
                log_.debug(String.format("Virtual machine %s watchdog terminated gracefully!", virtualMachineId));
                return;
            } 
            
            log_.debug(String.format("Virtual machine %s is still active! Suspending to force convergence!",
                                     virtualMachineId));
            boolean isSuspended = suspendVirtualMachine(virtualMachineId, sourceLocalController);
            if (isSuspended)
            {
                log_.debug(String.format("Virtual machine %s suspend finished successfully!!", virtualMachineId));
                return;
            }
            
            log_.debug(String.format("Suspending virtual machine %s migration FAILED!", virtualMachineId));
            migrationRequest_.setMigrated(false);
            migrationListener_.onMigrationEnded(migrationRequest_);
        }
    }
    
    /**
     * On migration ended callback.
     * 
     * @param migrationRequest      The migration request
     */
    @Override
    public void onMigrationEnded(MigrationRequest migrationRequest) 
    {
        synchronized (lockObject_)
        {
            isTerminated_ = true;
            lockObject_.notify();
        }
    }
}
