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
package org.inria.myriads.snoozenode.localcontroller.actuator.api;

import org.inria.myriads.snoozecommon.communication.virtualcluster.migration.MigrationRequest;

/**
 * Interface to control virtual machines.
 * 
 * @author Eugen Feller
 */
public interface VirtualMachineActuator 
{
    /** 
     * Start virtual machine. 
     * 
     * @param xmlDescription    The xml description
     * @return                  true if everything ok, false otherwise
     */
    boolean start(String xmlDescription);
    
    /** 
     * Suspend virtual machine. 
     * 
     * @param id    The virtual machine identifier
     * @return      true if everything ok, false otherwise
     */
    boolean suspend(String id);
    
    /** 
     * Resume virtual machine. 
     * 
     * @param id    The virtual machine identifier
     * @return      true if everything ok, false otherwise
     */
    boolean resume(String id);
        
    /** 
     * Shutdown virtual machine. 
     * 
     * @param id    The virtual machine identifier
     * @return      true if everything ok, false otherwise
     */
    boolean shutdown(String id);
    
    /** 
     * Destroy virtual machine. 
     * 
     * @param id    The virtual machine identifier
     * @return      true if everything ok, false otherwise
     */
    boolean destroy(String id);
    
    /** 
     * Migrate virtual machine. 
     * 
     * @param migrationRequest    The migration request
     * @return                    true if everything ok, false otherwise
     */
    boolean migrate(MigrationRequest migrationRequest);

    /**
     * Performs virtual machine lookup.
     * 
     * @param virtualMachineId      The virtual machine identifier
     * @return                      true if active, false otherwise
     */
    boolean isActive(String virtualMachineId);  
}
