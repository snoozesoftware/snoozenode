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
package org.inria.myriads.snoozenode.localcontroller.monitoring.transport;

import java.io.Serializable;
import java.util.List;

import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.VirtualMachineMonitoringData;

/**
 * Aggregated virtual machine data.
 * 
 * @author Eugen Feller
 */
public final class AggregatedVirtualMachineData 
    implements Serializable
{
    /** Aggregated virtual machine data. */
    private static final long serialVersionUID = 1L;

    /** Virtual machine identifier. */
    private String virtualMachineId_;
    
    /** Virtual machine monitoring data. */
    private List<VirtualMachineMonitoringData> monitoringData_;
    
    /** Default constructor. */
    public AggregatedVirtualMachineData()
    {
    }
    
    /**
     * Constructor.
     * 
     * @param virtualMachineId  The virtual machine identifier
     * @param monitoringData    The monitoring data
     */
    public AggregatedVirtualMachineData(String virtualMachineId,
                                        List<VirtualMachineMonitoringData> monitoringData)
    {
        virtualMachineId_ = virtualMachineId;
        monitoringData_ = monitoringData;
    }

    /**
     * Returns the virtual machine identifier.
     * 
     * @return  The virtual machine id
     */
    public String getVirtualMachineId() 
    {
        return virtualMachineId_;
    }

    /**
     * Returns the monitoring data.
     * 
     * @return  The monitoring data
     */
    public List<VirtualMachineMonitoringData> getMonitoringData() 
    {
        return monitoringData_;
    }
}
