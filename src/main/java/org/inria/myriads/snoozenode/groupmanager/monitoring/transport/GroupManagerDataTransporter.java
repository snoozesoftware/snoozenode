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
package org.inria.myriads.snoozenode.groupmanager.monitoring.transport;

import java.io.Serializable;

import org.inria.myriads.snoozecommon.communication.groupmanager.summary.GroupManagerSummaryInformation;


/**
 * Group manager data transporter.
 * 
 * @author Eugen Feller
 */
public final class GroupManagerDataTransporter 
    implements Serializable
{
    /** Default serial. */
    private static final long serialVersionUID = 1L;
    
    /** Group manager identifier. */
    private String groupManagerId_;
    
    /** Summary information. */
    private GroupManagerSummaryInformation summary_;
    
    
    /** Default constructor. */
    public GroupManagerDataTransporter() 
    {
    }
    
    /**
     * Consturctor.
     *  
     * @param groupManagerId    The group manager identifier
     * @param summary           The summary information
     */
    public GroupManagerDataTransporter(String groupManagerId,
                                       GroupManagerSummaryInformation summary
                                        )
    {
        groupManagerId_ = groupManagerId;
        summary_ = summary;
    }

    /**
     * Returns the identifier.
     * 
     * @return      The identifier
     */
    public String getId()
    {
        return groupManagerId_;
    }
    
    /**
     * Returns the summary information.
     * 
     * @return  The summary information
     */
    public GroupManagerSummaryInformation getSummary() 
    {
        return summary_;
    }

    
}
