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
package org.inria.myriads.snoozenode.groupmanager.managerpolicies.comparators;

import java.util.ArrayList;
import java.util.Comparator;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerStatus;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.util.MathUtils;
import org.inria.myriads.snoozenode.estimator.api.impl.StaticDynamicResourceDemandEstimator;

/**
 * L1 norm based local controller sorting in increasing order.
 * 
 * @author Eugen Feller
 */
public class LocalControllerL1Increasing
    implements Comparator<LocalControllerDescription> 
{
    /** Resource demand estimator. */
    private StaticDynamicResourceDemandEstimator estimator_;
    
    /**
     * Constructor.
     * 
     * @param estimator     The resource demand estimator
     */
    public LocalControllerL1Increasing(StaticDynamicResourceDemandEstimator estimator)
    {
        Guard.check(estimator);
        estimator_ = estimator;
    }
    
    /**
     * Compares two virtual machines.
     *  
     * @param localController1   First virtual machine
     * @param localController2   Second virtual machine
     * @return                   -1, 0, 1
     */
    public final int compare(LocalControllerDescription localController1, 
                             LocalControllerDescription localController2)
    {
        Guard.check(localController1, localController2);
        
        ArrayList<Double>utilization1 = estimator_.computeLocalControllerCapacity(localController1);                
        ArrayList<Double> utilization2 = estimator_.computeLocalControllerCapacity(localController2);  
        
        double value1 = MathUtils.computeL1Norm(utilization1);
        double value2 = MathUtils.computeL1Norm(utilization2);
        
        if (value1 < value2) 
        {
            return -1;
        } else if (value1 > value2) 
        {
            return 1;
        }

        LocalControllerStatus status1 = localController1.getStatus();
        LocalControllerStatus status2 = localController2.getStatus();
        if (status1.equals(LocalControllerStatus.ACTIVE) && status2.equals(LocalControllerStatus.PASSIVE))
        {
            return -1;
        } else if (status1.equals(LocalControllerStatus.PASSIVE) && status2.equals(LocalControllerStatus.ACTIVE))
        {
            return 1;
        }
        
        return 0;
    }
}
