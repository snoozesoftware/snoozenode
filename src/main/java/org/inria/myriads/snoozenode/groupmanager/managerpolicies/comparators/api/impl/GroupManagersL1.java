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

import java.util.Map;

import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.groupmanager.summary.GroupManagerSummaryInformation;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.util.MathUtils;
import org.inria.myriads.snoozecommon.util.MonitoringUtils;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.comparators.api.SnoozeComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sort group manager in decreasing order.
 * 
 * @author Matthieu Simonin
 */
public final class GroupManagersL1 extends SnoozeComparator<GroupManagerDescription>
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupManagersL1.class);
   
    /**
     * Constructor. 
     */
    public GroupManagersL1()
    {
        log_.debug("Building a L1 group manager comparator");
    }

    @Override
    public void initialize()
    {
        log_.debug("Initializing a L1 group manager comparator");
    }

    @Override
    protected int internalCompare(GroupManagerDescription groupManager1, GroupManagerDescription groupManager2)
    {
        Guard.check(groupManager1, groupManager2);
        
        Map<Long, GroupManagerSummaryInformation> groupManagerSummary1 = groupManager1.getSummaryInformation();
        if (groupManagerSummary1.size() == 0)
        {
            log_.debug(String.format("Group manager %s has no summary information!", groupManager1.getId()));
            return 0;
        }
        
        Map<Long, GroupManagerSummaryInformation> groupManagerSummary2 = groupManager2.getSummaryInformation();     
        if (groupManagerSummary2.size() == 0)
        {
            log_.debug(String.format("Group manager %s has no summary information!", groupManager2.getId()));
            return 0;
        }
        
        GroupManagerSummaryInformation summary1 = MonitoringUtils.getLatestSummaryInformation(groupManagerSummary1);
        GroupManagerSummaryInformation summary2 = MonitoringUtils.getLatestSummaryInformation(groupManagerSummary2);
        
        double activeCapacity1 = MathUtils.computeL1Norm(summary1.getActiveCapacity());                
        double activeCapacity2 = MathUtils.computeL1Norm(summary2.getActiveCapacity());
        log_.debug(String.format("L1 active capacity of group manager 1 and 2 is %.2f, %.2f", 
                                 activeCapacity1, activeCapacity2));
        
        if (activeCapacity1 < activeCapacity2) 
        {
            return -1;
        } else if (activeCapacity1 > activeCapacity2) 
        {
            return 1;
        }

        double passiveCapaciy1 = MathUtils.computeL1Norm(summary1.getPassiveCapacity());
        double passiveCapaciy2 = MathUtils.computeL1Norm(summary2.getPassiveCapacity());
        log_.debug(String.format("L1 passive capacity of group manager 1 and 2 is %.2f, %.2f", 
                                 passiveCapaciy1, passiveCapaciy2));
        if (passiveCapaciy1 < passiveCapaciy2)
        {
            return -1;
        } if (passiveCapaciy1 > passiveCapaciy2)
        {
            return 1;
        }
        
        return 0;
    }
}
