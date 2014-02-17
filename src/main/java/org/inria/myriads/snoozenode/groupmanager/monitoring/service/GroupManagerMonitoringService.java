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
package org.inria.myriads.snoozenode.groupmanager.monitoring.service;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.database.DatabaseSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.MonitoringSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.external.ExternalNotifierSettings;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.estimator.api.impl.StaticDynamicResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.monitoring.consumer.GroupManagerMonitoringDataConsumer;
import org.inria.myriads.snoozenode.groupmanager.monitoring.producer.GroupManagerHeartbeatDataProducer;
import org.inria.myriads.snoozenode.groupmanager.monitoring.producer.GroupManagerSummaryProducer;
import org.inria.myriads.snoozenode.groupmanager.monitoring.transport.GroupManagerDataTransporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Group manager monitoring service.
 * 
 * @author Eugen Feller
 */
public final class GroupManagerMonitoringService 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupManagerMonitoringService.class);
    
    /** GroupManagerId. */
    private String groupManagerId_;
    
    /** Monitoring data producer. */
    private GroupManagerSummaryProducer monitoringDataProducer_;

    /** Heartbeat producer. */
    private GroupManagerHeartbeatDataProducer heartbeatProducer_;
    
    /** Group Manager Monitoring Consumer. */
    private GroupManagerMonitoringDataConsumer groupManagerMonitoringDataConsumer_;
    
    /** Group manager repository. */
    private GroupManagerRepository repository_;
    
    /** Resource demand estimator.*/
    private StaticDynamicResourceDemandEstimator estimator_; 
    
    /** Blocking Queue.*/
    private BlockingQueue<GroupManagerDataTransporter> dataQueue_;

    /** Monitoring settings.*/
    private MonitoringSettings monitoringSettings_;
    
    /** Database settings. */
    private DatabaseSettings databaseSettings_;
    
    /** Monitoring external.*/
    private ExternalNotifierSettings monitoringExternalSettings_;

   

    
    
    
    /**
     * 
     * Constructor.
     * 
     * @param groupManagerId                The group manager identifier.
     * @param repository                    The group manager repository.
     * @param estimator                     The resource demand estimator.
     * @param databaseSettings              The database settings.
     * @param monitoringSettings            The monitoring settings.
     * @param monitoringExternalSettings    The external monitoring service.
     * @throws Exception                    Exception
     */
    public GroupManagerMonitoringService(
                                         String groupManagerId,
                                         GroupManagerRepository repository,
                                         StaticDynamicResourceDemandEstimator estimator,
                                         DatabaseSettings databaseSettings,
                                         MonitoringSettings monitoringSettings,
                                         ExternalNotifierSettings monitoringExternalSettings
                                        )
        throws Exception
    {
        Guard.check(repository, monitoringSettings, monitoringExternalSettings);
        
        log_.debug("Initializing the group manager monitoring service");
        groupManagerId_ = groupManagerId;
        repository_ = repository;
        estimator_ = estimator;
        databaseSettings_ = databaseSettings;
        monitoringSettings_ = monitoringSettings;
        monitoringExternalSettings_ = monitoringExternalSettings;
        dataQueue_ = new LinkedBlockingQueue<GroupManagerDataTransporter>();
    }
    
    
    /**
     * 
     * Start services.
     * 
     * @param groupLeader   The group leader address.
     * @throws Exception    Exception
     */
    public void startServices(NetworkAddress groupLeader) throws Exception
    {
        startGroupManagerSummaryProducer(groupLeader);
        startHeartbeatProducer();
        startGroupManagerMonitoringConsumer(groupLeader);
    }
    



    /**
     * Starts the summary information producer.
     * 
     * @param groupLeader   The group leader monitoring address
     * @throws Exception    The exception
     */
    private void startGroupManagerSummaryProducer(NetworkAddress groupLeader)
        throws Exception
    {
        log_.debug("Starting the group manager monitoring data producer");   
        monitoringDataProducer_ = new GroupManagerSummaryProducer(repository_,
                                                                  groupLeader,
                                                                  estimator_,
                                                                  monitoringSettings_,
                                                                  monitoringExternalSettings_,
                                                                  dataQueue_
                                                                  );
        new Thread(monitoringDataProducer_, "GroupManagerSummaryProducer").start();
    }
    
    /**
     * Starts the heartbeat producer.
     * 
     * @throws Exception    The exception
     */
    private void startHeartbeatProducer()
        throws Exception
    {
        log_.debug("Starting the heartbeat data producer");   
        heartbeatProducer_ = new GroupManagerHeartbeatDataProducer(
                groupManagerId_,
                monitoringSettings_.getInterval(),
                dataQueue_
                );
        new Thread(heartbeatProducer_, "GroupManagerHeartbeatProducer").start();
    }
    
    /**
     * 
     * Starts the groupManagerSummaryConsumer.
     * 
     * @param groupLeaderAddress    The groupLeader address.
     * @throws IOException 
     * 
     */
    private void startGroupManagerMonitoringConsumer(NetworkAddress groupLeaderAddress) throws IOException
    {
        log_.debug("Starting the heartbeat data producer");   
        groupManagerMonitoringDataConsumer_ = new GroupManagerMonitoringDataConsumer(
                groupManagerId_,
                groupLeaderAddress,
                databaseSettings_,
                dataQueue_
                );
        new Thread(groupManagerMonitoringDataConsumer_, "GroupManagerMonitoringDataConsumer").start();
    }
    
    /**
     * Terminates the monitoring service.
     */
    public void terminate()
    {
        log_.debug("Terminating the group manager monitoring service");        
        if (monitoringDataProducer_ != null)
        {
            monitoringDataProducer_.terminate();
        }
        
        if (heartbeatProducer_ != null)
        {
            heartbeatProducer_.terminate();
        }
    }
}
