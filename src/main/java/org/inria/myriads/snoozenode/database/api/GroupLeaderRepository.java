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
package org.inria.myriads.snoozenode.database.api;

import java.util.ArrayList;

import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.groupmanager.summary.GroupManagerSummaryInformation;

/**
 * Group leader repository interface.
 * 
 * @author Eugen Feller
 */
public interface GroupLeaderRepository 
{
    /** 
     * Adds a group manager description. 
     * 
     * @param description   The group manager description
     * @return              true if added, false otherwise
     */
    boolean addGroupManagerDescription(GroupManagerDescription description);

    /**
     * Returns the group manager descriptions.
     * 
     * @param numberOfBacklogEntries    The number of backlog entries
     * @return                          The group manager descriptions
     */
    ArrayList<GroupManagerDescription> getGroupManagerDescriptions(int numberOfBacklogEntries);
               
    /** 
     * Adds group manager data.
     * 
     * @param groupManagerId            The group manager identifier
     * @param summary                   The group manager summary information
     */
    void addGroupManagerSummaryInformation(String groupManagerId, GroupManagerSummaryInformation summary);
            
    /** 
     * Drops a group manager. 
     * 
     * @param groupManagerId       The group manager identifier
     * @return                     true if everything ok, false otherwise
     */
    boolean dropGroupManager(String groupManagerId);
    
    /**
     * Adds the IP address.
     * 
     * @param ipAddress     The ip address
     * @return              true if everything ok, false otherwise
     */
    boolean addIpAddress(String ipAddress);
    
    /**
     * Removes IP address from the pool.
     * 
     * @param ipAddress     The ip address
     * @return              true if everything ok, false otherwise
     */
    boolean removeIpAddress(String ipAddress);
    
    /**
     * Get the next free IP address.
     * 
     * @return     The next free ip address
     */
    String getFreeIpAddress();

    /**
     * Returns the number of free IP addresses.
     * 
     * @return  The number of free IP addresses
     */
    int getNumberOfFreeIpAddresses();
}
