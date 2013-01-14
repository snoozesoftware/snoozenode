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
package org.inria.myriads.snoozenode.groupmanager.managerpolicies.comparators;

import java.util.ArrayList;
import java.util.Comparator;

import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.util.MathUtils;

/**
 * L1 norm based virtual machine sorting in increasing order.
 * 
 * @author Eugen Feller
 */
public final class VirtualMachineL1Increasing
    implements Comparator<VirtualMachineMetaData> 
{
    /** Resource demand estimator. */
    private ResourceDemandEstimator estimator_;

    /**
     * Consturctor.
     * 
     * @param estimator     The resource demand estimator
     */
    public VirtualMachineL1Increasing(ResourceDemandEstimator estimator) 
    {
        Guard.check(estimator);
        estimator_ = estimator;
    }

    /**
     * Compares two virtual machines.
     *  
     * @param firstVirtualMachine       First virtual machine
     * @param secondVirtualMachine      Second virtual machine
     * @return                         -1, 0, 1
     */
    public int compare(VirtualMachineMetaData firstVirtualMachine, 
                       VirtualMachineMetaData secondVirtualMachine)
    {
        Guard.check(firstVirtualMachine, secondVirtualMachine);
        ArrayList<Double> estunatedDemand1 = estimator_.estimateVirtualMachineResourceDemand(firstVirtualMachine);
        double utilization1 = MathUtils.computeL1Norm(estunatedDemand1);

        ArrayList<Double> estunatedDemand2 = estimator_.estimateVirtualMachineResourceDemand(secondVirtualMachine);        
        double utilization2 = MathUtils.computeL1Norm(estunatedDemand2);
        
        if (utilization1 < utilization2) 
        {
            return -1;
        } else if (utilization1 > utilization2) 
        {
            return 1;
        }

        return 0;
    }
}
