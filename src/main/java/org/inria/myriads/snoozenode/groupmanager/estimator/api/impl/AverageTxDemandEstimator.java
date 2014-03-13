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
package org.inria.myriads.snoozenode.groupmanager.estimator.api.impl;

import java.util.Map;

import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.VirtualMachineMonitoringData;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.groupmanager.estimator.api.VirtualMachineMonitoringEstimator;
import org.inria.myriads.snoozenode.util.UtilizationUtils;


/**
 * @author msimonin
 *
 */
public final class AverageTxDemandEstimator 
    implements VirtualMachineMonitoringEstimator
{    
    /**
     * Estimates the network demand.
     * 
     * @param virtualMachineHistory     The virtual machine history data
     * @return                          The network demand estimate
     */
    public double estimate(Map<Long, VirtualMachineMonitoringData> virtualMachineHistory) 
    {
        Guard.check(virtualMachineHistory);      
        double rxUtilization = 0;
        double txUtilization = 0;
        
        for (VirtualMachineMonitoringData monitoringData : virtualMachineHistory.values())
        {
//            rxUtilization += UtilizationUtils.getNetworkRxUtilization(monitoringData.getUsedCapacity());
            txUtilization += UtilizationUtils.getNetworkTxUtilization(monitoringData.getUsedCapacity());
        }
        
//        rxUtilization = rxUtilization / virtualMachineHistory.size();
        txUtilization = txUtilization / virtualMachineHistory.size();
        
//        NetworkDemand networkDemand = new NetworkDemand(rxUtilization, txUtilization);
        return txUtilization;
    }
}
