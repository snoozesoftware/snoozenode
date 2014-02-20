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
package org.inria.myriads.snoozenode.groupmanager.managerpolicies.comparators.api.impl;

import java.util.ArrayList;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.util.MathUtils;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.comparators.api.SnoozeComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * L1 norm based virtual machine sorting in increasing order.
 * 
 * @author Matthieu Simonin
 */
public final class VirtualMachinesL1
   extends SnoozeComparator<VirtualMachineMetaData>
{
    
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(VirtualMachinesL1.class);
    
    /**
     * Constructor.
     * 
     * @param estimator     The resource demand estimator
     */
    public VirtualMachinesL1() 
    {
        log_.debug("Building a L1 virtual machines comparator");
    }

    
    @Override
    public void initialize()
    {
        log_.debug("Initializing a new L1 virtual machines comparator");
    }
    
    @Override
    protected int internalCompare(VirtualMachineMetaData firstVirtualMachine, VirtualMachineMetaData secondVirtualMachine)
    {
        Guard.check(firstVirtualMachine, secondVirtualMachine);
        ArrayList<Double> estunatedDemand1 = estimator_.estimateVirtualMachineResourceDemand(firstVirtualMachine);
        double utilization1 = MathUtils.computeL1Norm(estunatedDemand1);

        ArrayList<Double> estunatedDemand2 = 
                estimator_.estimateVirtualMachineResourceDemand(secondVirtualMachine);        
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
