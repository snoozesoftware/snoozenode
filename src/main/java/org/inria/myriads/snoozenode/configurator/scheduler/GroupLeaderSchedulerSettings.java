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
package org.inria.myriads.snoozenode.configurator.scheduler;

/**
 * Group leader scheduler settings.
 * 
 * @author Eugen Feller
 */
public final class GroupLeaderSchedulerSettings 
{
    /** Local controller assignment policy. */
    private String assignmentPolicy_;

    /** Virtual cluster dispatching policy. */
    private String dispatchingPolicy_;

    /**
     * Sets the virtual cluster dispatching policy.
     * 
     * @param dispatchingPolicy    The virtual cluster dispatching policy
     */
    public void setDispatchingPolicy(String dispatchingPolicy) 
    {
        dispatchingPolicy_ = dispatchingPolicy;
    }

    /**
     * Returns the virtual cluster dispatch policy.
     * 
     * @return  The virtual cluster dispatch policy
     */
    public String getDispatchingPolicy() 
    {
        return dispatchingPolicy_;
    }
    
    /**
     * Returns the local controller assignment policy.
     * 
     * @return  The local controller assignment policy
     */
    public String getAssignmentPolicy() 
    {
        return assignmentPolicy_;
    }
    
    /**
     * Sets the local controller assignment policy.
     * 
     * @param assignmentPolicy   The local controller assignment plicy
     */
    public void setAssignmentPolicy(String assignmentPolicy) 
    {
        assignmentPolicy_ = assignmentPolicy;
    }
}
