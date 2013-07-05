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
package org.inria.myriads.snoozenode.groupmanager.statemachine.api;

import java.util.List;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineSubmissionRequest;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineSubmissionResponse;
import org.inria.myriads.snoozecommon.communication.virtualmachine.ClientMigrationRequest;
import org.inria.myriads.snoozenode.groupmanager.statemachine.VirtualMachineCommand;
import org.inria.myriads.snoozenode.localcontroller.monitoring.enums.LocalControllerState;

/** 
 * State machine interface.
 * 
 * @author Eugen Feller
 *
 */
public interface StateMachine 
{
    /**
     * Resolves anomaly.
     * 
     * @param localControllerId      The aggregated local controller identifier
     * @param state                  The local controller state
     */
    void resolveAnomaly(String localControllerId, LocalControllerState state);
    
    /**
     * Starts the reconfiguration process.
     * 
     * @return     true if everything ok, false otherwise
     */
    boolean startReconfiguration();
    
    /**
     * Starts the virtual machine.
     * 
     * @param submissionRequest     The virtual machine submission
     * @return                      The task identifier
     */
    String startVirtualMachines(VirtualMachineSubmissionRequest submissionRequest);

    /**
     * Called upon virtual machine submission finished.
     */
    void onVirtualMachineSubmissionFinished();
    
    /**
     * Processes virtual machine command.
     * 
     * @param command      The virtual machine command
     * @param location     The virtual machine location
     * @return             true if everything ok, false otherwise
     */
    boolean controlVirtualMachine(VirtualMachineCommand command, VirtualMachineLocation location);
    
    
    /**
     * Starts the migration of the vm.
     * @param clientMigrationRequest clientMigrationRequest
     * 
     * 
     * @return     true if everything ok, false otherwise
     */
    boolean startMigration(ClientMigrationRequest clientMigrationRequest);
    
    /**
     * Indicates if state machine is busy or not.
     * 
     * @return      true if busy, else otherwise
     */
    boolean isBusy();
    
    /**
     * Returns virtual machine submission finish.
     * 
     * @param taskIdentifier    The task identifier
     * @return                  The submission response
     */
    VirtualMachineSubmissionResponse getVirtualMachineSubmissionResponse(String taskIdentifier);
    
    /**
     * Wakeup local controller.
     * 
     * @param localController   The local controller
     * @return                  true if everything ok, false otherwise
     */
    boolean onWakeupLocalController(LocalControllerDescription localController);
    
    /**
     * Wakesup local controllers.
     * 
     * @param localControllers  The local controllers
     * @return                  true if everything ok, false otherwise                 
     */
    boolean onWakeupLocalControllers(List<LocalControllerDescription> localControllers);
    
    /**
     * Called on energy savings enabled.
     * 
     * @param idleResources     The idle resources
     * @return                  true if everything ok, false otherwise
     */
    boolean onEnergySavingsEnabled(List<LocalControllerDescription> idleResources);

    /**
     * Called on anomaly resolved.
     * 
     * @param anomalyLocalController    The anomalied local controller
     */
    void onAnomalyResolved(LocalControllerDescription anomalyLocalController);


}
