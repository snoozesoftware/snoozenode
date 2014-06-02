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
package org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching;

import java.util.List;

import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.guard.Guard;

/**
 * Dispatching plan.
 * 
 * @author Eugen Feller
 */
public final class DispatchingPlan 
{
    /** List of group managers. */
    private List<GroupManagerDescription> groupManagers_;

    /**
     * Constructor.
     * 
     * @param groupManagers     The group managers
     */
    public DispatchingPlan(List<GroupManagerDescription> groupManagers)
    {
        groupManagers_ = groupManagers;
    }

    /**
     * Returns the group managers.
     * 
     * @return  The group managers
     */
    public List<GroupManagerDescription> getGroupManagers() 
    {
        return groupManagers_;
    }
    
    
    /** 
     * Merge a dispatching plan. 
     * 
     * @param dispatchPlan      The dispatch plan to merge with.
     * 
     * */
    public void merge(DispatchingPlan dispatchPlan)
    {
        Guard.check(dispatchPlan);
        groupManagers_.addAll(dispatchPlan.getGroupManagers());
    }

    
}
