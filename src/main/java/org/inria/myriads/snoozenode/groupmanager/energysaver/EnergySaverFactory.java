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
package org.inria.myriads.snoozenode.groupmanager.energysaver;

import org.inria.myriads.snoozenode.configurator.energymanagement.EnergyManagementSettings;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.groupmanager.energysaver.saver.EnergySaver;
import org.inria.myriads.snoozenode.groupmanager.energysaver.wakeup.WakeupResources;
import org.inria.myriads.snoozenode.groupmanager.statemachine.api.StateMachine;

/**
 * Energy saver factory.
 * 
 * @author Eugen Feller
 */
public final class EnergySaverFactory 
{
    /** Hide constructor. */
    private EnergySaverFactory()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Crates a new energy saver.
     * 
     * @param energySettings    The energy settings
     * @param repository        The group manager repository
     * @param stateMachine      The state machine
     * @return                  The energy saver object
     */
    public static EnergySaver newEnergySaver(EnergyManagementSettings energySettings, 
                                             GroupManagerRepository repository,
                                             StateMachine stateMachine)
    {
        return new EnergySaver(energySettings, repository, stateMachine);
    }
    
    /**
     * Creates a new resource wakeuper.
     * 
     * @param wakeupTimeout             The wakeup timeout
     * @param commandExecutionTimeout   The command execution timeout
     * @param repository                The grpip manager repository
     * @return                          The wakeup resource
     */
    public static WakeupResources newWakeupResource(int wakeupTimeout,
                                                    int commandExecutionTimeout,
                                                    GroupManagerRepository repository)
    {
        return new WakeupResources(wakeupTimeout, commandExecutionTimeout, repository);
    }
}
