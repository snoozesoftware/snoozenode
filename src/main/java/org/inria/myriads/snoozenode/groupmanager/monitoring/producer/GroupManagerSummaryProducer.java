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
package org.inria.myriads.snoozenode.groupmanager.monitoring.producer;

import java.io.IOException;
import java.util.ArrayList;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.summary.GroupManagerSummaryInformation;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.monitoring.transport.GroupManagerDataTransporter;
import org.inria.myriads.snoozenode.monitoring.connectionlistener.ConnectionListener;
import org.inria.myriads.snoozenode.monitoring.connectionlistener.RabbitMQConnectionWorker;
import org.inria.myriads.snoozenode.monitoring.datasender.api.DataSender;
import org.inria.myriads.snoozenode.monitoring.datasender.api.impl.DevNullDataSender;
import org.inria.myriads.snoozenode.monitoring.datasender.api.impl.RabbitMQDataSender;
import org.inria.myriads.snoozenode.monitoring.datasender.api.impl.TCPDataSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Group manager monitoring data producer.
 * 
 * @author Eugen Feller
 */
public final class GroupManagerSummaryProducer
    implements Runnable, ConnectionListener
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupManagerSummaryProducer.class);
        
    /** Group manager repository reference. */
    private GroupManagerRepository repository_;

    /** Resource demand estimator. */
    private ResourceDemandEstimator estimator_;
    
    /** Lock object. */
    private Object lockObject_;

    /** Monitoring interval. */
    private int monitoringInterval_;
    
    /** Terminated. */
    private boolean isTerminated_;
    
    /** internal sender.*/
    DataSender internalSender_;
    
    /** external sender.*/
    DataSender externalSender_;    
    
    RabbitMQConnectionWorker connectionWorker_;
    /**
     * Constructor.
     * 
     * @param repository            The group manager repository
     * @param groupLeaderAddress    The group leader address
     * @param estimator             The estimator
     * @param monitoringInterval    The monitoring interval
     * @throws IOException          The I/O exception
     */
    public GroupManagerSummaryProducer(GroupManagerRepository repository, 
                                       NetworkAddress groupLeaderAddress,
                                       ResourceDemandEstimator estimator,
                                       int monitoringInterval)
        throws  IOException 
    { 
        
        log_.debug("Initializing the group manager summary information producer");
        internalSender_ = new TCPDataSender(groupLeaderAddress);
        repository_ = repository;
        estimator_ = estimator;
        monitoringInterval_ = monitoringInterval;
        lockObject_ = new Object();
        connectionWorker_ = new RabbitMQConnectionWorker(this,10000,"grouleader");
        connectionWorker_.start();
    }
    
    /**
     * Creates the data transporter.
     * 
     * @return    The data transporter
     */
    private GroupManagerDataTransporter createDataTransporter()
    {
        ArrayList<LocalControllerDescription> localControllers = 
            repository_.getLocalControllerDescriptions(estimator_.getNumberOfMonitoringEntries(), false);
        ArrayList<String> legacyIpAddresses = repository_.getLegacyIpAddresses();
        
        GroupManagerSummaryInformation summary = estimator_.generateGroupManagerSummaryInformation(localControllers, 
                                                                                                   legacyIpAddresses);
        String groupManagerId = repository_.getGroupManagerId();
        GroupManagerDataTransporter dataTransporter = new GroupManagerDataTransporter(groupManagerId, summary);      
        return dataTransporter;
    }
    
    /** Run method. */
    public void run()
    {    
        try 
        {
            while (!isTerminated_)
            {                
                GroupManagerDataTransporter dataTransporter = createDataTransporter();
                GroupManagerSummaryInformation summary = dataTransporter.getSummary();
                log_.debug(String.format("Sending summary information to the group leader! " +
                                         "Active: %s, passive: %s, requested: %s, and used: %s capacity", 
                                         summary.getActiveCapacity(), summary.getPassiveCapacity(),
                                         summary.getRequestedCapacity(), summary.getUsedCapacity()));
                
                try{
                    internalSender_.send(dataTransporter);
                }
                catch(IOException exception)
                {
                    log_.debug(String.format("I/O error during summary sending (%s). Did the group leader fail?", 
                            exception.getMessage()));
                    break;
                }
                
                sendExternal(dataTransporter);
                
                synchronized (lockObject_)
                {
                    lockObject_.wait(monitoringInterval_);
                }
            }
        } 
        catch (InterruptedException exception)
        {
            
        }
        terminate();
        log_.debug("Group manager summary information producer is stopped!");
    }
    
    private void sendExternal(GroupManagerDataTransporter dataTransporter)
    {
        if (externalSender_ != null)
        {
            try
            {
                externalSender_.send(dataTransporter, dataTransporter.getId());
            }
            catch(Exception exception)
            {
                log_.debug(String.format("I/O error during external data sending (%s)! Did the group manager close " +
                        "its connection unexpectedly?", exception.getMessage()));
                externalSender_.close();
                externalSender_ = null;
                
                if (! connectionWorker_.isRunning())
                    connectionWorker_.restart();
            }
            
        }

        
    }

    /** 
     * Terminating the thread.
     */
    public synchronized void terminate() 
    {
        log_.debug("Terminating the group manager summary information producer");
        synchronized (lockObject_)
        {
            isTerminated_ = true;
            lockObject_.notify();
        }
    }

    @Override
    public void onConnectionSuccesfull(DataSender dataSender)
    {
        log_.debug("Connection successfull to the rabbitmq service");
        externalSender_ = dataSender;
        
    }
}
