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
package org.inria.myriads.snoozenode.configurator.energymanagement;

import org.inria.myriads.snoozenode.configurator.energymanagement.enums.PowerSavingAction;

/**
 * Energy management settings.
 * 
 * @author Eugen Feller
 */
public final class EnergyManagementSettings
{
    /** Energy savings enable flag. */
    private boolean isEnabled_;

    /** The amount of reserved nodes. */
    private int numberOfReservedNodes_;
    
    /** Power saving action. */
    private PowerSavingAction powerSavingAction_;
    
    /** Drivers settings. */
    private DriverSettings drivers_;
    
    /** Threshold settings. */
    private ThresholdSettings thresholds_;
    
    /** Command execution timeout. */
    private int commandExecutionTimeOut_;
    
    /** Constructor. */
    public EnergyManagementSettings() 
    {   
        drivers_ = new DriverSettings();
        thresholds_ = new ThresholdSettings();
    }
    
    /**
     * Returns the drivers.
     * 
     * @return  The drivers
     */
    public DriverSettings getDrivers()
    {
        return drivers_;
    }
    
    /**
     * Enables/disables the energy savings.
     * 
     * @param isEnabled     true if energy savings must be enabled, false otherwise
     */
    public void setEnabled(boolean isEnabled) 
    {
        isEnabled_ = isEnabled;
    }

    /**
     * Returns energy saving flag.
     * 
     * @return  true if energy savings enabled, false otherwise
     */
    public boolean isEnabled() 
    {
        return isEnabled_;
    }

    /** 
     * Returns the thresholds.
     * 
     * @return  The thresholds
     */
    public ThresholdSettings getThresholds()
    {
        return thresholds_;
    }
    
    /**
     * Sets the power saving action.
     * 
     * @param powerSavingAction     The power saving action
     */
    public void setPowerSavingAction(PowerSavingAction powerSavingAction) 
    {
        powerSavingAction_ = powerSavingAction;
    }

    /**
     * Returns the power saving action.
     * 
     * @return  The power saving action
     */
    public PowerSavingAction getPowerSavingAction() 
    {
        return powerSavingAction_;
    }

    /**
     * Sets the number of reserved nodes.
     * 
     * @param numberOfReservedNodes The number of reserved nodes
     */
    public void setNumberOfReservedNodes(int numberOfReservedNodes) 
    {
        numberOfReservedNodes_ = numberOfReservedNodes;
    }

    /**
     * Returns the number of reserved nodes.
     * 
     * @return  The number of reserved nodes
     */
    public int getNumberOfReservedNodes() 
    {
        return numberOfReservedNodes_;
    }

    /**
     * Sets the command execution timeout.
     * 
     * @param commandExecutionTimeout   The command execution timeout
     */
    public void setCommandExecutionTimeout(int commandExecutionTimeout) 
    {
        commandExecutionTimeOut_ = commandExecutionTimeout;
    }

    /**
     * Returns the command execution timeout.
     * 
     * @return  The command exeuction timeout
     */
    public int getCommandExecutionTimeout() 
    {
        return commandExecutionTimeOut_;
    }
}
