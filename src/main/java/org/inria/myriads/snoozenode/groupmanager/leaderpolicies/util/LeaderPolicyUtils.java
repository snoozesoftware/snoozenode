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
package org.inria.myriads.snoozenode.groupmanager.leaderpolicies.util;

import java.util.List;
import java.util.Map;

import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.groupmanager.summary.GroupManagerSummaryInformation;
import org.inria.myriads.snoozecommon.util.MonitoringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Leader utility.
 * 
 * @author Eugen Feller
 */
public final class LeaderPolicyUtils 
{
    /** Logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LeaderPolicyUtils.class);
        
    /**
     * Hide the consturctor.
     */
    private LeaderPolicyUtils() 
    {
        throw new UnsupportedOperationException();
    }
        
    /**
     * Prints group manager descriptions.
     * 
     * @param groupManagers     The group managers
     */
    public static void printGroupManagerDescriptions(List<GroupManagerDescription> groupManagers)
    {
        for (GroupManagerDescription groupManager : groupManagers)
        {  
            Map<Long, GroupManagerSummaryInformation> groupManagerSummary = groupManager.getSummaryInformation();
            if (groupManagerSummary.size() == 0)
            {
                log_.debug(String.format("Group manager %s has no summary information!", groupManager.getId()));
                continue;
            }
            
            GroupManagerSummaryInformation summary = MonitoringUtils.getLatestSummaryInformation(groupManagerSummary);
            log_.debug(String.format("Group manager %s active: %s, and passive: %s, used: %s capacity",
                                     groupManager.getId(), summary.getActiveCapacity(), 
                                     summary.getPassiveCapacity(), summary.getUsedCapacity()));
        }
    }
}
