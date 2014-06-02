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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.groupmanager.monitoring.MonitoringFactory;
import org.inria.myriads.snoozenode.groupmanager.monitoring.consumer.LocalControllerSummaryConsumer;
import org.inria.myriads.snoozenode.groupmanager.monitoring.receiver.LocalControllerSummaryReceiver;
import org.inria.myriads.snoozenode.groupmanager.statemachine.api.StateMachine;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.LocalControllerDataTransporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local controller monitoring service.
 * 
 * @author Eugen Feller
 */
public final class LocalControllerMonitoringService
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LocalControllerMonitoringService.class);
    
    /** Node configuration. */
    private NodeConfiguration nodeConfiguration_;
    
    /** Blocking queue. */
    private BlockingQueue<LocalControllerDataTransporter> dataQueue_;

    /** Local controller summary receiver. */
    private LocalControllerSummaryReceiver summaryReceiver_;

    /** Local controller summary consumer. */
    private LocalControllerSummaryConsumer summaryConsumer_;

    /** State machine. */
    private StateMachine stateMachine_;

    /** Repository. */
    private GroupManagerRepository repository_;
            
    /**
     * Constructor.
     * 
     * @param nodeConfiguration     The node configuration
     * @param repository            The repository
     * @param stateMachine          The state machine
     */
    public LocalControllerMonitoringService(NodeConfiguration nodeConfiguration,
                                            StateMachine stateMachine,
                                            GroupManagerRepository repository)
    {
        Guard.check(nodeConfiguration);
        log_.debug("Intializing the local controller monitoring service");
        
        nodeConfiguration_ = nodeConfiguration;
        stateMachine_ = stateMachine;
        repository_ = repository;
        dataQueue_ = new LinkedBlockingQueue<LocalControllerDataTransporter>();
    }
    
    /**
     * Starts the monitoring.
     * 
     * @throws Exception        Exception 
     */
    public void startMonitoring() 
        throws Exception
    {
        log_.debug("Starting local controller monitoring service");
        startLocalControllerSummaryConsumer();
        startLocalControllerSummaryReceiver();
    }
    
    /**
     * Starts the local controller monitoring data receiver.
     * 
     * @throws Exception 
     */
    private void startLocalControllerSummaryReceiver() 
        throws Exception
    {
        log_.debug("Starting the local controller summary information receiver");     
        NetworkAddress address = nodeConfiguration_.getNetworking().getListen().getMonitoringDataAddress();
        int timeout = nodeConfiguration_.getMonitoring().getTimeout();
        summaryReceiver_ = MonitoringFactory.newLocalControllerSummaryReceiver(address,
                                                                               timeout,
                                                                               dataQueue_,
                                                                               stateMachine_,
                                                                               repository_);
    }
    
    /**
     * Starts the data consumer.
     * 
     * @throws Exception 
     */
    private void startLocalControllerSummaryConsumer()   
        throws Exception
    {
        log_.debug("Starting the local controller summary information consumer");     
        summaryConsumer_ = MonitoringFactory.newLocalControllerSummaryConsumer(dataQueue_, stateMachine_, repository_);
    }

    /**
     * Terminates the monitoring service.
     */
    public synchronized void terminate()
    {    
        if (summaryReceiver_ != null)
        {
            log_.debug("Terminating the local controller summary receiver");    
            summaryReceiver_.terminate();
        }       
        
        if (summaryConsumer_ != null)
        {
            log_.debug("Terminating the local controller summary consumer");    
            summaryConsumer_.terminate();
        }
    }
}
