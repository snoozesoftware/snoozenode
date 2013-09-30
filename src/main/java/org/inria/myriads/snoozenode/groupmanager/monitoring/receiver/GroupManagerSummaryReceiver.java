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
package org.inria.myriads.snoozenode.groupmanager.monitoring.receiver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.database.api.GroupLeaderRepository;
import org.inria.myriads.snoozenode.groupmanager.monitoring.consumer.GroupManagerSummaryConsumer;
import org.inria.myriads.snoozenode.groupmanager.monitoring.transport.GroupManagerDataTransporter;
import org.inria.myriads.snoozenode.tcpip.DataListener;
import org.inria.myriads.snoozenode.tcpip.TCPDataReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Group manager monitoring data receiver.
 * 
 * @author Eugen Feller
 */
public final class GroupManagerSummaryReceiver extends TCPDataReceiver 
    implements DataListener 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupManagerSummaryReceiver.class);
    
    /** Holds the reference to the group leader repository. */
    private GroupLeaderRepository repository_;
    
    /** The blocking queue reference. */
    private BlockingQueue<GroupManagerDataTransporter> dataQueue_;
    
    /** Group manager identifiers. */
    private Map<String, String> groupManagerIds_;

    /**
     * Constructor.
     * 
     * @param networkAddress   The network address
     * @param timeOut          The timeout
     * @param repository       The group leader repository
     * @throws Exception 
     */
    public GroupManagerSummaryReceiver(NetworkAddress networkAddress, 
                                       int timeOut,
                                       GroupLeaderRepository repository)
        throws Exception                                        
    {
        super(networkAddress, timeOut);
        Guard.check(repository);
        log_.debug("Initializing the group manager summary information receiver");
        
        repository_ = repository;
        groupManagerIds_ = new HashMap<String, String>();
        dataQueue_ = new LinkedBlockingQueue<GroupManagerDataTransporter>();
          
        startDataConsumer();
        starDataReceiver();
        
        log_.debug("Group manager summary information receiver started");
    }
    
    /**
     * Starts the data consumer.
     * 
     * @throws Exception 
     */
    private void startDataConsumer()   
        throws Exception
    {
        GroupManagerSummaryConsumer consumer = new GroupManagerSummaryConsumer(dataQueue_, repository_);
        new Thread(consumer, "GroupManagerSummaryConsumer").start();       
    }
    
    /**
     * Starts the TCP data receiver.
     */
    private void starDataReceiver()
    {
        setHandler(this);
        new Thread(this, "TCPDataReceiver").start();       
    }
    
    /** 
     * Handles the monitoring data timeout.
     *  
     * @param workerThreadId    The worker thread id
     */
    @Override
    public synchronized void onFailure(String workerThreadId) 
    {
        Guard.check(workerThreadId);        
        log_.debug(String.format("Failed to receive group summary information from worker thread: %s",
                                 workerThreadId));
        String groupManagerId = groupManagerIds_.get(workerThreadId);
        if (groupManagerId == null)
        {
            log_.debug("No group manager identifier exists for this worker thread! Nothind to drop!");
            return;
        }
        
        boolean isDropped = repository_.dropGroupManager(groupManagerId);
        if (isDropped)
        {
            log_.debug("Group manager dropped successfully");
        } else
        {
            log_.debug("Unable to drop the group manager");
        }
    }
    
    /** 
     * Data event callback.
     *  
     * @param data              The data object
     * @param workerThreadId    The worker thread identifier
     */
    @Override
    public synchronized void onDataArrival(Object data, String workerThreadId) 
    {
        Guard.check(data, workerThreadId);
        
        GroupManagerDataTransporter dataTransporter = (GroupManagerDataTransporter) data;
        
        if (groupManagerIds_.get(workerThreadId) == null)
        {
            log_.debug("No mapping exists for this worker thread! Adding!");
            groupManagerIds_.put(workerThreadId, dataTransporter.getId());    
        }
        
        if (dataTransporter.getSummary() == null)
        {
            log_.debug("Received heartbeat ... skipping");
            return;
        }
        
        log_.debug(String.format("Received group manager %s summary information. " +
                                 "Active: %s, " +
                                 "Passive: %s, " +
                                 "Requested: %s, " +
                                 "Used: %s, " +
                                 "Legacy IP addresses: %s, " +
                                 "Assigned localControllers: %d, " +
                                 "Worker thread id: %s",
                                 dataTransporter.getId(), 
                                 dataTransporter.getSummary().getActiveCapacity(),
                                 dataTransporter.getSummary().getPassiveCapacity(),
                                 dataTransporter.getSummary().getRequestedCapacity(),
                                 dataTransporter.getSummary().getUsedCapacity(), 
                                 dataTransporter.getSummary().getLegacyIpAddresses(),
                                 dataTransporter.getSummary().getLocalControllers().size(),
                                 workerThreadId));
    

        
        dataQueue_.add(dataTransporter);
    }
}
