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
package org.inria.myriads.snoozenode.localcontroller.monitoring.transport;

import java.io.Serializable;
import java.util.List;

import org.inria.myriads.snoozenode.localcontroller.monitoring.enums.LocalControllerState;

/**
 * Aggregated virtual machine monitoring data.
 * 
 * @author Eugen Feller
 */
public final class LocalControllerDataTransporter 
    implements Serializable 
{
    /** Default serial. */
    private static final long serialVersionUID = 1L;

    /** Local controller state. */
    private LocalControllerState state_ = LocalControllerState.STABLE;
    
    /** Local controller identifier. */
    private String localControllerId_;
        
    /** Virtual machine monitoring data. */
    private List<AggregatedVirtualMachineData> aggregatedData_;
    
    /**
     * Constructor.
     * 
     * @param localControllerId     The local controller identifier
     * @param aggregatedData        The aggregated data
     */
    public LocalControllerDataTransporter(String localControllerId,
                                          List<AggregatedVirtualMachineData> aggregatedData)
    {
        localControllerId_ = localControllerId;
        aggregatedData_ = aggregatedData;
    }
    
    /**
     * Returns the aggregated data map.
     * 
     * @return      The aggregated monitoring data
     */
    public List<AggregatedVirtualMachineData> getData()
    {
        return aggregatedData_;
    }
    
    /**
     * Returns the local controller identifier.
     * 
     * @return  The local controller id
     */
    public String getLocalControllerId()
    {
        return localControllerId_;
    }
    
    /**
     * Indicates the local controller state.
     * 
     * @return  The local controller state
     */
    public LocalControllerState getState()
    {
        return state_;
    }
        
    /**
     * Sets the local controller state.
     * 
     * @param state     The local controller state
     */
    public void setState(LocalControllerState state)
    {
        state_ = state;
    }
}
