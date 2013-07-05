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
package org.inria.myriads.snoozenode.groupmanager.monitoring.consumer;

import java.util.concurrent.BlockingQueue;

import org.inria.myriads.snoozenode.database.api.GroupLeaderRepository;
import org.inria.myriads.snoozenode.groupmanager.monitoring.transport.GroupManagerDataTransporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Group manager monitoring data consumer.
 * 
 * @author Eugen Feller
 */
public final class GroupManagerSummaryConsumer
    implements Runnable
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupManagerSummaryConsumer.class);
    
    /** Group leader repository. */
    private GroupLeaderRepository repository_;
    
    /** Queue with data. */
    private BlockingQueue<GroupManagerDataTransporter> dataQueue_;

    /**
     * Group manager monitoring data consumer.
     * 
     * @param dataQueue     The data queue reference
     * @param repository    The group leader description
     * @throws Exception    The exception
     */ 
    public GroupManagerSummaryConsumer(BlockingQueue<GroupManagerDataTransporter> dataQueue,
                                       GroupLeaderRepository repository)
        throws Exception 
    {
        log_.debug("Initializing the group manager monitoring data consumer");
        dataQueue_ = dataQueue;
        repository_ = repository;
    }

    /** The run method. */
    public void run()
    {
        try
        {
            while (true)
            {            
                GroupManagerDataTransporter groupManagerTransporter = dataQueue_.take();
                if (groupManagerTransporter != null)
                {
                    repository_.addGroupManagerSummaryInformation(groupManagerTransporter.getId(),
                                                                  groupManagerTransporter.getSummary());
                }         
            }
        }
        catch (InterruptedException exception) 
        {
            log_.error("Group manager monitoring data consumer was interrupted: %s", 
                       exception.getMessage());
        }
    }
}
